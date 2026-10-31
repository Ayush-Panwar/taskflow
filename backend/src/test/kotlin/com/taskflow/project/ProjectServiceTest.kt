package com.taskflow.project

import com.taskflow.common.error.NotFoundException
import com.taskflow.common.error.UnknownUserException
import com.taskflow.security.AuthenticatedUser
import com.taskflow.user.User
import com.taskflow.user.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional

class ProjectServiceTest {

    private val projects = mockk<ProjectRepository>()
    private val users = mockk<UserRepository>()
    private val service = ProjectService(projects, users)

    private val currentUser = AuthenticatedUser(id = 1)
    private val owner = User("Asha", "asha@example.com", "hash").apply { id = 1 }

    @Test
    fun `create makes the current user the owner and cleans up the text`() {
        val saved = slot<Project>()
        every { users.findById(1) } returns Optional.of(owner)
        every { projects.save(capture(saved)) } answers { saved.captured }

        val response = service.create(currentUser, CreateProjectRequest(name = "  Website  ", description = "   "))

        assertSame(owner, saved.captured.owner)
        assertEquals("Website", response.name)
        assertNull(response.description)
    }

    @Test
    fun `create rejects a valid token whose user no longer exists`() {
        every { users.findById(1) } returns Optional.empty()

        assertThrows<UnknownUserException> {
            service.create(currentUser, CreateProjectRequest(name = "Website"))
        }
    }

    @Test
    fun `requireOwned returns a project the user owns`() {
        val project = Project("Website", null, owner)
        every { projects.findByIdAndOwnerId(5, 1) } returns project

        assertSame(project, service.requireOwned(currentUser, 5))
    }

    @Test
    fun `requireOwned treats someone else's project as not found`() {
        every { projects.findByIdAndOwnerId(5, 1) } returns null

        assertThrows<NotFoundException> { service.requireOwned(currentUser, 5) }
    }
}
