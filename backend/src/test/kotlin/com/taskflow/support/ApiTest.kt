package com.taskflow.support

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.UUID

data class TestUser(val id: Long, val email: String, val password: String, val token: String)

/**
 * Base class for integration tests. These run the whole application: security filters, controllers,
 * services and a real database. Tests share one database, so each one creates its own uniquely named
 * users rather than relying on a clean slate.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ApiTest {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    protected fun uniqueEmail() = "user-${UUID.randomUUID()}@example.com"

    protected fun json(vararg fields: Pair<String, Any?>): String = objectMapper.writeValueAsString(mapOf(*fields))

    protected fun bearer(token: String) = "Bearer $token"

    protected fun responseOf(body: String): JsonNode = objectMapper.readTree(body)

    /** Registers a new user through the API and logs in, exactly as a real client would. */
    protected fun newUser(name: String = "Test User"): TestUser {
        val email = uniqueEmail()
        val password = "correct-horse-battery"
        val registered = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = json("name" to name, "email" to email, "password" to password)
        }.andReturn().response
        check(registered.status == 201) { "Registration failed: ${registered.contentAsString}" }

        val loggedIn = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = json("email" to email, "password" to password)
        }.andReturn().response
        check(loggedIn.status == 200) { "Login failed: ${loggedIn.contentAsString}" }

        val body = responseOf(loggedIn.contentAsString)
        return TestUser(body["user"]["id"].asLong(), email, password, body["accessToken"].asText())
    }
}
