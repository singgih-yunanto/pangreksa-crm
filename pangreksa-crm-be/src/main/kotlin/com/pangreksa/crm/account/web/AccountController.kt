package com.pangreksa.crm.account.web

import com.pangreksa.crm.account.service.AccountService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/accounts")
class AccountController(private val service: AccountService) {

    @GetMapping
    @PreAuthorize("hasAuthority('ACCOUNT_VIEW')")
    fun list(
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "50") limit: Int,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false) accountTypeId: Long?,
        @RequestParam(required = false) industryId: Long?,
        @RequestParam(required = false) ownershipId: Long?,
        @RequestParam(required = false) ratingId: Long?,
        @RequestParam(required = false) ownerId: Long?,
        @RequestParam(required = false) employeesMin: Int?,
        @RequestParam(required = false) employeesMax: Int?,
    ): ResponseEntity<List<AccountDto>> {
        val page = service.list(
            offset, limit, q, sort,
            accountTypeId, industryId, ownershipId, ratingId, ownerId, employeesMin, employeesMax,
        )
        return ResponseEntity.ok()
            .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Total-Count")
            .header("X-Total-Count", page.total.toString())
            .body(page.items.map { it.toDto() })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNT_VIEW')")
    fun get(@PathVariable id: Long): AccountDto = service.get(id).toDto()

    @PostMapping
    @PreAuthorize("hasAuthority('ACCOUNT_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody req: AccountRequest): AccountDto = service.create(req).toDto()

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNT_EDIT')")
    fun update(@PathVariable id: Long, @RequestBody req: AccountRequest): AccountDto = service.update(id, req).toDto()

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNT_DELETE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = service.delete(id)
}
