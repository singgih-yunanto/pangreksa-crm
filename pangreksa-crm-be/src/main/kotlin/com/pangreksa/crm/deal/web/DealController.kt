package com.pangreksa.crm.deal.web

import com.pangreksa.crm.deal.service.DealService
import org.springframework.format.annotation.DateTimeFormat
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
@RequestMapping("/api/deals")
class DealController(private val service: DealService) {

    @GetMapping
    @PreAuthorize("hasAuthority('DEAL_VIEW')")
    fun list(
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "50") limit: Int,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false) stageId: Long?,
        @RequestParam(required = false) typeId: Long?,
        @RequestParam(required = false) leadSourceId: Long?,
        @RequestParam(required = false) ownerId: Long?,
        @RequestParam(required = false) amountMin: java.math.BigDecimal?,
        @RequestParam(required = false) amountMax: java.math.BigDecimal?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) closingFrom: java.time.LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) closingTo: java.time.LocalDate?,
    ): ResponseEntity<List<DealDto>> {
        val page = service.list(
            offset, limit, q, sort,
            stageId, typeId, leadSourceId, ownerId, amountMin, amountMax, closingFrom, closingTo,
        )
        return ResponseEntity.ok()
            .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Total-Count")
            .header("X-Total-Count", page.total.toString())
            .body(page.items.map { it.toDto() })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DEAL_VIEW')")
    fun get(@PathVariable id: Long): DealDto = service.get(id).toDto()

    @PostMapping
    @PreAuthorize("hasAuthority('DEAL_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody req: DealRequest): DealDto = service.create(req).toDto()

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('DEAL_EDIT')")
    fun update(@PathVariable id: Long, @RequestBody req: DealRequest): DealDto = service.update(id, req).toDto()

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DEAL_DELETE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = service.delete(id)
}
