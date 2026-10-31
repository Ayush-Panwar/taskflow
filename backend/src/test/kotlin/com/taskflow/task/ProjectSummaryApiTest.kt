package com.taskflow.task

import com.taskflow.support.ApiTest
import com.taskflow.support.TestUser
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDate

class ProjectSummaryApiTest : ApiTest() {

    @Autowired
    private lateinit var taskRepository: TaskRepository

    private fun createProject(user: TestUser): Long {
        val response = mockMvc.post("/api/projects") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("name" to "Project")
        }.andExpect { status { isCreated() } }.andReturn().response
        return responseOf(response.contentAsString)["id"].asLong()
    }

    /** Past due dates cannot go through the API, so they are written directly to the database. */
    private fun addTask(user: TestUser, projectId: Long, status: TaskStatus, dueDate: LocalDate? = null) {
        val response = mockMvc.post("/api/projects/$projectId/tasks") {
            header("Authorization", bearer(user.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("title" to "Task", "status" to status.name)
        }.andExpect { status { isCreated() } }.andReturn().response
        val taskId = responseOf(response.contentAsString)["id"].asLong()
        taskRepository.save(taskRepository.findById(taskId).orElseThrow().apply { this.dueDate = dueDate })
    }

    @Test
    fun `an empty project reports zero for every status`() {
        val user = newUser()
        val projectId = createProject(user)

        mockMvc.get("/api/projects/$projectId/summary") { header("Authorization", bearer(user.token)) }.andExpect {
            status { isOk() }
            jsonPath("$.projectId") { value(projectId) }
            jsonPath("$.counts.TODO") { value(0) }
            jsonPath("$.counts.IN_PROGRESS") { value(0) }
            jsonPath("$.counts.DONE") { value(0) }
            jsonPath("$.total") { value(0) }
            jsonPath("$.overdue") { value(0) }
        }
    }

    @Test
    fun `counts tasks per status and only unfinished past-due tasks as overdue`() {
        val user = newUser()
        val projectId = createProject(user)
        val today = LocalDate.now()
        addTask(user, projectId, TaskStatus.TODO, today.minusDays(1)) // overdue
        addTask(user, projectId, TaskStatus.TODO, today) // due today, not overdue yet
        addTask(user, projectId, TaskStatus.TODO) // no date
        addTask(user, projectId, TaskStatus.IN_PROGRESS, today.minusDays(5)) // overdue
        addTask(user, projectId, TaskStatus.DONE, today.minusDays(5)) // finished late, not overdue

        mockMvc.get("/api/projects/$projectId/summary") { header("Authorization", bearer(user.token)) }.andExpect {
            status { isOk() }
            jsonPath("$.counts.TODO") { value(3) }
            jsonPath("$.counts.IN_PROGRESS") { value(1) }
            jsonPath("$.counts.DONE") { value(1) }
            jsonPath("$.total") { value(5) }
            jsonPath("$.overdue") { value(2) }
        }
    }

    @Test
    fun `only counts tasks from the requested project`() {
        val user = newUser()
        val projectA = createProject(user)
        val projectB = createProject(user)
        addTask(user, projectA, TaskStatus.TODO)
        addTask(user, projectB, TaskStatus.TODO)
        addTask(user, projectB, TaskStatus.TODO)

        mockMvc.get("/api/projects/$projectA/summary") { header("Authorization", bearer(user.token)) }.andExpect {
            jsonPath("$.total") { value(1) }
        }
    }

    @Test
    fun `nobody can read the summary of a project they do not own`() {
        val projectId = createProject(newUser())

        mockMvc.get("/api/projects/$projectId/summary") { header("Authorization", bearer(newUser().token)) }
            .andExpect { status { isNotFound() } }
    }
}
