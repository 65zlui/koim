package org.example.controller

import jakarta.validation.ConstraintViolationException
import org.example.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArg(e: IllegalArgumentException): ResponseEntity<ApiResponse<Unit>> =
        ResponseEntity.badRequest()
            .body(ApiResponse.failure(e.message ?: "bad request"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Unit>> {
        val msg = e.bindingResult.fieldErrors.joinToString(",") { "${it.field}:${it.defaultMessage}" }
        return ResponseEntity.badRequest().body(ApiResponse.failure(msg))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraint(e: ConstraintViolationException): ResponseEntity<ApiResponse<Unit>> =
        ResponseEntity.badRequest()
            .body(ApiResponse.failure(e.message ?: "validation failed"))

    @ExceptionHandler(ResponseStatusException::class)
    fun handleStatus(e: ResponseStatusException): ResponseEntity<ApiResponse<Unit>> =
        ResponseEntity.status(e.statusCode)
            .body(ApiResponse.failure(e.reason ?: e.statusCode.toString()))
}
