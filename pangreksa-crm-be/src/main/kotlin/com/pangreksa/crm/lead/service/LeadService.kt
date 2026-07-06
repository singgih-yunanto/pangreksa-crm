package com.pangreksa.crm.lead.service

import com.pangreksa.crm.account.domain.AccountRepository
import com.pangreksa.crm.account.service.AccountService
import com.pangreksa.crm.account.web.AccountRequest
import com.pangreksa.crm.audit.AuditService
import com.pangreksa.crm.base.NotFoundException
import com.pangreksa.crm.base.Page
import com.pangreksa.crm.base.ValidationException
import com.pangreksa.crm.base.pageRequest
import com.pangreksa.crm.contact.domain.ContactRepository
import com.pangreksa.crm.contact.service.ContactService
import com.pangreksa.crm.contact.web.ContactRequest
import com.pangreksa.crm.deal.service.DealService
import com.pangreksa.crm.deal.web.DealRequest
import com.pangreksa.crm.lead.domain.Lead
import com.pangreksa.crm.lead.domain.LeadRepository
import com.pangreksa.crm.lead.web.LeadConversionResult
import com.pangreksa.crm.lead.web.LeadConvertRequest
import com.pangreksa.crm.lead.web.LeadRequest
import com.pangreksa.crm.lookup.web.LookupService
import com.pangreksa.crm.security.AccessService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class LeadService(
    private val repository: LeadRepository,
    private val access: AccessService,
    private val lookups: LookupService,
    private val audit: AuditService,
    private val accountService: AccountService,
    private val accountRepository: AccountRepository,
    private val contactService: ContactService,
    private val contactRepository: ContactRepository,
    private val dealService: DealService,
) {
    private val sortable = setOf("lastName", "company", "createdAt", "updatedAt")

    private fun fullName(l: Lead) = listOfNotNull(l.firstName, l.lastName).joinToString(" ").trim()

    @Transactional(readOnly = true)
    fun list(
        offset: Int, limit: Int, q: String?, sort: String?,
        leadStatusId: Long? = null, leadSourceId: Long? = null, ratingId: Long? = null,
        industryId: Long? = null, ownerId: Long? = null,
    ): Page<Lead> {
        val ids = access.accessibleOwnerIds()
        val pr = pageRequest(offset, limit, sort, sortable, "lastName")
        val page = repository.search(
            ids == null, ids ?: listOf(-1L), q ?: "",
            leadStatusId, leadSourceId, ratingId, industryId, ownerId, pr,
        )
        return Page(page.content, page.totalElements)
    }

    @Transactional(readOnly = true)
    fun get(id: Long): Lead {
        val l = repository.findDetailById(id) ?: throw NotFoundException("Lead not found: $id")
        if (!access.canAccess(l.owner?.id)) throw NotFoundException("Lead not found: $id")
        return l
    }

    @Transactional
    fun create(req: LeadRequest): Lead {
        val lastName = req.lastName?.trim().orEmpty()
        if (lastName.isEmpty()) throw ValidationException("lastName", "Last name is required")
        val l = Lead(lastName = lastName)
        l.owner = access.currentUser()
        // Default status "Not Contacted" unless one is supplied.
        l.leadStatus = req.leadStatusId?.let { lookups.resolveById("lead_status", it) }
            ?: lookups.resolve("lead_status", "Not Contacted")
        apply(l, req)
        val saved = repository.save(l)
        audit.record("leads", saved.id!!, "created", mapOf("name" to fullName(saved)))
        return saved
    }

    @Transactional
    fun update(id: Long, req: LeadRequest): Lead {
        val l = get(id)
        if (l.converted) throw ValidationException(null, "A converted lead is read-only")
        req.lastName?.let { if (it.isBlank()) throw ValidationException("lastName", "Last name is required"); l.lastName = it.trim() }
        req.leadStatusId?.let { l.leadStatus = lookups.resolveById("lead_status", it) }
        apply(l, req)
        val saved = repository.save(l)
        audit.record("leads", saved.id!!, "updated", mapOf("name" to fullName(saved)))
        return saved
    }

    @Transactional
    fun delete(id: Long) {
        val l = get(id)
        repository.deleteById(id)
        audit.record("leads", id, "deleted", mapOf("name" to fullName(l)))
    }

    /**
     * Convert a lead into an Account + Contact (+ optional Deal). Links to existing records when ids
     * are supplied, else creates new ones (reusing the module create-services so validation, audit and
     * derivations run). The lead is then flagged converted, locked, and made read-only.
     */
    @Transactional
    fun convert(id: Long, req: LeadConvertRequest): LeadConversionResult {
        val lead = get(id)
        if (lead.converted) throw ValidationException(null, "Lead is already converted")

        val account = req.accountId?.let {
            accountRepository.findById(it).orElseThrow { ValidationException("accountId", "Account not found: $it") }
        } ?: accountService.create(
            AccountRequest(
                name = lead.company?.trim()?.ifBlank { null } ?: "${fullName(lead)} (Account)".trim(),
                industryId = lead.industry?.id, phone = lead.phone, website = lead.website,
                employees = lead.noOfEmployees, annualRevenue = lead.annualRevenue,
                billingStreet = lead.street, billingCity = lead.city, billingState = lead.state,
                billingCountry = lead.country, billingCode = lead.zipCode, description = lead.description,
            ),
        )

        val contact = req.contactId?.let {
            contactRepository.findById(it).orElseThrow { ValidationException("contactId", "Contact not found: $it") }
        } ?: contactService.create(
            ContactRequest(
                firstName = lead.firstName, lastName = lead.lastName.ifBlank { "Contact" },
                accountId = account.id, title = lead.title, email = lead.email,
                phone = lead.phone, mobile = lead.mobile, leadSourceId = lead.leadSource?.id,
                description = lead.description,
            ),
        )

        val deal = if (req.createDeal == true) {
            val stageId = req.dealStageId ?: lookups.byCategory("deal_stage").firstOrNull()?.id
                ?: throw ValidationException("dealStageId", "No deal stage available")
            dealService.create(
                DealRequest(
                    name = req.dealName?.trim()?.ifBlank { null } ?: "${account.name} — new deal",
                    stageId = stageId, amount = req.dealAmount, closingDate = req.dealClosingDate,
                    accountId = account.id, contactId = contact.id,
                ),
            )
        } else {
            null
        }

        lead.converted = true
        lead.convertedAt = LocalDateTime.now()
        lead.locked = true
        lead.convertedAccount = account
        lead.convertedContact = contact
        lead.convertedDeal = deal
        repository.save(lead)
        audit.record("leads", id, "converted", mapOf("account" to account.name))

        return LeadConversionResult(id, account.id!!, contact.id!!, deal?.id)
    }

    private fun apply(l: Lead, req: LeadRequest) {
        req.firstName?.let { l.firstName = it.trim() }
        req.company?.let { l.company = it.trim() }
        req.title?.let { l.title = it.trim() }
        req.salutationId?.let { l.salutation = lookups.resolveById("salutation", it) }
        req.email?.let { l.email = it.trim() }
        req.secondaryEmail?.let { l.secondaryEmail = it.trim() }
        req.phone?.let { l.phone = it.trim() }
        req.mobile?.let { l.mobile = it.trim() }
        req.fax?.let { l.fax = it.trim() }
        req.website?.let { l.website = it.trim() }
        req.leadSourceId?.let { l.leadSource = lookups.resolveById("lead_source", it) }
        req.industryId?.let { l.industry = lookups.resolveById("industry", it) }
        req.ratingId?.let { l.rating = lookups.resolveById("rating", it) }
        req.noOfEmployees?.let { l.noOfEmployees = it }
        req.annualRevenue?.let { l.annualRevenue = it }
        req.emailOptOut?.let { l.emailOptOut = it }
        req.skypeId?.let { l.skypeId = it }
        req.twitter?.let { l.twitter = it }
        req.street?.let { l.street = it }; req.city?.let { l.city = it }; req.state?.let { l.state = it }
        req.country?.let { l.country = it }; req.zipCode?.let { l.zipCode = it }
        req.description?.let { l.description = it }
        req.tag?.let { l.tag = it }
    }
}
