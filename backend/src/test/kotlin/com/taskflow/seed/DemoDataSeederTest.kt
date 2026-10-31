package com.taskflow.seed

import com.taskflow.project.ProjectRepository
import com.taskflow.support.ApiTest
import com.taskflow.task.TaskRepository
import com.taskflow.task.activity.TaskActivityRepository
import com.taskflow.user.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Clock

class DemoDataSeederTest : ApiTest() {

    @Autowired private lateinit var users: UserRepository
    @Autowired private lateinit var projects: ProjectRepository
    @Autowired private lateinit var tasks: TaskRepository
    @Autowired private lateinit var activity: TaskActivityRepository
    @Autowired private lateinit var passwordEncoder: PasswordEncoder
    @Autowired private lateinit var clock: Clock

    @Test
    fun `seeding creates a usable demo account and is safe to run twice`() {
        val seeder = DemoDataSeeder(users, projects, tasks, activity, passwordEncoder, clock, "demo12345")

        seeder.run(DefaultApplicationArguments())
        val usersAfterFirstRun = users.count()
        val tasksAfterFirstRun = tasks.count()
        seeder.run(DefaultApplicationArguments())

        assertEquals(usersAfterFirstRun, users.count())
        assertEquals(tasksAfterFirstRun, tasks.count())

        val login = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = json("email" to "demo@taskflow.dev", "password" to "demo12345")
        }.andExpect { status { isOk() } }.andReturn().response
        val token = responseOf(login.contentAsString)["accessToken"].asText()

        val projectList = mockMvc.get("/api/projects") { header("Authorization", bearer(token)) }
            .andExpect { status { isOk() } }.andReturn().response
        val website = responseOf(projectList.contentAsString).first { it["name"].asText() == "Website Redesign" }

        val summary = mockMvc.get("/api/projects/${website["id"].asLong()}/summary") {
            header("Authorization", bearer(token))
        }.andExpect { status { isOk() } }.andReturn().response
        assertTrue(responseOf(summary.contentAsString)["overdue"].asLong() > 0, "The demo should show overdue tasks")
    }
}
