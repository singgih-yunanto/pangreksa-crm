package com.pangreksa.crm.audit

import com.pangreksa.crm.security.CurrentUser
import org.springframework.stereotype.Service

/** Records audit events for record mutations. Actor is taken from the current JWT principal. */
@Service
class AuditService(private val repository: AuditEventRepository) {

    /** Record a mutation. [changes] is a small summary map (e.g. {"name": "..."}), not a full diff. */
    fun record(module: String, recordId: Long, action: String, changes: Map<String, Any?> = emptyMap()) {
        val p = CurrentUser.currentOrNull()
        repository.save(
            AuditEvent(
                module = module, recordId = recordId, action = action,
                actorId = p?.userId, actorName = p?.fullName, changes = changes.toMutableMap(),
            ),
        )
    }

    fun forRecord(module: String, recordId: Long): List<AuditEvent> =
        repository.findByModuleAndRecordIdOrderByAtDesc(module, recordId)
}
