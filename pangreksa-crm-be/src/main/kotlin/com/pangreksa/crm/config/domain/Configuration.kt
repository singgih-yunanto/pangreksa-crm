package com.pangreksa.crm.config.domain

import com.pangreksa.crm.base.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository

/** App/system configuration as key/value (+ JSONB extra). Read-only this iteration; edit UI comes later. */
@Entity
@Table(name = "configuration")
class Configuration(
    @Column(name = "config_key", nullable = false, unique = true)
    var key: String = "",
    @Column(name = "config_value")
    var value: String? = null,
    var label: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var extra: MutableMap<String, Any?> = mutableMapOf(),
) : BaseEntity()

interface ConfigurationRepository : JpaRepository<Configuration, Long> {
    fun findByKey(key: String): Configuration?
}
