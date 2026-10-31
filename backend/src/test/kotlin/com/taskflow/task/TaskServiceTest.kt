package com.taskflow.task

import com.taskflow.common.error.FieldValidationException
import com.taskflow.common.error.NotFoundException
import com.taskflow.project.Project
import com.taskflow.project.ProjectService
import com.taskflow.security.AuthenticatedUser
import com.taskflow.user.User
import com.taskflow.user.UserRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Optional

class TaskServiceTest {

    private val today = LocalDate.of(2026, 3, 15)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private val tasks = mockk<TaskRepository>()
    private val projects = mockk<ProjectService>()
    private val users = mockk<UserRepository>()
    private val service = TaskService(tasks, projects, users, TaskResponseFactory(clock))

    private val currentUser = AuthenticatedUser(id = 1)
    private val owner = User("Asha", "asha@example.com", "hash").apply { id = 1 }
    private val teammate = User("Ben", "ben@example.com", "hash").apply { id = 2 }
    private val project = Project("Website", null, owner).apply { id = 10 }

    private fun aTask(
        status: TaskStatus = TaskStatus.TODO,
        priority: TaskPriority = TaskPriority.MEDIUM,
        dueDate: LocalDate? = null,
        assignee: User? = null,
        description: String? = null,
    ) = Task("A task", description, status, priority, dueDate, assignee, project).apply { id = 100 }

    private fun givenOwnedTask(task: Task) {
        every { tasks.findOwnedTask(100, 1) } returns task
        every { tasks.flush() } just runs
    }

    @Test
    fun `create applies default status and priority, and tidies the text`() {
        val saved = slot<Task>()
        every { projects.requireOwned(currentUser, 10) } returns project
        every { tasks.save(capture(saved)) } answers { saved.captured }

        val response = service.create(currentUser, 10, CreateTaskRequest(title = "  Ship it ", description = "  "))

        assertEquals("Ship it", response.title)
        assertNull(response.description)
        assertEquals(TaskStatus.TODO, response.status)
        assertEquals(TaskPriority.MEDIUM, response.priority)
        assertSame(project, saved.captured.project)
    }

    @Test
    fun `create checks project ownership before saving anything`() {
        every { projects.requireOwned(currentUser, 10) } throws NotFoundException("Project not found")

        assertThrows<NotFoundException> { service.create(currentUser, 10, CreateTaskRequest(title = "Task")) }

        verify(exactly = 0) { tasks.save(any<Task>()) }
    }

    @Test
    fun `create rejects an assignee who does not exist`() {
        every { projects.requireOwned(currentUser, 10) } returns project
        every { users.findById(99) } returns Optional.empty()

        val exception = assertThrows<FieldValidationException> {
            service.create(currentUser, 10, CreateTaskRequest(title = "Task", assigneeId = 99))
        }

        assertEquals("assigneeId", exception.fieldErrors.single().field)
    }

    @Test
    fun `update replaces the editable fields`() {
        val task = aTask(description = "Old", assignee = owner)
        givenOwnedTask(task)
        every { users.findById(2) } returns Optional.of(teammate)

        val response = service.update(
            currentUser,
            100,
            UpdateTaskRequest(title = "New", description = null, priority = TaskPriority.HIGH, dueDate = today, assigneeId = 2),
        )

        assertEquals("New", response.title)
        assertNull(response.description)
        assertEquals(TaskPriority.HIGH, response.priority)
        assertEquals(today, response.dueDate)
        assertEquals(2, response.assignee?.id)
    }

    @Test
    fun `update clears the assignee and keeps status and priority when they are omitted`() {
        val task = aTask(status = TaskStatus.IN_PROGRESS, priority = TaskPriority.HIGH, assignee = teammate)
        givenOwnedTask(task)

        val response = service.update(currentUser, 100, UpdateTaskRequest(title = "Renamed"))

        assertNull(response.assignee)
        assertEquals(TaskStatus.IN_PROGRESS, response.status)
        assertEquals(TaskPriority.HIGH, response.priority)
    }

    @Test
    fun `updating a task the user cannot reach is not found`() {
        every { tasks.findOwnedTask(100, 1) } returns null

        assertThrows<NotFoundException> { service.update(currentUser, 100, UpdateTaskRequest(title = "x")) }
        assertThrows<NotFoundException> {
            service.updateStatus(currentUser, 100, UpdateTaskStatusRequest(TaskStatus.DONE))
        }
        assertThrows<NotFoundException> { service.delete(currentUser, 100) }
    }

    @Test
    fun `updateStatus changes only the status`() {
        val task = aTask(priority = TaskPriority.LOW)
        givenOwnedTask(task)

        val response = service.updateStatus(currentUser, 100, UpdateTaskStatusRequest(TaskStatus.DONE))

        assertEquals(TaskStatus.DONE, response.status)
        assertEquals(TaskPriority.LOW, response.priority)
        verify { tasks.flush() }
    }

    @Test
    fun `delete removes the owned task`() {
        val task = aTask()
        every { tasks.findOwnedTask(100, 1) } returns task
        every { tasks.delete(task) } just runs

        service.delete(currentUser, 100)

        verify { tasks.delete(task) }
    }

    @Test
    fun `a task is overdue only if its date has passed and it is not done`() {
        assertTrue(aTask(dueDate = today.minusDays(1)).isOverdue(today), "due yesterday")
        assertFalse(aTask(dueDate = today).isOverdue(today), "due today")
        assertFalse(aTask(dueDate = today.plusDays(1)).isOverdue(today), "due tomorrow")
        assertFalse(aTask(dueDate = null).isOverdue(today), "no due date")
        assertFalse(aTask(dueDate = today.minusDays(1), status = TaskStatus.DONE).isOverdue(today), "already done")
    }

    @Test
    fun `the response reports overdue using the injected clock`() {
        givenOwnedTask(aTask(dueDate = today.minusDays(1)))

        val response = service.updateStatus(currentUser, 100, UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS))

        assertTrue(response.overdue)
    }
}
