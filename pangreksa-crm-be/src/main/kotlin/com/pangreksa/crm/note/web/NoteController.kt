package com.pangreksa.crm.note.web

import com.pangreksa.crm.base.NotFoundException
import com.pangreksa.crm.base.ValidationException
import com.pangreksa.crm.note.domain.Note
import com.pangreksa.crm.note.domain.NoteRepository
import com.pangreksa.crm.security.AccessService
import com.pangreksa.crm.security.CurrentUser
import com.pangreksa.crm.security.Permissions
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

data class NoteDto(
    val id: Long, val parentType: String, val parentId: Long, val body: String,
    val authorId: Long?, val authorName: String?, val createdAt: LocalDateTime,
)

data class NoteRequest(val parentType: String? = null, val parentId: Long? = null, val body: String? = null)

fun Note.toDto() = NoteDto(id!!, parentType, parentId, body, owner?.id, owner?.fullName, createdAt)

@Service
class NoteService(
    private val repository: NoteRepository,
    private val access: AccessService,
) {
    @Transactional(readOnly = true)
    fun forParent(type: String, id: Long): List<Note> =
        repository.findByParentTypeAndParentIdOrderByCreatedAtDesc(type, id)

    @Transactional
    fun create(req: NoteRequest): Note {
        val type = req.parentType?.trim().orEmpty()
        val body = req.body?.trim().orEmpty()
        if (type.isEmpty() || req.parentId == null) throw ValidationException("parentId", "A parent record is required")
        if (body.isEmpty()) throw ValidationException("body", "Note text is required")
        val n = Note(parentType = type, parentId = req.parentId, body = body)
        n.owner = access.currentUser()
        return repository.save(n)
    }

    @Transactional
    fun delete(id: Long) {
        val n = repository.findById(id).orElseThrow { NotFoundException("Note not found: $id") }
        val p = CurrentUser.require()
        val isOwnerOrAdmin = n.owner?.id == p.userId || Permissions.VIEW_ALL in p.permissions
        if (!isOwnerOrAdmin) throw NotFoundException("Note not found: $id")
        repository.deleteById(id)
    }
}

@RestController
@RequestMapping("/api/notes")
class NoteController(private val service: NoteService) {
    @GetMapping
    fun list(@RequestParam type: String, @RequestParam id: Long): List<NoteDto> =
        service.forParent(type, id).map { it.toDto() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody req: NoteRequest): NoteDto = service.create(req).toDto()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = service.delete(id)
}
