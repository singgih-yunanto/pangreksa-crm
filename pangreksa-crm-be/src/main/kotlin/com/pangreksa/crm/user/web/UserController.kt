package com.pangreksa.crm.user.web

import com.pangreksa.crm.user.domain.AppUser
import com.pangreksa.crm.user.service.UserRequest
import com.pangreksa.crm.user.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

data class UserDto(
    val id: Long, val username: String, val fullName: String, val email: String?,
    val active: Boolean, val roleId: Long?, val roleName: String?,
)

fun AppUser.toDto() = UserDto(id!!, username, fullName, email, active, role?.id, role?.name)

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAuthority('ADMIN_USERS')")
class UserController(private val service: UserService) {
    @GetMapping fun list(): List<UserDto> = service.list().map { it.toDto() }
    @GetMapping("/{id}") fun get(@PathVariable id: Long): UserDto = service.get(id).toDto()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody req: UserRequest): UserDto = service.create(req).toDto()

    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody req: UserRequest): UserDto = service.update(id, req).toDto()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = service.delete(id)
}
