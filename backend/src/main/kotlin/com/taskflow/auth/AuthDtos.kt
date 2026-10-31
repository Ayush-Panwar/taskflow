package com.taskflow.auth

import com.taskflow.user.UserResponse
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

/*
 * Request fields are nullable on purpose. With a non-null Kotlin type, a missing field would fail
 * during JSON parsing, before validation runs, and the client would get "malformed JSON" instead of
 * "name is required". The @field: prefix puts each annotation on the field, where the validator looks.
 */

data class RegisterRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 100, message = "Name must be at most 100 characters")
    val name: String? = null,

    @field:NotBlank(message = "Email is required")
    @field:Email(regexp = ".+@.+\\..+", message = "Email must be a valid email address")
    @field:Size(max = 254, message = "Email must be at most 254 characters")
    val email: String? = null,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    val password: String? = null,
)

data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    val email: String? = null,

    @field:NotBlank(message = "Password is required")
    val password: String? = null,
)

data class AuthResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresAt: Instant,
    val user: UserResponse,
)
