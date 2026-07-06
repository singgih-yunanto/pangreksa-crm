package com.pangreksa.crm.activity.meeting.domain

import com.pangreksa.crm.base.OwnedEntity
import com.pangreksa.crm.lookup.domain.Lookup
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

@Entity
@Table(name = "meetings")
class Meeting(
    @Column(nullable = false) var title: String = "",
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "status_id") var status: Lookup? = null,
    var location: String? = null,
    @Column(name = "start_at") var startAt: LocalDateTime? = null,
    @Column(name = "end_at") var endAt: LocalDateTime? = null,
    @Column(name = "all_day", nullable = false) var allDay: Boolean = false,
    @Column(name = "who_type") var whoType: String? = null,
    @Column(name = "who_id") var whoId: Long? = null,
    @Column(name = "what_type") var whatType: String? = null,
    @Column(name = "what_id") var whatId: Long? = null,
    @Column(columnDefinition = "text") var description: String? = null,
) : OwnedEntity()

private const val FILTERS =
    "and (:statusId is null or m.status.id = :statusId) " +
    "and (:ownerId is null or m.owner.id = :ownerId) " +
    "and (:whatType is null or (m.whatType = :whatType and m.whatId = :whatId)) " +
    "and (:whoType is null or (m.whoType = :whoType and m.whoId = :whoId))"

interface MeetingRepository : JpaRepository<Meeting, Long> {
    @Query(
        value = "select m from Meeting m left join fetch m.status left join fetch m.owner " +
            "where (:allOwners = true or m.owner.id in :owners) " +
            "and (:q = '' or lower(m.title) like lower(concat('%', :q, '%'))) " +
            FILTERS,
        countQuery = "select count(m) from Meeting m where (:allOwners = true or m.owner.id in :owners) " +
            "and (:q = '' or lower(m.title) like lower(concat('%', :q, '%'))) " +
            FILTERS,
    )
    fun search(
        @Param("allOwners") allOwners: Boolean,
        @Param("owners") owners: Collection<Long>,
        @Param("q") q: String,
        @Param("statusId") statusId: Long?,
        @Param("ownerId") ownerId: Long?,
        @Param("whatType") whatType: String?,
        @Param("whatId") whatId: Long?,
        @Param("whoType") whoType: String?,
        @Param("whoId") whoId: Long?,
        pageable: Pageable,
    ): Page<Meeting>

    @Query("select m from Meeting m left join fetch m.status left join fetch m.owner where m.id = :id")
    fun findDetailById(@Param("id") id: Long): Meeting?

    fun findByWhatTypeAndWhatIdOrderByCreatedAtDesc(whatType: String, whatId: Long): List<Meeting>
}
