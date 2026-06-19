package com.pangreksa.crm.base

import com.pangreksa.crm.user.domain.AppUser
import jakarta.persistence.Column
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/** A business record with an owner (for RBAC row-level visibility), tags, lock, and flexible JSONB data. */
@MappedSuperclass
abstract class OwnedEntity : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: AppUser? = null

    var tag: String? = null

    @Column(name = "locked", nullable = false)
    var locked: Boolean = false

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_fields", columnDefinition = "jsonb")
    var customFields: MutableMap<String, String?> = mutableMapOf()
}
