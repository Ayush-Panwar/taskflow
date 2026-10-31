package com.taskflow.task

import com.taskflow.support.ApiTest
import com.taskflow.support.TestUser
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvcResultMatchersDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDate

class TaskListApiTest : ApiTest() {

    @Autowired
    private lateinit var taskRepository: TaskRepository

    private lateinit var owner: TestUser
    private lateinit var teammate: TestUser
    private var projectId: Long = 0

    private val today: LocalDate = LocalDate.now()

    @BeforeEach
    fun setUpProject() {
        owner = newUser("Owner")
        teammate = newUser("Teammate")
        val response = mockMvc.post("/api/projects") {
            header("Authorization", bearer(owner.token))
            contentType = MediaType.APPLICATION_JSON
            content = json("name" to "Board")
        }.andExpect { status { isCreated() } }.andReturn().response
        projectId = responseOf(response.contentAsString)["id"].asLong()
    }

    private fun addTask(
        title: String,
        status: TaskStatus = TaskStatus.TODO,
        priority: TaskPriority = TaskPriority.MEDIUM,
        dueDate: LocalDate? = null,
        assigneeId: Long? = null,
    ) {
        mockMvc.post("/api/projects/$projectId/tasks") {
            header("Authorization", bearer(owner.token))
            contentType = MediaType.APPLICATION_JSON
            content = json(
                "title" to title,
                "status" to status.name,
                "priority" to priority.name,
                "dueDate" to dueDate?.toString(),
                "assigneeId" to assigneeId,
            )
        }.andExpect { status { isCreated() } }
    }

    private fun listTasks(query: String, expectations: MockMvcResultMatchersDsl.() -> Unit) {
        mockMvc.get("/api/projects/$projectId/tasks?$query") {
            header("Authorization", bearer(owner.token))
        }.andExpect(expectations)
    }

    private fun expectTitles(query: String, vararg titles: String) = listTasks(query) {
        status { isOk() }
        jsonPath("$.content[*].title") { value(contains(*titles)) }
    }

    @Test
    fun `an empty project returns an empty page`() {
        listTasks("") {
            status { isOk() }
            jsonPath("$.content") { isEmpty() }
            jsonPath("$.totalElements") { value(0) }
            jsonPath("$.hasNext") { value(false) }
        }
    }

    @Test
    fun `filters by status`() {
        addTask("Todo", status = TaskStatus.TODO)
        addTask("Doing", status = TaskStatus.IN_PROGRESS)
        addTask("Finished", status = TaskStatus.DONE)

        expectTitles("status=IN_PROGRESS", "Doing")
    }

    @Test
    fun `filters by priority`() {
        addTask("Low", priority = TaskPriority.LOW)
        addTask("High", priority = TaskPriority.HIGH)

        expectTitles("priority=HIGH", "High")
    }

    @Test
    fun `filters by assignee`() {
        addTask("Mine", assigneeId = owner.id)
        addTask("Theirs", assigneeId = teammate.id)
        addTask("Nobody's")

        expectTitles("assignee=${teammate.id}", "Theirs")
    }

    @Test
    fun `filters combine`() {
        addTask("Match", status = TaskStatus.TODO, priority = TaskPriority.HIGH, assigneeId = teammate.id)
        addTask("Wrong status", status = TaskStatus.DONE, priority = TaskPriority.HIGH, assigneeId = teammate.id)
        addTask("Wrong priority", status = TaskStatus.TODO, priority = TaskPriority.LOW, assigneeId = teammate.id)
        addTask("Wrong assignee", status = TaskStatus.TODO, priority = TaskPriority.HIGH, assigneeId = owner.id)

        expectTitles("status=TODO&priority=HIGH&assignee=${teammate.id}", "Match")
    }

    @Test
    fun `sorts by due date with undated tasks last, in both directions`() {
        addTask("No date")
        addTask("Later", dueDate = today.plusDays(9))
        addTask("Sooner", dueDate = today.plusDays(1))

        expectTitles("sort=dueDate", "Sooner", "Later", "No date")
        expectTitles("sort=dueDate,asc", "Sooner", "Later", "No date")
        expectTitles("sort=dueDate,desc", "Later", "Sooner", "No date")
    }

