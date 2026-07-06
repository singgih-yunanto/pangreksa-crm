package com.pangreksa.crm.audit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

/**
 * An immutable audit record of a create/update/delete (and later, config/login) event on a record.
 * Powers the record-detail Timeline together with activities and notes.
 */
@Entity
@Table(name = "audit_events")
class AuditEvent(
    @Column(nullable = false) var module: String = "",
    @Column(name = "record_id", nullable = false) var recordId: Long = 0,
    @Column(nullable = false) var action: String = "",
    @Column(name = "actor_id") var actorId: Long? = null,
    @Column(name = "actor_name") var actorName: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var changes: MutableMap<String, Any?> = mutableMapOf(),
    @Column(nullable = false) var at: LocalDateTime = LocalDateTime.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}

interface AuditEventRepository : JpaRepository<AuditEvent, Long> {
    fun findByModuleAndRecordIdOrderByAtDesc(module: String, recordId: Long): List<AuditEvent>
}
