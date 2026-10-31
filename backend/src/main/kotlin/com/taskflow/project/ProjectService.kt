package com.taskflow.project

import com.taskflow.common.error.NotFoundException
import com.taskflow.common.error.UnknownUserException
import com.taskflow.common.text.trimRequired
import com.taskflow.common.text.trimToNull
import com.taskflow.security.AuthenticatedUser
import com.taskflow.user.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectService(
    private val projects: ProjectRepository,
    private val users: UserRepository,
) {
    @Transactional(readOnly = true)
    fun listFor(user: AuthenticatedUser): List<ProjectResponse> =
        projects.findAllByOwnerIdOrderByCreatedAtDesc(user.id).map(ProjectResponse::from)

    @Transactional(readOnly = true)
    fun get(user: AuthenticatedUser, projectId: Long): ProjectResponse =
        ProjectResponse.from(requireOwned(user, projectId))

    @Transactional
    fun create(user: AuthenticatedUser, request: CreateProjectRequest): ProjectResponse {
        val owner = users.findByIdOrNull(user.id) ?: throw UnknownUserException()
        val project = Project(
            name = request.name!!.trimRequired("name", "Name"),
            description = request.description.trimToNull(),
            owner = owner,
        )
        return ProjectResponse.from(projects.save(project))
    }

    /**
     * The single access check for projects. A project owned by someone else gets the same 404 as one
     * that does not exist, so ids cannot be probed to find out which projects exist.
     */
    @Transactional(readOnly = true)
    fun requireOwned(user: AuthenticatedUser, projectId: Long): Project =
        projects.findByIdAndOwnerId(projectId, user.id) ?: throw NotFoundException("Project not found")
}
