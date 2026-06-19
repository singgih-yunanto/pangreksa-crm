package com.pangreksa.crm.base

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

/** A page of results plus the total count (for infinite-scroll X-Total-Count). */
data class Page<T>(val items: List<T>, val total: Long)

/**
 * Build a Spring [PageRequest] from offset/limit/sort. The frontend's infinite query always sends
 * offset as a multiple of limit, so page = offset / limit.
 * [sort] format: "field,asc" | "field,desc"; [allowed] restricts sortable fields; falls back to [default].
 */
fun pageRequest(offset: Int, limit: Int, sort: String?, allowed: Set<String>, default: String): PageRequest {
    val safeLimit = limit.coerceIn(1, 200)
    val page = (offset.coerceAtLeast(0)) / safeLimit
    val parts = sort?.split(",")?.map { it.trim() } ?: emptyList()
    val field = parts.getOrNull(0)?.takeIf { it in allowed } ?: default
    val dir = if (parts.getOrNull(1).equals("desc", ignoreCase = true)) Sort.Direction.DESC else Sort.Direction.ASC
    return PageRequest.of(page, safeLimit, Sort.by(dir, field))
}
