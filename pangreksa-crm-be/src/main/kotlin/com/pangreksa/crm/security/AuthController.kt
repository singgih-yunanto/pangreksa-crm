package com.pangreksa.crm.security

import com.pangreksa.crm.base.ValidationException
import com.pangreksa.crm.user.domain.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class LoginRequest(val username: String? = null, val password: String? = null)
data class MeDto(val id: Long, val username: String, val fullName: String, val roleName: String?, val permissions: List<String>)
data class LoginResponse(val token: String, val user: MeDto)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val users: UserRepository,
    private val encoder: PasswordEncoder,
    private val jwt: JwtService,
) {
    @PostMapping("/login")
    fun login(@RequestBody req: LoginRequest): LoginResponse {
        val username = req.username?.trim().orEmpty()
        val password = req.password.orEmpty()
        val user = users.findByUsername(username)
        if (user == null || !user.active || !encoder.matches(password, user.passwordHash)) {
            throw ValidationException(null, "Invalid username or password")
        }
        val token = jwt.issue(user)
        return LoginResponse(token, user.let {
            MeDto(it.id!!, it.username, it.fullName, it.role?.name, (it.role?.permissions ?: emptySet()).toList())
        })
    }

    @GetMapping("/me")
    fun me(): ResponseEntity<MeDto> {
        val p = CurrentUser.currentOrNull() ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return ResponseEntity.ok(MeDto(p.userId, p.username, p.fullName, p.roleName, p.permissions.toList()))
    }
}
