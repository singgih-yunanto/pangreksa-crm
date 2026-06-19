package com.pangreksa.crm.lookup.domain

import com.pangreksa.crm.base.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Normalized lookup/picklist values (stage, lead status, account type, …).
 * [extra] JSONB carries additional data — e.g. a stage's `{closed, won, probability}` or a status's `{closed}`.
 */
@Entity
@Table(name = "lookups", uniqueConstraints = [UniqueConstraint(name = "uq_lookup_cat_code", columnNames = ["category", "code"])])
class Lookup(
    @Column(nullable = false)
    var category: String = "",
    @Column(nullable = false)
    var code: String = "",
    @Column(nullable = false)
    var label: String = "",
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,
    @Column(nullable = false)
    var active: Boolean = true,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var extra: MutableMap<String, Any?> = mutableMapOf(),
) : BaseEntity()

interface LookupRepository : JpaRepository<Lookup, Long> {
    fun findByCategoryAndActiveTrueOrderBySortOrderAsc(category: String): List<Lookup>
    fun findByCategoryAndCode(category: String, code: String): Lookup?
}
