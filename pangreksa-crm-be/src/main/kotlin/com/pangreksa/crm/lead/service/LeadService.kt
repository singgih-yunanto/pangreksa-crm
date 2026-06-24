package com.pangreksa.crm.lead.service

import com.pangreksa.crm.base.NotFoundException
import com.pangreksa.crm.base.Page
import com.pangreksa.crm.base.ValidationException
import com.pangreksa.crm.base.pageRequest
import com.pangreksa.crm.lead.domain.Lead
import com.pangreksa.crm.lead.domain.LeadRepository
import com.pangreksa.crm.lead.web.LeadRequest
import com.pangreksa.crm.lookup.web.LookupService
import com.pangreksa.crm.security.AccessService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LeadService(
    private val repository: LeadRepository,
    private val access: AccessService,
    private val lookups: LookupService,
) {
    private val sortable = setOf("lastName", "company", "createdAt", "updatedAt")

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
        return repository.save(l)
    }

    @Transactional
    fun update(id: Long, req: LeadRequest): Lead {
        val l = get(id)
        req.lastName?.let { if (it.isBlank()) throw ValidationException("lastName", "Last name is required"); l.lastName = it.trim() }
        req.leadStatusId?.let { l.leadStatus = lookups.resolveById("lead_status", it) }
        apply(l, req)
        return repository.save(l)
    }

    @Transactional
    fun delete(id: Long) { get(id); repository.deleteById(id) }

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
