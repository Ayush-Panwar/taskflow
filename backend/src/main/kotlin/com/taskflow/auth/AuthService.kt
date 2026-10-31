package com.taskflow.auth

import com.taskflow.common.error.EmailAlreadyRegisteredException
import com.taskflow.common.error.FieldValidationException
import com.taskflow.common.error.InvalidCredentialsException
import com.taskflow.common.text.trimRequired
import com.taskflow.security.TokenService
import com.taskflow.user.User
import com.taskflow.user.UserRepository
import com.taskflow.user.UserResponse
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AuthService(
    private val users: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokens: TokenService,
) {
    /** Checked when the email is unknown, so that "no such user" takes as long as "wrong password". */
    private val decoyHash: String = passwordEncoder.encode(UUID.randomUUID().toString())

    @Transactional
    fun register(request: RegisterRequest): UserResponse {
        val email = normalizeEmail(request.email!!)
        val password = request.password!!
        // @Size counts characters, but BCrypt rejects anything over 72 bytes, and some characters take several.
        if (password.toByteArray().size > BCRYPT_MAX_BYTES) {
            throw FieldValidationException("password", "Password is too long")
        }
        if (users.existsByEmail(email)) {
            throw EmailAlreadyRegisteredException()
        }
        val user = User(
            name = request.name!!.trimRequired("name", "Name"),
            email = email,
            password = passwordEncoder.encode(password),
        )
        try {
            return UserResponse.from(users.saveAndFlush(user))
        } catch (_: DataIntegrityViolationException) {
            // Two simultaneous requests can both pass the check above. The unique constraint stops the second.
            throw EmailAlreadyRegisteredException()
        }
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthResponse {
        val user = users.findByEmail(normalizeEmail(request.email!!))
        val passwordMatches = passwordEncoder.matches(request.password!!, user?.password ?: decoyHash)
        if (user == null || !passwordMatches) {
            throw InvalidCredentialsException() // The same error for both, so emails cannot be probed.
        }
        val token = tokens.issueFor(user)
        return AuthResponse(
            accessToken = token.value,
            expiresAt = token.expiresAt,
            user = UserResponse.from(user),
        )
    }

    private fun normalizeEmail(email: String) = email.trim().lowercase()

    private companion object {
        const val BCRYPT_MAX_BYTES = 72
    }
}
