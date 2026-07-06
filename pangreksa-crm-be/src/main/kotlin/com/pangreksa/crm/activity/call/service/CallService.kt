package com.pangreksa.crm.activity.call.service

import com.pangreksa.crm.activity.call.domain.Call
import com.pangreksa.crm.activity.call.domain.CallRepository
import com.pangreksa.crm.activity.call.web.CallRequest
import com.pangreksa.crm.audit.AuditService
import com.pangreksa.crm.base.NotFoundException
import com.pangreksa.crm.base.Page
import com.pangreksa.crm.base.ValidationException
import com.pangreksa.crm.base.pageRequest
import com.pangreksa.crm.lookup.web.LookupService
import com.pangreksa.crm.security.AccessService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CallService(
    private val repository: CallRepository,
    private val access: AccessService,
    private val lookups: LookupService,
    private val audit: AuditService,
) {
    private val sortable = setOf("subject", "startAt", "createdAt", "updatedAt")

    @Transactional(readOnly = true)
    fun list(
        offset: Int, limit: Int, q: String?, sort: String?,
        callTypeId: Long? = null, callResultId: Long? = null, ownerId: Long? = null,
        whatType: String? = null, whatId: Long? = null, whoType: String? = null, whoId: Long? = null,
    ): Page<Call> {
        val ids = access.accessibleOwnerIds()
        val pr = pageRequest(offset, limit, sort, sortable, "startAt")
        val page = repository.search(
            ids == null, ids ?: listOf(-1L), q ?: "",
            callTypeId, callResultId, ownerId, whatType, whatId, whoType, whoId, pr,
        )
        return Page(page.content, page.totalElements)
    }

    @Transactional(readOnly = true)
    fun get(id: Long): Call {
        val c = repository.findDetailById(id) ?: throw NotFoundException("Call not found: $id")
        if (!access.canAccess(c.owner?.id)) throw NotFoundException("Call not found: $id")
        return c
    }

    @Transactional
    fun create(req: CallRequest): Call {
        val subject = req.subject?.trim().orEmpty()
        if (subject.isEmpty()) throw ValidationException("subject", "Subject is required")
        val c = Call(subject = subject)
        c.owner = access.currentUser()
        c.callType = req.callTypeId?.let { lookups.resolveById("call_type", it) }
            ?: lookups.resolve("call_type", "Outbound")
        apply(c, req)
        val saved = repository.save(c)
        audit.record("calls", saved.id!!, "created", mapOf("subject" to saved.subject))
        return saved
    }

    @Transactional
    fun update(id: Long, req: CallRequest): Call {
        val c = get(id)
        req.subject?.let { if (it.isBlank()) throw ValidationException("subject", "Subject is required"); c.subject = it.trim() }
        req.callTypeId?.let { c.callType = lookups.resolveById("call_type", it) }
        apply(c, req)
        val saved = repository.save(c)
        audit.record("calls", saved.id!!, "updated", mapOf("subject" to saved.subject))
        return saved
    }

    @Transactional
    fun delete(id: Long) {
        val c = get(id)
        repository.deleteById(id)
        audit.record("calls", id, "deleted", mapOf("subject" to c.subject))
    }

    private fun apply(c: Call, req: CallRequest) {
        req.startAt?.let { c.startAt = it }
        req.durationMinutes?.let { c.durationMinutes = it }
        req.callPurposeId?.let { c.callPurpose = lookups.resolveById("call_purpose", it) }
        req.callResultId?.let { c.callResult = lookups.resolveById("call_result", it) }
        req.whoType?.let { c.whoType = it.ifBlank { null } }
        req.whoId?.let { c.whoId = it }
        req.whatType?.let { c.whatType = it.ifBlank { null } }
        req.whatId?.let { c.whatId = it }
        req.description?.let { c.description = it }
        req.tag?.let { c.tag = it }
    }

    @Transactional(readOnly = true)
    fun forWhat(whatType: String, whatId: Long): List<Call> =
        repository.findByWhatTypeAndWhatIdOrderByCreatedAtDesc(whatType, whatId)
}
