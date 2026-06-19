package com.pangreksa.crm.base

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** Thrown by the service layer when a business/data validation rule fails. */
class ValidationException(val field: String?, override val message: String) : RuntimeException(message)

/** Thrown when a record is not found. */
class NotFoundException(override val message: String) : RuntimeException(message)

data class FieldError(val field: String?, val message: String)
data class ApiError(val message: String, val errors: List<FieldError> = emptyList())

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(ValidationException::class)
    fun handleValidation(ex: ValidationException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError(ex.message, listOf(FieldError(ex.field, ex.message))))

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError(ex.message))
}
