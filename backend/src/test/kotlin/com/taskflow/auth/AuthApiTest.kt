package com.taskflow.auth

import com.taskflow.support.ApiTest
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post

class AuthApiTest : ApiTest() {

    @Test
    fun `register creates a user and never returns the password`() {
        val email = uniqueEmail()

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = json("name" to "Asha", "email" to email, "password" to "correct-horse-battery")
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { isNumber() }
            jsonPath("$.name") { value("Asha") }
            jsonPath("$.email") { value(email) }
            jsonPath("$.password") { doesNotExist() }
            content { string(not(containsString("correct-horse-battery"))) }
        }
    }

    @Test
    fun `register reports every invalid field with its own message`() {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = json("email" to "not-an-email", "password" to "short")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
            jsonPath("$.path") { value("/api/auth/register") }
            jsonPath("$.fieldErrors[*].field") { value(hasItems("name", "email", "password")) }
            jsonPath("$.fieldErrors[?(@.field == 'name')].message") { value("Name is required") }
            jsonPath("$.fieldErrors[?(@.field == 'email')].message") { value("Email must be a valid email address") }
            jsonPath("$.fieldErrors[?(@.field == 'password')].message") { value("Password must be between 8 and 72 characters") }
        }
    }

    @Test
    fun `register rejects a duplicate email regardless of case`() {
        val email = uniqueEmail()
        val request = json("name" to "Asha", "email" to email, "password" to "correct-horse-battery")
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = request
        }.andExpect { status { isCreated() } }

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = json("name" to "Other", "email" to email.uppercase(), "password" to "correct-horse-battery")
        }.andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("EMAIL_ALREADY_REGISTERED") }
            jsonPath("$.fieldErrors[0].field") { value("email") }
        }
    }

    @Test
    fun `register rejects a password over 72 bytes even when it is under 72 characters`() {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = json("name" to "Asha", "email" to uniqueEmail(), "password" to "é".repeat(40))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].field") { value("password") }
        }
    }

    @Test
    fun `login returns a bearer token and the user`() {
        val user = newUser("Asha")

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = json("email" to user.email.uppercase(), "password" to user.password)
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { isNotEmpty() }
            jsonPath("$.tokenType") { value("Bearer") }
            jsonPath("$.expiresAt") { isNotEmpty() }
            jsonPath("$.user.id") { value(user.id) }
            jsonPath("$.user.name") { value("Asha") }
        }
    }

    @Test
    fun `login gives the same answer for a wrong password and an unknown email`() {
        val user = newUser()

        listOf(
            json("email" to user.email, "password" to "wrong-password"),
            json("email" to uniqueEmail(), "password" to "wrong-password"),
        ).forEach { request ->
            mockMvc.post("/api/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = request
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("INVALID_CREDENTIALS") }
                jsonPath("$.message") { value("Invalid email or password") }
            }
        }
    }

    @Test
    fun `login with a very long password is rejected normally rather than crashing`() {
        val user = newUser()

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = json("email" to user.email, "password" to "x".repeat(10_000))
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `login still works when an expired or invalid token is sent along`() {
        val user = newUser()

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer not-a-real-token")
            content = json("email" to user.email, "password" to user.password)
        }.andExpect { status { isOk() } }
    }
}
