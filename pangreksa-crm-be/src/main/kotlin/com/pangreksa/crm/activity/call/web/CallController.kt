package com.pangreksa.crm.activity.call.web

import com.pangreksa.crm.activity.ActivityLinkResolver
import com.pangreksa.crm.activity.call.service.CallService
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
@RequestMapping("/api/calls")
class CallController(
    private val service: CallService,
    private val links: ActivityLinkResolver,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('CALL_VIEW')")
    fun list(
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "50") limit: Int,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false) callTypeId: Long?,
        @RequestParam(required = false) callResultId: Long?,
        @RequestParam(required = false) ownerId: Long?,
        @RequestParam(required = false) whatType: String?,
        @RequestParam(required = false) whatId: Long?,
        @RequestParam(required = false) whoType: String?,
        @RequestParam(required = false) whoId: Long?,
    ): ResponseEntity<List<CallDto>> {
        val page = service.list(offset, limit, q, sort, callTypeId, callResultId, ownerId, whatType, whatId, whoType, whoId)
        return ResponseEntity.ok()
            .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Total-Count")
            .header("X-Total-Count", page.total.toString())
            .body(page.items.map { it.toDto(links) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CALL_VIEW')")
    fun get(@PathVariable id: Long): CallDto = service.get(id).toDto(links)

    @PostMapping
    @PreAuthorize("hasAuthority('CALL_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody req: CallRequest): CallDto = service.create(req).toDto(links)

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('CALL_EDIT')")
    fun update(@PathVariable id: Long, @RequestBody req: CallRequest): CallDto = service.update(id, req).toDto(links)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CALL_DELETE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = service.delete(id)
}
