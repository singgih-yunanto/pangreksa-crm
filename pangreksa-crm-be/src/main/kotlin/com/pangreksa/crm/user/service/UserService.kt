package com.pangreksa.crm.user.service

import com.pangreksa.crm.base.NotFoundException
import com.pangreksa.crm.base.ValidationException
import com.pangreksa.crm.user.domain.AppUser
import com.pangreksa.crm.user.domain.Role
import com.pangreksa.crm.user.domain.RoleRepository
import com.pangreksa.crm.user.domain.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class UserRequest(
    val username: String? = null, val fullName: String? = null, val email: String? = null,
    val active: Boolean? = null, val roleId: Long? = null, val password: String? = null,
)

@Service
class UserService(
    private val users: UserRepository,
    private val roles: RoleRepository,
    private val encoder: PasswordEncoder,
) {
    @Transactional(readOnly = true)
    fun list(): List<AppUser> = users.findAllByOrderByUsernameAsc()

    @Transactional(readOnly = true)
    fun get(id: Long): AppUser = users.findById(id).orElseThrow { NotFoundException("User not found: $id") }

    @Transactional
    fun create(req: UserRequest): AppUser {
        val username = req.username?.trim().orEmpty()
        if (username.isEmpty()) throw ValidationException("username", "Username is required")
        if (users.existsByUsername(username)) throw ValidationException("username", "Username already exists")
        val fullName = req.fullName?.trim().orEmpty()
        if (fullName.isEmpty()) throw ValidationException("fullName", "Full name is required")
        val password = req.password.orEmpty()
        if (password.isBlank()) throw ValidationException("password", "Password is required")
        val u = AppUser(
            username = username, fullName = fullName, email = req.email?.trim(),
            passwordHash = encoder.encode(password)!!, active = req.active ?: true,
            role = req.roleId?.let { resolveRole(it) },
        )
        return users.save(u)
    }

    @Transactional
    fun update(id: Long, req: UserRequest): AppUser {
        val u = get(id)
        req.fullName?.let { if (it.isBlank()) throw ValidationException("fullName", "Full name is required"); u.fullName = it.trim() }
        req.email?.let { u.email = it.trim() }
        req.active?.let { u.active = it }
        req.roleId?.let { u.role = resolveRole(it) }
        req.password?.takeIf { it.isNotBlank() }?.let { u.passwordHash = encoder.encode(it)!! }
        return users.save(u)
    }

    @Transactional
    fun delete(id: Long) = users.deleteById(id)

    private fun resolveRole(id: Long): Role =
        roles.findById(id).orElseThrow { ValidationException("roleId", "Role not found: $id") }
}
