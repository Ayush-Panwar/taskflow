package com.taskflow.auth

import com.taskflow.common.error.EmailAlreadyRegisteredException
import com.taskflow.common.error.FieldValidationException
import com.taskflow.common.error.InvalidCredentialsException
import com.taskflow.security.IssuedToken
import com.taskflow.security.TokenService
import com.taskflow.user.User
import com.taskflow.user.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant

class AuthServiceTest {

    private val users = mockk<UserRepository>()
    private val tokens = mockk<TokenService>()
    private val passwordEncoder = mockk<PasswordEncoder> {
        every { encode(any()) } returns "hashed"
    }
    private val service = AuthService(users, passwordEncoder, tokens)

    @Test
    fun `register stores a hash and a normalised email`() {
        val saved = slot<User>()
        every { users.existsByEmail("asha@example.com") } returns false
        every { users.saveAndFlush(capture(saved)) } answers { saved.captured }

        val response = service.register(
            RegisterRequest(name = "  Asha ", email = " Asha@Example.COM ", password = "plain-password"),
        )

        assertEquals("hashed", saved.captured.password)
        assertEquals("asha@example.com", saved.captured.email)
        assertEquals("Asha", saved.captured.name)
        assertEquals("asha@example.com", response.email)
    }

    @Test
    fun `register rejects an email that already exists`() {
        every { users.existsByEmail("asha@example.com") } returns true

        assertThrows<EmailAlreadyRegisteredException> {
            service.register(RegisterRequest("Asha", "asha@example.com", "plain-password"))
        }

        verify(exactly = 0) { users.saveAndFlush(any<User>()) }
    }

    @Test
    fun `register reports a duplicate caught only by the database constraint`() {
        every { users.existsByEmail("asha@example.com") } returns false
        every { users.saveAndFlush(any<User>()) } throws DataIntegrityViolationException("uq_users_email")

        assertThrows<EmailAlreadyRegisteredException> {
            service.register(RegisterRequest("Asha", "asha@example.com", "plain-password"))
        }
    }

    @Test
    fun `register rejects a password that exceeds the BCrypt byte limit`() {
        val exception = assertThrows<FieldValidationException> {
            service.register(RegisterRequest("Asha", "asha@example.com", "é".repeat(40)))
        }

        assertEquals("password", exception.fieldErrors.single().field)
    }

    @Test
    fun `login issues a token when the password matches`() {
        val user = User("Asha", "asha@example.com", "stored-hash").apply { id = 7 }
        every { users.findByEmail("asha@example.com") } returns user
        every { passwordEncoder.matches("plain-password", "stored-hash") } returns true
        every { tokens.issueFor(user) } returns IssuedToken("signed-token", Instant.EPOCH)

        val response = service.login(LoginRequest("Asha@Example.com", "plain-password"))

        assertEquals("signed-token", response.accessToken)
        assertEquals(7, response.user.id)
    }

    @Test
    fun `login rejects a wrong password`() {
        val user = User("Asha", "asha@example.com", "stored-hash")
        every { users.findByEmail("asha@example.com") } returns user
        every { passwordEncoder.matches(any(), "stored-hash") } returns false

        assertThrows<InvalidCredentialsException> {
            service.login(LoginRequest("asha@example.com", "wrong"))
        }
    }

    @Test
    fun `login for an unknown email still runs a password check, so timing reveals nothing`() {
        every { users.findByEmail(any()) } returns null
        every { passwordEncoder.matches(any(), any()) } returns false

        assertThrows<InvalidCredentialsException> {
            service.login(LoginRequest("nobody@example.com", "anything"))
        }

        verify(exactly = 1) { passwordEncoder.matches("anything", "hashed") }
    }
}
