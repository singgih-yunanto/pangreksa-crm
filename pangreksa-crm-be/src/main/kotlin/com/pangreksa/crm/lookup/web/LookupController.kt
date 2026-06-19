package com.pangreksa.crm.lookup.web

import com.pangreksa.crm.base.NotFoundException
import com.pangreksa.crm.base.ValidationException
import com.pangreksa.crm.lookup.domain.Lookup
import com.pangreksa.crm.lookup.domain.LookupRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class LookupDto(
    val id: Long, val category: String, val code: String, val label: String,
    val sortOrder: Int, val extra: Map<String, Any?>,
)

fun Lookup.toDto() = LookupDto(id!!, category, code, label, sortOrder, extra)

/** Resolves lookup FKs for the module services. */
@Service
class LookupService(private val repository: LookupRepository) {
    fun byCategory(category: String): List<Lookup> = repository.findByCategoryAndActiveTrueOrderBySortOrderAsc(category)

    fun resolve(category: String, code: String?): Lookup? {
        if (code.isNullOrBlank()) return null
        return repository.findByCategoryAndCode(category, code)
            ?: throw ValidationException(null, "Unknown $category: $code")
    }

    fun resolveById(category: String, id: Long?): Lookup? {
        if (id == null) return null
        val l = repository.findById(id).orElseThrow { ValidationException(null, "Lookup not found: $id") }
        if (l.category != category) throw ValidationException(null, "Lookup $id is not a $category")
        return l
    }
}

@RestController
@RequestMapping("/api/lookups")
class LookupController(private val repository: LookupRepository) {
    @GetMapping
    fun byCategory(@RequestParam category: String): List<LookupDto> =
        repository.findByCategoryAndActiveTrueOrderBySortOrderAsc(category).map { it.toDto() }
}
