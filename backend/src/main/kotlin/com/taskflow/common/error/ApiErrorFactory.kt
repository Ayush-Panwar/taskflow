package com.taskflow.common.error

import org.springframework.http.HttpStatusCode
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.time.Clock

/**
 * Factory for [ApiError]. The exception handler and the security handlers both build their
 * responses here, which is what keeps the error shape consistent across the whole API.
 */
@Component
class ApiErrorFactory(private val clock: Clock) {

    fun create(
        status: HttpStatusCode,
        code: String,
        message: String,
        path: String,
        fieldErrors: List<FieldViolation> = emptyList(),
    ) = ApiError(
        timestamp = clock.instant(),
        status = status.value(),
        error = HttpStatus.resolve(status.value())?.reasonPhrase ?: "Error",
        code = code,
        message = message,
        path = path,
        fieldErrors = fieldErrors,
    )
}
