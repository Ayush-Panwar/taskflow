package com.taskflow.task.activity

import com.taskflow.support.ApiTest
import com.taskflow.support.TestUser
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

class TaskActivityApiTest : ApiTest() {

    private fun createTask(user: TestUser): Long {
        val project = mockMvc.post("/api/projects") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("name" to "Project")
        }.andExpect { status { isCreated() } }.andReturn().response
        val projectId = responseOf(project.contentAsString)["id"].asLong()

        val task = mockMvc.post("/api/projects/$projectId/tasks") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("title" to "Task")
        }.andExpect { status { isCreated() } }.andReturn().response
        return responseOf(task.contentAsString)["id"].asLong()
    }

    private fun changeStatus(user: TestUser, taskId: Long, status: String) {
        mockMvc.patch("/api/tasks/$taskId/status") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("status" to status)
        }.andExpect { status { isOk() } }
    }

    private fun readActivity(user: TestUser, taskId: Long): ResultActionsDsl =
        mockMvc.get("/api/tasks/$taskId/activity") { header("Authorization", bearer(user.token)) }

    @Test
    fun `a new task has no history`() {
        val user = newUser()
        val taskId = createTask(user)

        readActivity(user, taskId).andExpect {
            status { isOk() }
            jsonPath("$") { isEmpty() }
        }
    }

    @Test
    fun `a status change records who made it, what changed and when`() {
        val user = newUser("Asha")
        val taskId = createTask(user)

        changeStatus(user, taskId, "IN_PROGRESS")

        readActivity(user, taskId).andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].actor.id") { value(user.id) }
            jsonPath("$[0].actor.name") { value("Asha") }
            jsonPath("$[0].fromStatus") { value("TODO") }
            jsonPath("$[0].toStatus") { value("IN_PROGRESS") }
            jsonPath("$[0].occurredAt") { isNotEmpty() }
        }
    }

    @Test
    fun `history lists the newest change first`() {
        val user = newUser()
        val taskId = createTask(user)

        changeStatus(user, taskId, "IN_PROGRESS")
        changeStatus(user, taskId, "DONE")
        changeStatus(user, taskId, "TODO")

        readActivity(user, taskId).andExpect {
            jsonPath("$[*].toStatus") { value(contains("TODO", "DONE", "IN_PROGRESS")) }
        }
    }

    @Test
    fun `a status change made with PUT is recorded, and a save with no change is not`() {
        val user = newUser()
        val taskId = createTask(user)

        mockMvc.put("/api/tasks/$taskId") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("title" to "Task", "status" to "DONE")
        }.andExpect { status { isOk() } }
        mockMvc.put("/api/tasks/$taskId") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("title" to "Renamed", "status" to "DONE")
        }.andExpect { status { isOk() } }
        changeStatus(user, taskId, "DONE")

        readActivity(user, taskId).andExpect {
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].fromStatus") { value("TODO") }
            jsonPath("$[0].toStatus") { value("DONE") }
        }
    }

    @Test
    fun `deleting a task also removes its history`() {
        val user = newUser()
        val taskId = createTask(user)
        changeStatus(user, taskId, "DONE")

        mockMvc.delete("/api/tasks/$taskId") { header("Authorization", bearer(user.token)) }
            .andExpect { status { isNoContent() } }

        readActivity(user, taskId).andExpect { status { isNotFound() } }
    }

    @Test
    fun `nobody can read the history of a task in a project they do not own`() {
        val taskId = createTask(newUser())

        readActivity(newUser(), taskId).andExpect { status { isNotFound() } }
    }
}
