package com.pangreksa.crm.activity.call.domain

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
@Table(name = "calls")
class Call(
    @Column(nullable = false) var subject: String = "",
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "call_type_id") var callType: Lookup? = null,
    @Column(name = "start_at") var startAt: LocalDateTime? = null,
    @Column(name = "duration_minutes") var durationMinutes: Int? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "call_purpose_id") var callPurpose: Lookup? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "call_result_id") var callResult: Lookup? = null,
    @Column(name = "who_type") var whoType: String? = null,
    @Column(name = "who_id") var whoId: Long? = null,
    @Column(name = "what_type") var whatType: String? = null,
    @Column(name = "what_id") var whatId: Long? = null,
    @Column(columnDefinition = "text") var description: String? = null,
) : OwnedEntity()

private const val FILTERS =
    "and (:callTypeId is null or c.callType.id = :callTypeId) " +
    "and (:callResultId is null or c.callResult.id = :callResultId) " +
    "and (:ownerId is null or c.owner.id = :ownerId) " +
    "and (:whatType is null or (c.whatType = :whatType and c.whatId = :whatId)) " +
    "and (:whoType is null or (c.whoType = :whoType and c.whoId = :whoId))"

interface CallRepository : JpaRepository<Call, Long> {
    @Query(
        value = "select c from Call c " +
            "left join fetch c.callType left join fetch c.callPurpose left join fetch c.callResult left join fetch c.owner " +
            "where (:allOwners = true or c.owner.id in :owners) " +
            "and (:q = '' or lower(c.subject) like lower(concat('%', :q, '%'))) " +
            FILTERS,
        countQuery = "select count(c) from Call c where (:allOwners = true or c.owner.id in :owners) " +
            "and (:q = '' or lower(c.subject) like lower(concat('%', :q, '%'))) " +
            FILTERS,
    )
    fun search(
        @Param("allOwners") allOwners: Boolean,
        @Param("owners") owners: Collection<Long>,
        @Param("q") q: String,
        @Param("callTypeId") callTypeId: Long?,
        @Param("callResultId") callResultId: Long?,
        @Param("ownerId") ownerId: Long?,
        @Param("whatType") whatType: String?,
        @Param("whatId") whatId: Long?,
        @Param("whoType") whoType: String?,
        @Param("whoId") whoId: Long?,
        pageable: Pageable,
    ): Page<Call>

    @Query(
        "select c from Call c " +
            "left join fetch c.callType left join fetch c.callPurpose left join fetch c.callResult left join fetch c.owner " +
            "where c.id = :id",
    )
    fun findDetailById(@Param("id") id: Long): Call?

    fun findByWhatTypeAndWhatIdOrderByCreatedAtDesc(whatType: String, whatId: Long): List<Call>
}
