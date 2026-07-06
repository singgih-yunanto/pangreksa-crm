package com.pangreksa.crm.contact.service

import com.pangreksa.crm.account.domain.AccountRepository
import com.pangreksa.crm.audit.AuditService
import com.pangreksa.crm.base.NotFoundException
import com.pangreksa.crm.base.Page
import com.pangreksa.crm.base.ValidationException
import com.pangreksa.crm.base.pageRequest
import com.pangreksa.crm.contact.domain.Contact
import com.pangreksa.crm.contact.domain.ContactRepository
import com.pangreksa.crm.contact.web.ContactRequest
import com.pangreksa.crm.lookup.web.LookupService
import com.pangreksa.crm.security.AccessService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ContactService(
    private val repository: ContactRepository,
    private val accountRepository: AccountRepository,
    private val access: AccessService,
    private val lookups: LookupService,
    private val audit: AuditService,
) {
    private val sortable = setOf("lastName", "firstName", "title", "createdAt", "updatedAt")

    private fun name(c: Contact) = listOfNotNull(c.firstName, c.lastName).joinToString(" ").trim()

    @Transactional(readOnly = true)
    fun list(
        offset: Int, limit: Int, q: String?, sort: String?,
        leadSourceId: Long? = null, ownerId: Long? = null,
    ): Page<Contact> {
        val ids = access.accessibleOwnerIds()
        val pr = pageRequest(offset, limit, sort, sortable, "lastName")
        val page = repository.search(
            ids == null, ids ?: listOf(-1L), q ?: "",
            leadSourceId, ownerId, pr,
        )
        return Page(page.content, page.totalElements)
    }

    @Transactional(readOnly = true)
    fun get(id: Long): Contact {
        val c = repository.findDetailById(id) ?: throw NotFoundException("Contact not found: $id")
        if (!access.canAccess(c.owner?.id)) throw NotFoundException("Contact not found: $id")
        return c
    }

    @Transactional
    fun create(req: ContactRequest): Contact {
        val lastName = req.lastName?.trim().orEmpty()
        if (lastName.isEmpty()) throw ValidationException("lastName", "Last name is required")
        val c = Contact(lastName = lastName)
        c.owner = access.currentUser()
        apply(c, req)
        val saved = repository.save(c)
        audit.record("contacts", saved.id!!, "created", mapOf("name" to name(saved)))
        return saved
    }

    @Transactional
    fun update(id: Long, req: ContactRequest): Contact {
        val c = get(id)
        req.lastName?.let { if (it.isBlank()) throw ValidationException("lastName", "Last name is required"); c.lastName = it.trim() }
        apply(c, req)
        val saved = repository.save(c)
        audit.record("contacts", saved.id!!, "updated", mapOf("name" to name(saved)))
        return saved
    }

    @Transactional
    fun delete(id: Long) {
        val c = get(id)
        repository.deleteById(id)
        audit.record("contacts", id, "deleted", mapOf("name" to name(c)))
    }

    private fun apply(c: Contact, req: ContactRequest) {
        req.firstName?.let { c.firstName = it.trim() }
        req.accountId?.let { c.account = accountRepository.findById(it).orElseThrow { ValidationException("accountId", "Account not found: $it") } }
        req.title?.let { c.title = it.trim() }
        req.department?.let { c.department = it.trim() }
        req.email?.let { c.email = it.trim() }
        req.secondaryEmail?.let { c.secondaryEmail = it.trim() }
        req.phone?.let { c.phone = it.trim() }
        req.mobile?.let { c.mobile = it.trim() }
        req.homePhone?.let { c.homePhone = it.trim() }
        req.otherPhone?.let { c.otherPhone = it.trim() }
        req.fax?.let { c.fax = it.trim() }
        req.dateOfBirth?.let { c.dateOfBirth = it }
        req.assistant?.let { c.assistant = it.trim() }
        req.leadSourceId?.let { c.leadSource = lookups.resolveById("lead_source", it) }
        req.reportingToId?.let { c.reportingTo = get(it) }
        req.mailingStreet?.let { c.mailingStreet = it }; req.mailingCity?.let { c.mailingCity = it }
        req.mailingState?.let { c.mailingState = it }; req.mailingCountry?.let { c.mailingCountry = it }
        req.mailingCode?.let { c.mailingCode = it }
        req.otherStreet?.let { c.otherStreet = it }; req.otherCity?.let { c.otherCity = it }
        req.otherState?.let { c.otherState = it }; req.otherCountry?.let { c.otherCountry = it }
        req.otherCode?.let { c.otherCode = it }
        req.emailOptOut?.let { c.emailOptOut = it }
        req.description?.let { c.description = it }
        req.tag?.let { c.tag = it }
    }
}
