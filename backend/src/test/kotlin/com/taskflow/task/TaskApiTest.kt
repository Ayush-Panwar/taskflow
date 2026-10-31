package com.taskflow.task

import com.taskflow.support.ApiTest
import com.taskflow.support.TestUser
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.Instant
import java.time.LocalDate
import org.junit.jupiter.api.assertThrows

class TaskApiTest : ApiTest() {

    @Autowired
    private lateinit var taskRepository: TaskRepository

    private fun createProject(user: TestUser, name: String = "Project"): Long {
        val response = mockMvc.post("/api/projects") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("name" to name)
        }.andExpect { status { isCreated() } }.andReturn().response
        return responseOf(response.contentAsString)["id"].asLong()
    }

    private fun createTask(user: TestUser, projectId: Long, vararg fields: Pair<String, Any?>): Long {
        val response = mockMvc.post("/api/projects/$projectId/tasks") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json(*(arrayOf<Pair<String, Any?>>("title" to "A task") + fields))
        }.andExpect { status { isCreated() } }.andReturn().response
        return responseOf(response.contentAsString)["id"].asLong()
    }

    @Test
    fun `creating a task applies the defaults`() {
        val user = newUser()
        val projectId = createProject(user)

        mockMvc.post("/api/projects/$projectId/tasks") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("title" to "  Write the report  ")
        }.andExpect {
            status { isCreated() }
            jsonPath("$.title") { value("Write the report") }
            jsonPath("$.status") { value("TODO") }
            jsonPath("$.priority") { value("MEDIUM") }
            jsonPath("$.dueDate") { value(null as Any?) }
            jsonPath("$.overdue") { value(false) }
            jsonPath("$.assignee") { value(null as Any?) }
            jsonPath("$.projectId") { value(projectId) }
            jsonPath("$.createdAt") { isNotEmpty() }
            jsonPath("$.updatedAt") { isNotEmpty() }
        }
    }

    @Test
    fun `a task can be assigned to any registered user`() {
        val owner = newUser()
        val teammate = newUser("Teammate")
        val projectId = createProject(owner)

        mockMvc.post("/api/projects/$projectId/tasks") {
            header("Authorization", bearer(owner.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("title" to "Task", "assigneeId" to teammate.id)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.assignee.id") { value(teammate.id) }
            jsonPath("$.assignee.name") { value("Teammate") }
            jsonPath("$.assignee.password") { doesNotExist() }
            jsonPath("$.assignee.email") { doesNotExist() }
        }
    }

    @Test
    fun `an unknown assignee is a field error`() {
        val user = newUser()
        val projectId = createProject(user)

        mockMvc.post("/api/projects/$projectId/tasks") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("title" to "Task", "assigneeId" to 999_999_999)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].field") { value("assigneeId") }
            jsonPath("$.fieldErrors[0].message") { value("This user does not exist") }
        }
    }

    @Test
    fun `the title is required and limited to 120 characters`() {
        val user = newUser()
        val projectId = createProject(user)

        mockMvc.post("/api/projects/$projectId/tasks") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("description" to "No title here")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
            jsonPath("$.fieldErrors[0].field") { value("title") }
            jsonPath("$.fieldErrors[0].message") { value("Title is required") }
        }

        mockMvc.post("/api/projects/$projectId/tasks") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("title" to "x".repeat(121))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].message") { value("Title must be at most 120 characters") }
        }

        mockMvc.post("/api/projects/$projectId/tasks") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("title" to "x".repeat(120))
        }.andExpect { status { isCreated() } }
    }

    @Test
    fun `the due date cannot be in the past on create, but today is allowed`() {
        val user = newUser()
        val projectId = createProject(user)

        mockMvc.post("/api/projects/$projectId/tasks") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("title" to "Late", "dueDate" to LocalDate.now().minusDays(1).toString())
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].field") { value("dueDate") }
            jsonPath("$.fieldErrors[0].message") { value("Due date cannot be in the past") }
        }

        mockMvc.post("/api/projects/$projectId/tasks") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("title" to "Today", "dueDate" to LocalDate.now().toString())
        }.andExpect {
            status { isCreated() }
            jsonPath("$.overdue") { value(false) }
        }
    }

    @Test
    fun `wrong value types are reported against the field, listing what is allowed`() {
        val user = newUser()
        val projectId = createProject(user)

        mockMvc.post("/api/projects/$projectId/tasks") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("title" to "Task", "priority" to "URGENT")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].field") { value("priority") }
            jsonPath("$.fieldErrors[0].message") { value("Must be one of: LOW, MEDIUM, HIGH") }
        }

        mockMvc.post("/api/projects/$projectId/tasks") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("title" to "Task", "dueDate" to "tomorrow")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].field") { value("dueDate") }
            jsonPath("$.fieldErrors[0].message") { value(containsString("yyyy-MM-dd")) }
        }
    }

    @Test
    fun `PUT replaces the editable fields and advances updatedAt`() {
        val user = newUser()
        val teammate = newUser("Teammate")
        val projectId = createProject(user)
        val taskId = createTask(user, projectId, "description" to "Old", "assigneeId" to teammate.id)
        val before = taskRepository.findById(taskId).orElseThrow().updatedAt
        val newDate = LocalDate.now().plusDays(3).toString()

        val response = mockMvc.put("/api/tasks/$taskId") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("title" to "New title", "priority" to "HIGH", "dueDate" to newDate, "status" to "IN_PROGRESS")
        }.andExpect {
            status { isOk() }
            jsonPath("$.title") { value("New title") }
            jsonPath("$.description") { value(null as Any?) }
            jsonPath("$.priority") { value("HIGH") }
            jsonPath("$.status") { value("IN_PROGRESS") }
            jsonPath("$.dueDate") { value(newDate) }
            jsonPath("$.assignee") { value(null as Any?) }
        }.andReturn().response

        val updatedAt = Instant.parse(responseOf(response.contentAsString)["updatedAt"].asText())
        assertTrue(updatedAt.isAfter(before), "updatedAt should move forward: before=$before after=$updatedAt")
    }

    @Test
    fun `PUT keeps the status and priority when they are omitted`() {
        val user = newUser()
        val projectId = createProject(user)
        val taskId = createTask(user, projectId, "priority" to "HIGH", "status" to "IN_PROGRESS")

        mockMvc.put("/api/tasks/$taskId") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("title" to "Renamed")
        }.andExpect {
            status { isOk() }
            jsonPath("$.priority") { value("HIGH") }
            jsonPath("$.status") { value("IN_PROGRESS") }
        }
    }

    @Test
    fun `an overdue task can still be edited without changing its date`() {
        val user = newUser()
        val projectId = createProject(user)
        val taskId = createTask(user, projectId)
        val yesterday = LocalDate.now().minusDays(1)
        taskRepository.save(taskRepository.findById(taskId).orElseThrow().apply { dueDate = yesterday })

        mockMvc.put("/api/tasks/$taskId") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("title" to "Still late", "dueDate" to yesterday.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.title") { value("Still late") }
            jsonPath("$.overdue") { value(true) }
        }
    }

    @Test
    fun `finishing an overdue task clears the overdue flag`() {
        val user = newUser()
        val projectId = createProject(user)
        val taskId = createTask(user, projectId)
        taskRepository.save(taskRepository.findById(taskId).orElseThrow().apply { dueDate = LocalDate.now().minusDays(1) })

        mockMvc.patch("/api/tasks/$taskId/status") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("status" to "DONE")
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("DONE") }
            jsonPath("$.overdue") { value(false) }
        }
    }

    @Test
    fun `PATCH status changes only the status and requires a valid one`() {
        val user = newUser()
        val projectId = createProject(user)
        val taskId = createTask(user, projectId, "title" to "Keep me", "priority" to "LOW")

        mockMvc.patch("/api/tasks/$taskId/status") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("status" to "IN_PROGRESS")
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("IN_PROGRESS") }
            jsonPath("$.title") { value("Keep me") }
            jsonPath("$.priority") { value("LOW") }
        }

        mockMvc.patch("/api/tasks/$taskId/status") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].field") { value("status") }
            jsonPath("$.fieldErrors[0].message") { value("Status is required") }
        }

        mockMvc.patch("/api/tasks/$taskId/status") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("status" to "WAITING")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].message") { value("Must be one of: TODO, IN_PROGRESS, DONE") }
        }
    }

    @Test
    fun `saving the same values does not advance updatedAt`() {
        val user = newUser()
        val projectId = createProject(user)
        val taskId = createTask(user, projectId, "priority" to "HIGH")
        val before = taskRepository.findById(taskId).orElseThrow().updatedAt

        mockMvc.patch("/api/tasks/$taskId/status") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("status" to "TODO")
        }.andExpect { status { isOk() } }

        assertEquals(before, taskRepository.findById(taskId).orElseThrow().updatedAt)
    }

    @Test
    fun `DELETE removes the task`() {
        val user = newUser()
        val projectId = createProject(user)
        val taskId = createTask(user, projectId)

        mockMvc.delete("/api/tasks/$taskId") { header("Authorization", bearer(user.token)) }
            .andExpect { status { isNoContent() } }
        mockMvc.delete("/api/tasks/$taskId") { header("Authorization", bearer(user.token)) }
            .andExpect {
                status { isNotFound() }
                jsonPath("$.message") { value("Task not found") }
            }
    }

    @Test
    fun `nobody can create, change or delete tasks in a project they do not own`() {
        val owner = newUser()
        val other = newUser()
        val projectId = createProject(owner)
        val taskId = createTask(owner, projectId)

        mockMvc.post("/api/projects/$projectId/tasks") {
            header("Authorization", bearer(other.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("title" to "Intruder")
        }.andExpect { status { isNotFound() } }

        mockMvc.put("/api/tasks/$taskId") {
            header("Authorization", bearer(other.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("title" to "Hacked")
        }.andExpect { status { isNotFound() } }

        mockMvc.patch("/api/tasks/$taskId/status") {
            header("Authorization", bearer(other.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("status" to "DONE")
        }.andExpect { status { isNotFound() } }

        mockMvc.delete("/api/tasks/$taskId") { header("Authorization", bearer(other.token)) }
            .andExpect { status { isNotFound() } }

        val untouched = taskRepository.findById(taskId).orElseThrow()
        assertEquals("A task", untouched.title)
        assertEquals(TaskStatus.TODO, untouched.status)
    }

    @Test
    fun `being assigned a task gives no access to it`() {
        val owner = newUser()
        val teammate = newUser()
        val projectId = createProject(owner)
        val taskId = createTask(owner, projectId, "assigneeId" to teammate.id)

        mockMvc.patch("/api/tasks/$taskId/status") {
            header("Authorization", bearer(teammate.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("status" to "DONE")
        }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `a stale copy of a task cannot overwrite a newer version`() {
        val user = newUser()
        val projectId = createProject(user)
        val taskId = createTask(user, projectId)
        val staleCopy = taskRepository.findById(taskId).orElseThrow()

        mockMvc.patch("/api/tasks/$taskId/status") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("status" to "DONE")
        }.andExpect { status { isOk() } }

        staleCopy.title = "Overwrite attempt"
        assertThrows<OptimisticLockingFailureException> { taskRepository.save(staleCopy) }
    }
}
