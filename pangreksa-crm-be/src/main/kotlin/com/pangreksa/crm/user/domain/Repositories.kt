package com.pangreksa.crm.user.domain

import org.springframework.data.jpa.repository.JpaRepository

interface RoleRepository : JpaRepository<Role, Long> {
    fun findByName(name: String): Role?
}

interface UserRepository : JpaRepository<AppUser, Long> {
    fun findByUsername(username: String): AppUser?
    fun existsByUsername(username: String): Boolean
    fun findAllByOrderByUsernameAsc(): List<AppUser>
    fun findByRoleIdIn(roleIds: Collection<Long>): List<AppUser>
}
