package com.pangreksa.crm.user.domain

import com.pangreksa.crm.base.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class AppUser(
    @Column(nullable = false, unique = true)
    var username: String = "",
    @Column(name = "full_name", nullable = false)
    var fullName: String = "",
    var email: String? = null,
    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = "",
    @Column(nullable = false)
    var active: Boolean = true,
    @ManyToOne
    @JoinColumn(name = "role_id")
    var role: Role? = null,
) : BaseEntity()
