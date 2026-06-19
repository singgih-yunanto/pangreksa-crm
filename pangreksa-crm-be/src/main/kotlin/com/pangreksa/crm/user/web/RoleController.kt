package com.pangreksa.crm.user.web

import com.pangreksa.crm.security.Permissions
import com.pangreksa.crm.user.domain.Role
import com.pangreksa.crm.user.service.RoleRequest
import com.pangreksa.crm.user.service.RoleService
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

data class RoleDto(
    val id: Long, val name: String, val description: String?,
    val parentRoleId: Long?, val parentRoleName: String?, val permissions: List<String>,
)

fun Role.toDto() = RoleDto(id!!, name, description, parentRole?.id, parentRole?.name, permissions.sorted())

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasAuthority('ADMIN_ROLES')")
class RoleController(private val service: RoleService) {
    @GetMapping fun list(): List<RoleDto> = service.list().map { it.toDto() }
    @GetMapping("/permissions") fun permissions(): List<String> = Permissions.ALL
    @GetMapping("/{id}") fun get(@PathVariable id: Long): RoleDto = service.get(id).toDto()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody req: RoleRequest): RoleDto = service.create(req).toDto()

    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody req: RoleRequest): RoleDto = service.update(id, req).toDto()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = service.delete(id)
}
