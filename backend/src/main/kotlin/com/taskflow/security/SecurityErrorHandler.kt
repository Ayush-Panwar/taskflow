package com.taskflow.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.taskflow.common.error.ApiErrorFactory
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

/**
 * Security failures happen in the servlet filters, before any controller runs, so
 * @RestControllerAdvice never sees them. This writes the same error shape for those cases.
 */
@Component
class SecurityErrorHandler(
    private val errors: ApiErrorFactory,
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint, AccessDeniedHandler {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        write(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "A valid access token is required")
    }

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AccessDeniedException,
    ) = write(request, response, HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to this resource")

    private fun write(
        request: HttpServletRequest,
        response: HttpServletResponse,
        status: HttpStatus,
        code: String,
        message: String,
    ) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        objectMapper.writeValue(response.writer, errors.create(status, code, message, request.requestURI))
    }
}
