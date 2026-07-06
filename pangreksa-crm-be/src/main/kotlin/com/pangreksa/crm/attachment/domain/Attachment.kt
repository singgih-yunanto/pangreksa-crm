package com.pangreksa.crm.attachment.domain

import com.pangreksa.crm.base.BaseEntity
import com.pangreksa.crm.user.domain.AppUser
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository

/** File attachment metadata for any record (polymorphic parent). Bytes live in the BlobStore. */
@Entity
@Table(name = "attachments")
class Attachment(
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id") var owner: AppUser? = null,
    @Column(name = "parent_type", nullable = false) var parentType: String = "",
    @Column(name = "parent_id", nullable = false) var parentId: Long = 0,
    @Column(nullable = false) var filename: String = "",
    @Column(name = "content_type") var contentType: String? = null,
    @Column(name = "size_bytes", nullable = false) var sizeBytes: Long = 0,
    @Column(name = "storage_key", nullable = false) var storageKey: String = "",
) : BaseEntity()

interface AttachmentRepository : JpaRepository<Attachment, Long> {
    fun findByParentTypeAndParentIdOrderByCreatedAtDesc(parentType: String, parentId: Long): List<Attachment>
}
