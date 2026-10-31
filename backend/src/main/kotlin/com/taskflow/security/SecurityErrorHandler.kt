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
import org.springframework.security.web.firewall.RequestRejectedException
import org.springframework.security.web.firewall.RequestRejectedHandler
import org.springframework.stereotype.Component

/**
 * Security failures happen in the servlet filters, before any controller runs, so
 * @RestControllerAdvice never sees them. This writes the same error shape for those cases.
 */
@Component
class SecurityErrorHandler(
    private val errors: ApiErrorFactory,
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint, AccessDeniedHandler, RequestRejectedHandler {

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

    /** Spring Security's firewall refuses suspicious URLs, such as those containing // or ; */
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: RequestRejectedException,
    ) = write(request, response, HttpStatus.BAD_REQUEST, "REQUEST_REJECTED", "This URL is not allowed")

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
