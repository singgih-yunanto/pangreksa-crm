package com.pangreksa.crm.attachment.web

import com.pangreksa.crm.attachment.domain.Attachment
import com.pangreksa.crm.attachment.domain.AttachmentRepository
import com.pangreksa.crm.base.NotFoundException
import com.pangreksa.crm.base.ValidationException
import com.pangreksa.crm.security.AccessService
import com.pangreksa.crm.security.CurrentUser
import com.pangreksa.crm.security.Permissions
import com.pangreksa.crm.storage.BlobStore
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.util.UUID

private const val MAX_BYTES = 10L * 1024 * 1024

data class AttachmentDto(
    val id: Long, val parentType: String, val parentId: Long,
    val filename: String, val contentType: String?, val sizeBytes: Long,
    val ownerId: Long?, val ownerName: String?, val createdAt: LocalDateTime,
)

fun Attachment.toDto() = AttachmentDto(
    id!!, parentType, parentId, filename, contentType, sizeBytes, owner?.id, owner?.fullName, createdAt,
)

@Service
class AttachmentService(
    private val repository: AttachmentRepository,
    private val access: AccessService,
    private val blobs: BlobStore,
) {
    @Transactional(readOnly = true)
    fun forParent(type: String, id: Long): List<Attachment> =
        repository.findByParentTypeAndParentIdOrderByCreatedAtDesc(type, id)

    @Transactional
    fun upload(parentType: String, parentId: Long, file: MultipartFile): Attachment {
        if (parentType.isBlank()) throw ValidationException("parentType", "A parent record is required")
        if (file.isEmpty) throw ValidationException("file", "File is empty")
        if (file.size > MAX_BYTES) throw ValidationException("file", "File exceeds the 10 MB limit")
        val original = file.originalFilename?.substringAfterLast('/')?.substringAfterLast('\\') ?: "file"
        val ext = original.substringAfterLast('.', "").let { if (it.isBlank()) "" else ".$it" }
        val key = "att/${UUID.randomUUID()}$ext"
        file.inputStream.use { blobs.put(key, it, file.contentType) }
        val a = Attachment(
            parentType = parentType, parentId = parentId, filename = original,
            contentType = file.contentType, sizeBytes = file.size, storageKey = key,
        )
        a.owner = access.currentUser()
        return repository.save(a)
    }

    @Transactional(readOnly = true)
    fun get(id: Long): Attachment =
        repository.findById(id).orElseThrow { NotFoundException("Attachment not found: $id") }

    fun open(a: Attachment) = blobs.get(a.storageKey)

    @Transactional
    fun delete(id: Long) {
        val a = get(id)
        val p = CurrentUser.require()
        val isOwnerOrAdmin = a.owner?.id == p.userId || Permissions.VIEW_ALL in p.permissions
        if (!isOwnerOrAdmin) throw NotFoundException("Attachment not found: $id")
        repository.deleteById(id)
        blobs.delete(a.storageKey)
    }
}

@RestController
@RequestMapping("/api/attachments")
class AttachmentController(private val service: AttachmentService) {
    @GetMapping
    fun list(@RequestParam type: String, @RequestParam id: Long): List<AttachmentDto> =
        service.forParent(type, id).map { it.toDto() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun upload(
        @RequestParam parentType: String,
        @RequestParam parentId: Long,
        @RequestParam file: MultipartFile,
    ): AttachmentDto = service.upload(parentType, parentId, file).toDto()

    @GetMapping("/{id}/download")
    fun download(@PathVariable id: Long): ResponseEntity<InputStreamResource> {
        val a = service.get(id)
        val media = a.contentType?.let { runCatching { MediaType.parseMediaType(it) }.getOrNull() }
            ?: MediaType.APPLICATION_OCTET_STREAM
        return ResponseEntity.ok()
            .contentType(media)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${a.filename}\"")
            .header(HttpHeaders.CONTENT_LENGTH, a.sizeBytes.toString())
            .body(InputStreamResource(service.open(a)))
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = service.delete(id)
}
