package com.pangreksa.crm.analytics

import com.pangreksa.crm.activity.call.domain.Call
import com.pangreksa.crm.activity.meeting.domain.Meeting
import com.pangreksa.crm.activity.task.domain.Task
import com.pangreksa.crm.deal.domain.Deal
import com.pangreksa.crm.lead.domain.Lead
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDate

/**
 * Read-only analytics repositories. Separate from the domain repositories so all aggregation lives in
 * the analytics package. EVERY query threads the row-level ownership scope (`:allOwners` + `:owners`,
 * plus an optional `:ownerId` narrowing) so reports never see beyond the caller's visibility.
 */

interface DealAnalyticsRepository : Repository<Deal, Long> {

    @Query(
        """
        select
          count(case when d.stage.id in :openIds then 1 end) as openCount,
          count(case when d.stage.id in :wonIds  then 1 end) as wonCount,
          count(d) as totalCount,
          coalesce(sum(case when d.stage.id in :openIds then d.amount else 0 end), 0) as pipeline,
          coalesce(sum(case when d.stage.id in :openIds then d.expectedRevenue else 0 end), 0) as expected
        from Deal d
        where (:allOwners = true or d.owner.id in :owners)
          and (:ownerId is null or d.owner.id = :ownerId)
          and (cast(:from as date) is null or d.closingDate >= :from)
          and (cast(:to   as date) is null or d.closingDate <= :to)
        """,
    )
    fun summaryTotals(
        @Param("allOwners") allOwners: Boolean, @Param("owners") owners: Collection<Long>,
        @Param("ownerId") ownerId: Long?, @Param("openIds") openIds: Collection<Long>,
        @Param("wonIds") wonIds: Collection<Long>, @Param("from") from: LocalDate?, @Param("to") to: LocalDate?,
    ): DealSummary

    @Query(
        """
        select d.stage.label as label, d.stage.sortOrder as sortOrder, count(d) as count,
               coalesce(sum(d.amount), 0) as amount
        from Deal d
        where (:allOwners = true or d.owner.id in :owners)
          and (:ownerId is null or d.owner.id = :ownerId)
          and (cast(:from as date) is null or d.closingDate >= :from)
          and (cast(:to   as date) is null or d.closingDate <= :to)
          and d.stage.id in :openIds
        group by d.stage.label, d.stage.sortOrder
        order by d.stage.sortOrder
        """,
    )
    fun pipelineByStage(
        @Param("allOwners") allOwners: Boolean, @Param("owners") owners: Collection<Long>,
        @Param("ownerId") ownerId: Long?, @Param("openIds") openIds: Collection<Long>,
        @Param("from") from: LocalDate?, @Param("to") to: LocalDate?,
    ): List<AmountBucket>

    @Query(
        """
        select d.owner.fullName as owner, count(d) as count, coalesce(sum(d.amount), 0) as total,
               coalesce(sum(case when d.stage.id in :wonIds then d.amount else 0 end), 0) as won
        from Deal d
        where (:allOwners = true or d.owner.id in :owners)
          and (:ownerId is null or d.owner.id = :ownerId)
          and (cast(:from as date) is null or d.closingDate >= :from)
          and (cast(:to   as date) is null or d.closingDate <= :to)
          and d.owner is not null
        group by d.owner.fullName
        """,
    )
    fun salesByOwner(
        @Param("allOwners") allOwners: Boolean, @Param("owners") owners: Collection<Long>,
        @Param("ownerId") ownerId: Long?, @Param("wonIds") wonIds: Collection<Long>,
        @Param("from") from: LocalDate?, @Param("to") to: LocalDate?,
    ): List<OwnerSales>
}

interface LeadAnalyticsRepository : Repository<Lead, Long> {

    @Query(
        """
        select l.leadStatus.label as label, l.leadStatus.sortOrder as sortOrder, count(l) as count
        from Lead l
        where (:allOwners = true or l.owner.id in :owners)
          and (:ownerId is null or l.owner.id = :ownerId)
          and (cast(:from as date) is null or cast(l.createdAt as date) >= :from)
          and (cast(:to   as date) is null or cast(l.createdAt as date) <= :to)
          and l.leadStatus is not null
        group by l.leadStatus.label, l.leadStatus.sortOrder
        order by l.leadStatus.sortOrder
        """,
    )
    fun countByStatus(
        @Param("allOwners") allOwners: Boolean, @Param("owners") owners: Collection<Long>,
        @Param("ownerId") ownerId: Long?, @Param("from") from: LocalDate?, @Param("to") to: LocalDate?,
    ): List<Bucket>

    @Query(
        "select count(l) from Lead l where (:allOwners = true or l.owner.id in :owners) " +
            "and (:ownerId is null or l.owner.id = :ownerId) and l.converted = true",
    )
    fun countConverted(
        @Param("allOwners") allOwners: Boolean, @Param("owners") owners: Collection<Long>, @Param("ownerId") ownerId: Long?,
    ): Long

    @Query(
        "select count(l) from Lead l where (:allOwners = true or l.owner.id in :owners) " +
            "and (:ownerId is null or l.owner.id = :ownerId) and l.converted = false",
    )
    fun countOpen(
        @Param("allOwners") allOwners: Boolean, @Param("owners") owners: Collection<Long>, @Param("ownerId") ownerId: Long?,
    ): Long
}

interface TaskAnalyticsRepository : Repository<Task, Long> {
    @Query(
        "select count(t) from Task t where (:allOwners = true or t.owner.id in :owners) " +
            "and (:ownerId is null or t.owner.id = :ownerId)",
    )
    fun countScoped(
        @Param("allOwners") allOwners: Boolean, @Param("owners") owners: Collection<Long>, @Param("ownerId") ownerId: Long?,
    ): Long

    @Query(
        """
        select t.status.label as label, t.status.sortOrder as sortOrder, count(t) as count
        from Task t
        where (:allOwners = true or t.owner.id in :owners)
          and (:ownerId is null or t.owner.id = :ownerId)
          and t.status is not null
        group by t.status.label, t.status.sortOrder
        order by t.status.sortOrder
        """,
    )
    fun countByStatus(
        @Param("allOwners") allOwners: Boolean, @Param("owners") owners: Collection<Long>, @Param("ownerId") ownerId: Long?,
    ): List<Bucket>
}

interface MeetingAnalyticsRepository : Repository<Meeting, Long> {
    @Query(
        "select count(m) from Meeting m where (:allOwners = true or m.owner.id in :owners) " +
            "and (:ownerId is null or m.owner.id = :ownerId)",
    )
    fun countScoped(
        @Param("allOwners") allOwners: Boolean, @Param("owners") owners: Collection<Long>, @Param("ownerId") ownerId: Long?,
    ): Long
}

interface CallAnalyticsRepository : Repository<Call, Long> {
    @Query(
        "select count(c) from Call c where (:allOwners = true or c.owner.id in :owners) " +
            "and (:ownerId is null or c.owner.id = :ownerId)",
    )
    fun countScoped(
        @Param("allOwners") allOwners: Boolean, @Param("owners") owners: Collection<Long>, @Param("ownerId") ownerId: Long?,
    ): Long
}
