package com.pangreksa.crm.contact.web

import com.pangreksa.crm.contact.service.ContactService
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
@RequestMapping("/api/contacts")
class ContactController(private val service: ContactService) {

    @GetMapping
    @PreAuthorize("hasAuthority('CONTACT_VIEW')")
    fun list(
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "50") limit: Int,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<List<ContactDto>> {
        val page = service.list(offset, limit, q, sort)
        return ResponseEntity.ok()
            .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Total-Count")
            .header("X-Total-Count", page.total.toString())
            .body(page.items.map { it.toDto() })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTACT_VIEW')")
    fun get(@PathVariable id: Long): ContactDto = service.get(id).toDto()

    @PostMapping
    @PreAuthorize("hasAuthority('CONTACT_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody req: ContactRequest): ContactDto = service.create(req).toDto()

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTACT_EDIT')")
    fun update(@PathVariable id: Long, @RequestBody req: ContactRequest): ContactDto = service.update(id, req).toDto()

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTACT_DELETE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = service.delete(id)
}
