package com.taskflow.common

import com.taskflow.support.ApiTest
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get

class OpenApiDocsTest : ApiTest() {

    private val documentedPaths = listOf(
        "/api/auth/register",
        "/api/auth/login",
        "/api/projects",
        "/api/projects/{id}",
        "/api/projects/{id}/tasks",
        "/api/projects/{id}/summary",
        "/api/tasks/{id}",
        "/api/tasks/{id}/status",
        "/api/tasks/{id}/activity",
        "/api/users",
    )

    @Test
    fun `the API description is public and covers every endpoint`() {
        val body = mockMvc.get("/v3/api-docs").andExpect { status { isOk() } }.andReturn().response.contentAsString
        val docs = responseOf(body)

        documentedPaths.forEach { path ->
            assertFalse(docs["paths"][path] == null, "$path should be documented")
        }
    }

    @Test
    fun `the login endpoints need no token and everything else does`() {
        val body = mockMvc.get("/v3/api-docs").andReturn().response.contentAsString
        val paths = responseOf(body)["paths"]

        assertEquals(0, paths["/api/auth/login"]["post"]["security"].size())
        assertEquals(0, paths["/api/auth/register"]["post"]["security"].size())
        assertEquals("bearerAuth", responseOf(body)["security"][0].fieldNames().next())
    }

    @Test
    fun `the logged-in user is not shown as a parameter clients must send`() {
        val body = mockMvc.get("/v3/api-docs").andReturn().response.contentAsString
        val listTasks = responseOf(body)["paths"]["/api/projects/{id}/tasks"]["get"]

        val names = listTasks["parameters"].map { it["name"].asText() }.toSet()

        assertEquals(setOf("id", "status", "priority", "assignee", "sort", "page", "size"), names)
    }

    @Test
    fun `swagger UI is reachable without a token`() {
        mockMvc.get("/swagger-ui/index.html").andExpect {
            status { isOk() }
            content { string(org.hamcrest.Matchers.containsString("Swagger UI")) }
        }
    }
}
