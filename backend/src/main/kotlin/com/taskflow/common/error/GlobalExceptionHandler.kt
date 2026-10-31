package com.taskflow.common.error

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.TypeMismatchException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.LocalDate

/**
 * Turns every exception raised while handling a request into an [ApiError].
 *
 * Extending [ResponseEntityExceptionHandler] brings in Spring MVC's own errors (wrong HTTP method,
 * unsupported media type, unknown URL). Failures in the security filters happen before this class is
 * reached, so they are covered separately by [com.taskflow.security.SecurityErrorHandler].
 */
@RestControllerAdvice
class GlobalExceptionHandler(private val errors: ApiErrorFactory) : ResponseEntityExceptionHandler() {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(ex: ApiException, request: HttpServletRequest): ResponseEntity<ApiError> =
        ResponseEntity.status(ex.status).body(
            errors.create(ex.status, ex.code, ex.message.orEmpty(), request.requestURI, ex.fieldErrors),
        )

    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handleConcurrentUpdate(request: HttpServletRequest): ResponseEntity<ApiError> =
        conflict("CONCURRENT_UPDATE", "This task was changed by someone else. Reload it and try again.", request)

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleConstraintViolation(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        log.warn("Database constraint violated on {}: {}", request.requestURI, ex.mostSpecificCause.message)
        return conflict("DATA_CONFLICT", "The request conflicts with existing data", request)
    }

    /** Last resort. Logs the details and tells the client nothing about the internals. */
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.error("Unexpected error on {} {}", request.method, request.requestURI, ex)
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        return ResponseEntity.status(status).body(
            errors.create(status, "INTERNAL_ERROR", "Something went wrong on our side", request.requestURI),
        )
    }

    /** Bean Validation failures on request bodies and query objects, plus query values of the wrong type. */
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> =
        validationFailure(ex.bindingResult.fieldErrors.map(::toViolation), request)

    /** Malformed JSON, or a JSON value of the wrong type such as an unknown enum or a bad date. */
    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val cause = ex.cause
        if (cause is MismatchedInputException && cause.path.isNotEmpty()) {
            val field = cause.path.joinToString(".") { it.fieldName ?: "[${it.index}]" }
            return validationFailure(listOf(FieldViolation(field, describeExpected(cause.targetType))), request)
        }
        val badRequest = HttpStatus.BAD_REQUEST
        return respond(errors.create(badRequest, "MALFORMED_REQUEST", "Malformed JSON request body", pathOf(request)))
    }

    /** A path variable of the wrong type, such as /api/projects/abc. */
    override fun handleTypeMismatch(
        ex: TypeMismatchException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val field = (ex as? MethodArgumentTypeMismatchException)?.name ?: ex.propertyName ?: "request"
        return validationFailure(listOf(FieldViolation(field, describeExpected(ex.requiredType))), request)
    }

    /** Every remaining Spring MVC error goes through here, so it gets the same shape too. */
    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val error = errors.create(statusCode, "REQUEST_REJECTED", messageFor(statusCode), pathOf(request))
        return ResponseEntity.status(statusCode).headers(headers).body(error)
    }

    private fun toViolation(error: FieldError): FieldViolation {
        val message = if (error.isBindingFailure) {
            describeExpected(error.unwrap(TypeMismatchException::class.java).requiredType)
        } else {
            error.defaultMessage ?: "Is invalid"
        }
        return FieldViolation(error.field, message)
    }

    private fun describeExpected(type: Class<*>?): String = when {
        type == null -> "Has an invalid value"
        type.isEnum -> "Must be one of: ${type.enumConstants.joinToString(", ")}"
        type == LocalDate::class.java -> "Must be a valid date in yyyy-MM-dd format"
        Number::class.java.isAssignableFrom(type) || type.isPrimitive -> "Must be a number"
        else -> "Has an invalid value"
    }

    private fun messageFor(status: HttpStatusCode): String = when (status.value()) {
        404 -> "The requested resource was not found"
        405 -> "This HTTP method is not supported here"
        406 -> "This response format is not supported"
        415 -> "This content type is not supported. Use application/json."
        else -> HttpStatus.resolve(status.value())?.reasonPhrase ?: "The request could not be processed"
    }

    private fun validationFailure(violations: List<FieldViolation>, request: WebRequest): ResponseEntity<Any> {
        val status = HttpStatus.BAD_REQUEST
        return respond(errors.create(status, "VALIDATION_FAILED", "Request validation failed", pathOf(request), violations))
    }

    private fun conflict(code: String, message: String, request: HttpServletRequest): ResponseEntity<ApiError> {
        val status = HttpStatus.CONFLICT
        return ResponseEntity.status(status).body(errors.create(status, code, message, request.requestURI))
    }

    private fun respond(error: ApiError): ResponseEntity<Any> =
        ResponseEntity.status(error.status).body(error)

    private fun pathOf(request: WebRequest): String =
        (request as ServletWebRequest).request.requestURI
}
