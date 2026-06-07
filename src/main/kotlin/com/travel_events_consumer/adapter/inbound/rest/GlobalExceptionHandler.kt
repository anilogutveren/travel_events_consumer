package com.travel_events_consumer.adapter.inbound.rest

import com.travel_events_consumer.domain.model.AuditLogNotFoundException
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(AuditLogNotFoundException::class)
    fun handleNotFound(ex: AuditLogNotFoundException): ProblemDetail =
        ProblemDetail.forStatus(HttpStatus.NOT_FOUND).also {
            it.type = URI.create("https://travel-events-consumer/errors/not-found")
            it.title = "Resource Not Found"
            it.detail = ex.message
        }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ProblemDetail =
        ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).also {
            it.type = URI.create("https://travel-events-consumer/errors/validation")
            it.title = "Validation Failed"
            it.detail = ex.bindingResult.fieldErrors.joinToString("; ") { e ->
                "${e.field}: ${e.defaultMessage}"
            }
        }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException): ProblemDetail =
        ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).also {
            it.type = URI.create("https://travel-events-consumer/errors/validation")
            it.title = "Constraint Violation"
            it.detail = ex.constraintViolations.joinToString("; ") { v ->
                "${v.propertyPath}: ${v.message}"
            }
        }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ProblemDetail =
        ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).also {
            it.type = URI.create("https://travel-events-consumer/errors/internal")
            it.title = "Internal Server Error"
            it.detail = "An unexpected error occurred. Please contact support."
        }
}
