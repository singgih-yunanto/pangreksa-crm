package com.pangreksa.crm.activity.meeting.web

import com.pangreksa.crm.activity.ActivityLinkResolver
import com.pangreksa.crm.activity.meeting.service.MeetingService
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
@RequestMapping("/api/meetings")
class MeetingController(
    private val service: MeetingService,
    private val links: ActivityLinkResolver,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('MEETING_VIEW')")
    fun list(
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "50") limit: Int,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false) statusId: Long?,
        @RequestParam(required = false) ownerId: Long?,
        @RequestParam(required = false) whatType: String?,
        @RequestParam(required = false) whatId: Long?,
        @RequestParam(required = false) whoType: String?,
        @RequestParam(required = false) whoId: Long?,
    ): ResponseEntity<List<MeetingDto>> {
        val page = service.list(offset, limit, q, sort, statusId, ownerId, whatType, whatId, whoType, whoId)
        return ResponseEntity.ok()
            .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Total-Count")
            .header("X-Total-Count", page.total.toString())
            .body(page.items.map { it.toDto(links) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MEETING_VIEW')")
    fun get(@PathVariable id: Long): MeetingDto = service.get(id).toDto(links)

    @PostMapping
    @PreAuthorize("hasAuthority('MEETING_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody req: MeetingRequest): MeetingDto = service.create(req).toDto(links)

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('MEETING_EDIT')")
    fun update(@PathVariable id: Long, @RequestBody req: MeetingRequest): MeetingDto = service.update(id, req).toDto(links)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MEETING_DELETE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = service.delete(id)
}
