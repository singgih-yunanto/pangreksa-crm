package com.pangreksa.crm.activity.task.service

import com.pangreksa.crm.activity.task.domain.Task
import com.pangreksa.crm.activity.task.domain.TaskRepository
import com.pangreksa.crm.activity.task.web.TaskRequest
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
class TaskService(
    private val repository: TaskRepository,
    private val access: AccessService,
    private val lookups: LookupService,
    private val audit: AuditService,
) {
    private val sortable = setOf("subject", "dueDate", "createdAt", "updatedAt")

    @Transactional(readOnly = true)
    fun list(
        offset: Int, limit: Int, q: String?, sort: String?,
        statusId: Long? = null, priorityId: Long? = null, ownerId: Long? = null,
        whatType: String? = null, whatId: Long? = null, whoType: String? = null, whoId: Long? = null,
    ): Page<Task> {
        val ids = access.accessibleOwnerIds()
        val pr = pageRequest(offset, limit, sort, sortable, "dueDate")
        val page = repository.search(
            ids == null, ids ?: listOf(-1L), q ?: "",
            statusId, priorityId, ownerId, whatType, whatId, whoType, whoId, pr,
        )
        return Page(page.content, page.totalElements)
    }

    @Transactional(readOnly = true)
    fun get(id: Long): Task {
        val t = repository.findDetailById(id) ?: throw NotFoundException("Task not found: $id")
        if (!access.canAccess(t.owner?.id)) throw NotFoundException("Task not found: $id")
        return t
    }

    @Transactional
    fun create(req: TaskRequest): Task {
        val subject = req.subject?.trim().orEmpty()
        if (subject.isEmpty()) throw ValidationException("subject", "Subject is required")
        val t = Task(subject = subject)
        t.owner = access.currentUser()
        t.status = req.statusId?.let { lookups.resolveById("task_status", it) }
            ?: lookups.resolve("task_status", "Not Started")
        apply(t, req)
        val saved = repository.save(t)
        audit.record("tasks", saved.id!!, "created", mapOf("subject" to saved.subject))
        return saved
    }

    @Transactional
    fun update(id: Long, req: TaskRequest): Task {
        val t = get(id)
        req.subject?.let { if (it.isBlank()) throw ValidationException("subject", "Subject is required"); t.subject = it.trim() }
        req.statusId?.let { t.status = lookups.resolveById("task_status", it) }
        apply(t, req)
        val saved = repository.save(t)
        audit.record("tasks", saved.id!!, "updated", mapOf("subject" to saved.subject))
        return saved
    }

    @Transactional
    fun delete(id: Long) {
        val t = get(id)
        repository.deleteById(id)
        audit.record("tasks", id, "deleted", mapOf("subject" to t.subject))
    }

    private fun apply(t: Task, req: TaskRequest) {
        req.priorityId?.let { t.priority = lookups.resolveById("task_priority", it) }
        req.dueDate?.let { t.dueDate = it }
        req.whoType?.let { t.whoType = it.ifBlank { null } }
        req.whoId?.let { t.whoId = it }
        req.whatType?.let { t.whatType = it.ifBlank { null } }
        req.whatId?.let { t.whatId = it }
        req.description?.let { t.description = it }
        req.tag?.let { t.tag = it }
    }

    /** Tasks linked (via what) to a parent record — for the Timeline. */
    @Transactional(readOnly = true)
    fun forWhat(whatType: String, whatId: Long): List<Task> =
        repository.findByWhatTypeAndWhatIdOrderByCreatedAtDesc(whatType, whatId)
}
