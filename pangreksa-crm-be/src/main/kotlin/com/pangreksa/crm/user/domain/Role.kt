package com.pangreksa.crm.user.domain

import com.pangreksa.crm.base.BaseEntity
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "roles")
class Role(
    @Column(nullable = false, unique = true)
    var name: String = "",
    var description: String? = null,
    /** Parent in the role hierarchy; a user with this role can see records owned by users in descendant roles. */
    @ManyToOne
    @JoinColumn(name = "parent_role_id")
    var parentRole: Role? = null,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = [JoinColumn(name = "role_id")])
    @Column(name = "permission")
    var permissions: MutableSet<String> = mutableSetOf(),
) : BaseEntity()
