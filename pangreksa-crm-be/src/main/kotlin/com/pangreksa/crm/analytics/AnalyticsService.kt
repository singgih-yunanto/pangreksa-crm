package com.pangreksa.crm.analytics

import com.pangreksa.crm.analytics.web.ActivityStatsDto
import com.pangreksa.crm.analytics.web.BucketDto
import com.pangreksa.crm.analytics.web.LeadStatsDto
import com.pangreksa.crm.analytics.web.OwnerSalesDto
import com.pangreksa.crm.analytics.web.SummaryDto
import com.pangreksa.crm.lookup.web.LookupService
import com.pangreksa.crm.security.AccessService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Aggregates dashboard/report metrics. Every call resolves the caller's ownership scope via
 * [AccessService.accessibleOwnerIds] and threads it into the analytics queries, so reports can never
 * surface data outside the user's row-level visibility (own + role descendants; admin/VIEW_ALL = all).
 */
@Service
class AnalyticsService(
    private val deals: DealAnalyticsRepository,
    private val leads: LeadAnalyticsRepository,
    private val tasks: TaskAnalyticsRepository,
    private val meetings: MeetingAnalyticsRepository,
    private val calls: CallAnalyticsRepository,
    private val access: AccessService,
    private val lookups: LookupService,
) {
    private class Scope(val allOwners: Boolean, val owners: Collection<Long>)

    /** Owner ids the caller may aggregate over; empty→sentinel so `in ()` never breaks. */
    private fun scope(): Scope {
        val ids = access.accessibleOwnerIds()
        return Scope(ids == null, (ids ?: emptySet()).toList().ifEmpty { listOf(-1L) })
    }

    private fun stageIdsWhere(pred: (Map<String, Any?>) -> Boolean): Collection<Long> =
        lookups.byCategory("deal_stage").filter { pred(it.extra) }.mapNotNull { it.id }.ifEmpty { listOf(-1L) }

    private fun openStageIds() = stageIdsWhere { (it["closed"] as? Boolean) != true }
    private fun wonStageIds() = stageIdsWhere { (it["won"] as? Boolean) == true }

    @Transactional(readOnly = true)
    fun summary(ownerId: Long?, from: LocalDate?, to: LocalDate?): SummaryDto {
        val s = scope()
        val t = deals.summaryTotals(s.allOwners, s.owners, ownerId, openStageIds(), wonStageIds(), from, to)
        val winRate = if (t.totalCount > 0) Math.round(t.wonCount * 100.0 / t.totalCount).toInt() else 0
        return SummaryDto(
            pipelineValue = t.pipeline ?: BigDecimal.ZERO,
            expectedRevenue = t.expected ?: BigDecimal.ZERO,
            winRate = winRate,
            openDeals = t.openCount,
            wonDeals = t.wonCount,
            openLeads = leads.countOpen(s.allOwners, s.owners, ownerId),
        )
    }

    @Transactional(readOnly = true)
    fun pipelineByStage(ownerId: Long?, from: LocalDate?, to: LocalDate?): List<BucketDto> {
        val s = scope()
        return deals.pipelineByStage(s.allOwners, s.owners, ownerId, openStageIds(), from, to)
            .map { BucketDto(it.label ?: "—", it.count, it.amount ?: BigDecimal.ZERO) }
    }

    @Transactional(readOnly = true)
    fun salesByOwner(ownerId: Long?, from: LocalDate?, to: LocalDate?): List<OwnerSalesDto> {
        val s = scope()
        return deals.salesByOwner(s.allOwners, s.owners, ownerId, wonStageIds(), from, to)
            .map {
                val total = it.total ?: BigDecimal.ZERO
                val won = it.won ?: BigDecimal.ZERO
                OwnerSalesDto(it.owner ?: "—", it.count, total, won, total.subtract(won))
            }
            .sortedByDescending { it.total }
    }

    @Transactional(readOnly = true)
    fun leadsByStatus(ownerId: Long?, from: LocalDate?, to: LocalDate?): LeadStatsDto {
        val s = scope()
        val buckets = leads.countByStatus(s.allOwners, s.owners, ownerId, from, to)
            .map { BucketDto(it.label ?: "—", it.count) }
        return LeadStatsDto(buckets, leads.countConverted(s.allOwners, s.owners, ownerId))
    }

    @Transactional(readOnly = true)
    fun activities(ownerId: Long?): ActivityStatsDto {
        val s = scope()
        return ActivityStatsDto(
            tasks = tasks.countScoped(s.allOwners, s.owners, ownerId),
            meetings = meetings.countScoped(s.allOwners, s.owners, ownerId),
            calls = calls.countScoped(s.allOwners, s.owners, ownerId),
            tasksByStatus = tasks.countByStatus(s.allOwners, s.owners, ownerId)
                .map { BucketDto(it.label ?: "—", it.count) },
        )
    }
}
