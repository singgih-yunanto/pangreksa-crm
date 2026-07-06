package com.pangreksa.crm.note.domain

import com.pangreksa.crm.base.BaseEntity
import com.pangreksa.crm.user.domain.AppUser
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/** A free-text note attached to any record (polymorphic parent). Surfaces in the record Timeline. */
@Entity
@Table(name = "notes")
class Note(
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id") var owner: AppUser? = null,
    @Column(name = "parent_type", nullable = false) var parentType: String = "",
    @Column(name = "parent_id", nullable = false) var parentId: Long = 0,
    @Column(columnDefinition = "text", nullable = false) var body: String = "",
) : BaseEntity()

interface NoteRepository : JpaRepository<Note, Long> {
    @Query(
        "select n from Note n left join fetch n.owner " +
            "where n.parentType = :parentType and n.parentId = :parentId order by n.createdAt desc",
    )
    fun findByParentTypeAndParentIdOrderByCreatedAtDesc(
        @Param("parentType") parentType: String, @Param("parentId") parentId: Long,
    ): List<Note>
}
