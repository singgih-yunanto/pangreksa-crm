package com.pangreksa.crm.activity.meeting.service

import com.pangreksa.crm.activity.meeting.domain.Meeting
import com.pangreksa.crm.activity.meeting.domain.MeetingRepository
import com.pangreksa.crm.activity.meeting.web.MeetingRequest
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
class MeetingService(
    private val repository: MeetingRepository,
    private val access: AccessService,
    private val lookups: LookupService,
    private val audit: AuditService,
) {
    private val sortable = setOf("title", "startAt", "createdAt", "updatedAt")

    @Transactional(readOnly = true)
    fun list(
        offset: Int, limit: Int, q: String?, sort: String?,
        statusId: Long? = null, ownerId: Long? = null,
        whatType: String? = null, whatId: Long? = null, whoType: String? = null, whoId: Long? = null,
    ): Page<Meeting> {
        val ids = access.accessibleOwnerIds()
        val pr = pageRequest(offset, limit, sort, sortable, "startAt")
        val page = repository.search(
            ids == null, ids ?: listOf(-1L), q ?: "",
            statusId, ownerId, whatType, whatId, whoType, whoId, pr,
        )
        return Page(page.content, page.totalElements)
    }

    @Transactional(readOnly = true)
    fun get(id: Long): Meeting {
        val m = repository.findDetailById(id) ?: throw NotFoundException("Meeting not found: $id")
        if (!access.canAccess(m.owner?.id)) throw NotFoundException("Meeting not found: $id")
        return m
    }

    @Transactional
    fun create(req: MeetingRequest): Meeting {
        val title = req.title?.trim().orEmpty()
        if (title.isEmpty()) throw ValidationException("title", "Title is required")
        val m = Meeting(title = title)
        m.owner = access.currentUser()
        m.status = req.statusId?.let { lookups.resolveById("meeting_status", it) }
            ?: lookups.resolve("meeting_status", "Planned")
        apply(m, req)
        val saved = repository.save(m)
        audit.record("meetings", saved.id!!, "created", mapOf("title" to saved.title))
        return saved
    }

    @Transactional
    fun update(id: Long, req: MeetingRequest): Meeting {
        val m = get(id)
        req.title?.let { if (it.isBlank()) throw ValidationException("title", "Title is required"); m.title = it.trim() }
        req.statusId?.let { m.status = lookups.resolveById("meeting_status", it) }
        apply(m, req)
        val saved = repository.save(m)
        audit.record("meetings", saved.id!!, "updated", mapOf("title" to saved.title))
        return saved
    }

    @Transactional
    fun delete(id: Long) {
        val m = get(id)
        repository.deleteById(id)
        audit.record("meetings", id, "deleted", mapOf("title" to m.title))
    }

    private fun apply(m: Meeting, req: MeetingRequest) {
        req.location?.let { m.location = it.trim() }
        req.startAt?.let { m.startAt = it }
        req.endAt?.let { m.endAt = it }
        req.allDay?.let { m.allDay = it }
        req.whoType?.let { m.whoType = it.ifBlank { null } }
        req.whoId?.let { m.whoId = it }
        req.whatType?.let { m.whatType = it.ifBlank { null } }
        req.whatId?.let { m.whatId = it }
        req.description?.let { m.description = it }
        req.tag?.let { m.tag = it }
    }

    @Transactional(readOnly = true)
    fun forWhat(whatType: String, whatId: Long): List<Meeting> =
        repository.findByWhatTypeAndWhatIdOrderByCreatedAtDesc(whatType, whatId)
}
