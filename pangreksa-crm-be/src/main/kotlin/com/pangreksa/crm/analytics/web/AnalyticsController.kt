package com.pangreksa.crm.analytics.web

import com.pangreksa.crm.analytics.AnalyticsService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/analytics")
class AnalyticsController(private val service: AnalyticsService) {

    /** Dashboard KPIs — available to any authenticated user (scoped to their own visibility). */
    @GetMapping("/summary")
    fun summary(
        @RequestParam(required = false) ownerId: Long?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ): SummaryDto = service.summary(ownerId, from, to)

    @GetMapping("/pipeline-by-stage")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    fun pipelineByStage(
        @RequestParam(required = false) ownerId: Long?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ): List<BucketDto> = service.pipelineByStage(ownerId, from, to)

    @GetMapping("/sales-by-owner")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    fun salesByOwner(
        @RequestParam(required = false) ownerId: Long?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ): List<OwnerSalesDto> = service.salesByOwner(ownerId, from, to)

    @GetMapping("/leads-by-status")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    fun leadsByStatus(
        @RequestParam(required = false) ownerId: Long?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ): LeadStatsDto = service.leadsByStatus(ownerId, from, to)

    @GetMapping("/activities")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    fun activities(@RequestParam(required = false) ownerId: Long?): ActivityStatsDto = service.activities(ownerId)
}
