package com.pangreksa.crm.activity.task.domain

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
import java.time.LocalDate

@Entity
@Table(name = "tasks")
class Task(
    @Column(nullable = false) var subject: String = "",
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "status_id") var status: Lookup? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "priority_id") var priority: Lookup? = null,
    @Column(name = "due_date") var dueDate: LocalDate? = null,
    @Column(name = "who_type") var whoType: String? = null,
    @Column(name = "who_id") var whoId: Long? = null,
    @Column(name = "what_type") var whatType: String? = null,
    @Column(name = "what_id") var whatId: Long? = null,
    @Column(columnDefinition = "text") var description: String? = null,
) : OwnedEntity()

private const val FILTERS =
    "and (:statusId is null or t.status.id = :statusId) " +
    "and (:priorityId is null or t.priority.id = :priorityId) " +
    "and (:ownerId is null or t.owner.id = :ownerId) " +
    "and (:whatType is null or (t.whatType = :whatType and t.whatId = :whatId)) " +
    "and (:whoType is null or (t.whoType = :whoType and t.whoId = :whoId))"

interface TaskRepository : JpaRepository<Task, Long> {
    @Query(
        value = "select t from Task t " +
            "left join fetch t.status left join fetch t.priority left join fetch t.owner " +
            "where (:allOwners = true or t.owner.id in :owners) " +
            "and (:q = '' or lower(t.subject) like lower(concat('%', :q, '%'))) " +
            FILTERS,
        countQuery = "select count(t) from Task t where (:allOwners = true or t.owner.id in :owners) " +
            "and (:q = '' or lower(t.subject) like lower(concat('%', :q, '%'))) " +
            FILTERS,
    )
    fun search(
        @Param("allOwners") allOwners: Boolean,
        @Param("owners") owners: Collection<Long>,
        @Param("q") q: String,
        @Param("statusId") statusId: Long?,
        @Param("priorityId") priorityId: Long?,
        @Param("ownerId") ownerId: Long?,
        @Param("whatType") whatType: String?,
        @Param("whatId") whatId: Long?,
        @Param("whoType") whoType: String?,
        @Param("whoId") whoId: Long?,
        pageable: Pageable,
    ): Page<Task>

    @Query(
        "select t from Task t left join fetch t.status left join fetch t.priority left join fetch t.owner " +
            "where t.id = :id",
    )
    fun findDetailById(@Param("id") id: Long): Task?

    fun findByWhatTypeAndWhatIdOrderByCreatedAtDesc(whatType: String, whatId: Long): List<Task>
}
