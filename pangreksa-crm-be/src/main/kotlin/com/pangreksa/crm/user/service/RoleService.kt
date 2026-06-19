package com.pangreksa.crm.user.service

import com.pangreksa.crm.base.NotFoundException
import com.pangreksa.crm.base.ValidationException
import com.pangreksa.crm.user.domain.Role
import com.pangreksa.crm.user.domain.RoleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class RoleRequest(
    val name: String? = null, val description: String? = null,
    val parentRoleId: Long? = null, val permissions: Set<String>? = null,
)

@Service
class RoleService(private val roles: RoleRepository) {
    @Transactional(readOnly = true)
    fun list(): List<Role> = roles.findAll().sortedBy { it.name }

    @Transactional(readOnly = true)
    fun get(id: Long): Role = roles.findById(id).orElseThrow { NotFoundException("Role not found: $id") }

    @Transactional
    fun create(req: RoleRequest): Role {
        val name = req.name?.trim().orEmpty()
        if (name.isEmpty()) throw ValidationException("name", "Role name is required")
        val r = Role(name = name, description = req.description?.trim())
        apply(r, req)
        return roles.save(r)
    }

    @Transactional
    fun update(id: Long, req: RoleRequest): Role {
        val r = get(id)
        req.name?.let { if (it.isBlank()) throw ValidationException("name", "Role name is required"); r.name = it.trim() }
        req.description?.let { r.description = it.trim() }
        apply(r, req)
        return roles.save(r)
    }

    @Transactional
    fun delete(id: Long) = roles.deleteById(id)

    private fun apply(r: Role, req: RoleRequest) {
        req.parentRoleId?.let { r.parentRole = if (it == r.id) null else get(it) }
        req.permissions?.let { r.permissions = it.toMutableSet() }
    }
}
