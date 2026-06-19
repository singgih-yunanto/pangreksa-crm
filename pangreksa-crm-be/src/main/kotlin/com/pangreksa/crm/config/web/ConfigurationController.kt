package com.pangreksa.crm.config.web

import com.pangreksa.crm.config.domain.ConfigurationRepository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Service
class ConfigurationService(private val repository: ConfigurationRepository) {
    fun string(key: String, default: String): String = repository.findByKey(key)?.value ?: default
    fun int(key: String, default: Int): Int = repository.findByKey(key)?.value?.toIntOrNull() ?: default
}

@RestController
@RequestMapping("/api/configuration")
class ConfigurationController(private val service: ConfigurationService) {
    /** Effective settings consumed by the frontend (e.g. money formatting). Read-only. */
    @GetMapping
    fun get(): Map<String, Any> = mapOf(
        "currency" to service.string("currency", "IDR"),
        "decimalPlaces" to service.int("decimal_places", 0),
        "locale" to service.string("locale", "id-ID"),
    )
}
