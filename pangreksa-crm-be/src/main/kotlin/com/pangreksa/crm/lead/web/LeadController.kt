package com.pangreksa.crm.lead.web

import com.pangreksa.crm.lead.service.LeadService
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
@RequestMapping("/api/leads")
class LeadController(private val service: LeadService) {

    @GetMapping
    @PreAuthorize("hasAuthority('LEAD_VIEW')")
    fun list(
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "50") limit: Int,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false) leadStatusId: Long?,
        @RequestParam(required = false) leadSourceId: Long?,
        @RequestParam(required = false) ratingId: Long?,
        @RequestParam(required = false) industryId: Long?,
        @RequestParam(required = false) ownerId: Long?,
    ): ResponseEntity<List<LeadDto>> {
        val page = service.list(
            offset, limit, q, sort,
            leadStatusId, leadSourceId, ratingId, industryId, ownerId,
        )
        return ResponseEntity.ok()
            .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Total-Count")
            .header("X-Total-Count", page.total.toString())
            .body(page.items.map { it.toDto() })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LEAD_VIEW')")
    fun get(@PathVariable id: Long): LeadDto = service.get(id).toDto()

    @PostMapping
    @PreAuthorize("hasAuthority('LEAD_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody req: LeadRequest): LeadDto = service.create(req).toDto()

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('LEAD_EDIT')")
    fun update(@PathVariable id: Long, @RequestBody req: LeadRequest): LeadDto = service.update(id, req).toDto()

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LEAD_DELETE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = service.delete(id)
}