    @Test
    fun `sorts by priority in business order rather than alphabetically`() {
        addTask("Low", priority = TaskPriority.LOW)
        addTask("High", priority = TaskPriority.HIGH)
        addTask("Medium", priority = TaskPriority.MEDIUM)

        expectTitles("sort=priority", "High", "Medium", "Low")
        expectTitles("sort=priority,desc", "High", "Medium", "Low")
        expectTitles("sort=priority,asc", "Low", "Medium", "High")
    }

    @Test
    fun `sorts by due date by default`() {
        addTask("Later", dueDate = today.plusDays(5))
        addTask("Sooner", dueDate = today.plusDays(1))

        expectTitles("", "Sooner", "Later")
    }

    @Test
    fun `paginates with accurate totals`() {
        (1..5).forEach { addTask("Task $it", dueDate = today.plusDays(it.toLong())) }

        listTasks("size=2&page=0") {
            status { isOk() }
            jsonPath("$.content[*].title") { value(contains("Task 1", "Task 2")) }
            jsonPath("$.page") { value(0) }
            jsonPath("$.size") { value(2) }
            jsonPath("$.totalElements") { value(5) }
            jsonPath("$.totalPages") { value(3) }
            jsonPath("$.hasNext") { value(true) }
        }
        listTasks("size=2&page=2") {
            status { isOk() }
            jsonPath("$.content[*].title") { value(contains("Task 5")) }
            jsonPath("$.hasNext") { value(false) }
        }
    }

    @Test
    fun `tasks with equal sort values never repeat or vanish across pages`() {
        (1..6).forEach { addTask("Task $it", dueDate = today.plusDays(1)) }

        val seen = (0..2).flatMap { page ->
            val body = mockMvc.get("/api/projects/$projectId/tasks?sort=dueDate&size=2&page=$page") {
                header("Authorization", bearer(owner.token))
            }.andReturn().response.contentAsString
            responseOf(body)["content"].map { it["id"].asLong() }
        }

        check(seen.size == 6 && seen.toSet().size == 6) { "Expected 6 distinct tasks across pages, got $seen" }
    }

    @Test
    fun `filtering and sorting work together with pagination`() {
        addTask("Skip", priority = TaskPriority.LOW)
        addTask("Second", priority = TaskPriority.HIGH, dueDate = today.plusDays(2))
        addTask("First", priority = TaskPriority.HIGH, dueDate = today.plusDays(1))
        addTask("Third", priority = TaskPriority.HIGH, dueDate = today.plusDays(3))

        listTasks("priority=HIGH&sort=dueDate&size=2") {
            status { isOk() }
            jsonPath("$.content[*].title") { value(contains("First", "Second")) }
            jsonPath("$.totalElements") { value(3) }
        }
    }

    @Test
    fun `invalid query values are field errors rather than server errors`() {
        listTasks("status=WAITING") {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].field") { value("status") }
            jsonPath("$.fieldErrors[0].message") { value("Must be one of: TODO, IN_PROGRESS, DONE") }
        }
        listTasks("priority=URGENT") {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].field") { value("priority") }
        }
        listTasks("assignee=someone") {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].field") { value("assignee") }
            jsonPath("$.fieldErrors[0].message") { value("Must be a number") }
        }
        listTasks("page=-1") {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].field") { value("page") }
        }
        listTasks("page=abc") {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].field") { value("page") }
            jsonPath("$.fieldErrors[0].message") { value("Must be a number") }
        }
        listTasks("size=1000") {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].message") { value("Size must be between 1 and 100") }
        }
        listTasks("sort=password") {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].field") { value("sort") }
        }
    }

    @Test
    fun `a past-due task that is not done comes back flagged as overdue`() {
        addTask("Late")
        addTask("Finished", status = TaskStatus.DONE)
        taskRepository.findAll()
            .filter { it.project.id == projectId }
            .forEach { taskRepository.save(it.apply { dueDate = today.minusDays(1) }) }

        listTasks("status=TODO") { jsonPath("$.content[0].overdue") { value(true) } }
        listTasks("status=DONE") { jsonPath("$.content[0].overdue") { value(false) } }
    }

    @Test
    fun `nobody can list tasks in a project they do not own`() {
        addTask("Secret")

        mockMvc.get("/api/projects/$projectId/tasks") {
            header("Authorization", bearer(teammate.token))
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.message") { value("Project not found") }
        }
    }
}
