package com.taskflow.project

import com.taskflow.support.ApiTest
import com.taskflow.support.TestUser
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class ProjectApiTest : ApiTest() {

    private fun createProject(user: TestUser, name: String): Long {
        val response = mockMvc.post("/api/projects") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("name" to name, "description" to "About $name")
        }.andExpect { status { isCreated() } }.andReturn().response
        return responseOf(response.contentAsString)["id"].asLong()
    }

    @Test
    fun `a user can create a project and see it, newest first`() {
        val user = newUser()
        createProject(user, "First")
        createProject(user, "Second")

        mockMvc.get("/api/projects") { header("Authorization", bearer(user.token)) }.andExpect {
            status { isOk() }
            jsonPath("$[*].name") { value(contains("Second", "First")) }
            jsonPath("$[0].description") { value("About Second") }
            jsonPath("$[0].createdAt") { isNotEmpty() }
        }
    }

    @Test
    fun `a user never sees another user's projects`() {
        val owner = newUser()
        val other = newUser()
        val projectId = createProject(owner, "Private")

        mockMvc.get("/api/projects") { header("Authorization", bearer(other.token)) }.andExpect {
            status { isOk() }
            jsonPath("$") { isEmpty() }
        }
        mockMvc.get("/api/projects/$projectId") { header("Authorization", bearer(other.token)) }.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("NOT_FOUND") }
            jsonPath("$.message") { value("Project not found") }
        }
    }

    @Test
    fun `a project id that does not exist and one owned by someone else look identical`() {
        val owner = newUser()
        val other = newUser()
        val projectId = createProject(owner, "Private")

        val hidden = mockMvc.get("/api/projects/$projectId") { header("Authorization", bearer(other.token)) }
            .andReturn().response
        val missing = mockMvc.get("/api/projects/999999999") { header("Authorization", bearer(other.token)) }
            .andReturn().response

        check(hidden.status == missing.status)
        check(responseOf(hidden.contentAsString)["message"] == responseOf(missing.contentAsString)["message"])
    }

    @Test
    fun `a project needs a name of a sensible length`() {
        val user = newUser()

        mockMvc.post("/api/projects") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("name" to "   ")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].field") { value("name") }
            jsonPath("$.fieldErrors[0].message") { value("Name is required") }
        }

        mockMvc.post("/api/projects") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("name" to "x".repeat(101))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].message") { value("Name must be at most 100 characters") }
        }
    }

    @Test
    fun `malformed input gets a clear 400 rather than a server error`() {
        val user = newUser()

        mockMvc.post("/api/projects") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = "{ this is not json"
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("MALFORMED_REQUEST") }
        }

        mockMvc.get("/api/projects/not-a-number") { header("Authorization", bearer(user.token)) }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].field") { value("id") }
            jsonPath("$.fieldErrors[0].message") { value("Must be a number") }
        }
    }
}
