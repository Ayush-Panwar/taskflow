package com.taskflow.task

import com.taskflow.common.PageResponse
import com.taskflow.common.error.FieldValidationException
import com.taskflow.common.error.NotFoundException
import com.taskflow.common.text.trimToNull
import com.taskflow.project.ProjectService
import com.taskflow.security.AuthenticatedUser
import com.taskflow.task.activity.TaskActivityRepository
import com.taskflow.task.activity.TaskActivityResponse
import com.taskflow.task.activity.TaskStatusChanged
import com.taskflow.task.sort.TaskSortResolver
import com.taskflow.user.User
import com.taskflow.user.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Service
class TaskService(
    private val tasks: TaskRepository,
    private val activityLog: TaskActivityRepository,
    private val projects: ProjectService,
    private val users: UserRepository,
    private val responses: TaskResponseFactory,
    private val sortResolver: TaskSortResolver,
    private val events: ApplicationEventPublisher,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun summary(user: AuthenticatedUser, projectId: Long): ProjectSummaryResponse {
        projects.requireOwned(user, projectId)
        // GROUP BY returns no row for a status without tasks, so every status starts at zero.
        val counts = TaskStatus.entries.associateWith { 0L } +
            tasks.countByStatus(projectId).associate { it.status to it.count }
        return ProjectSummaryResponse(
            projectId = projectId,
            counts = counts,
            total = counts.values.sum(),
            overdue = tasks.countOverdue(projectId, LocalDate.now(clock)),
        )
    }

    @Transactional(readOnly = true)
    fun list(user: AuthenticatedUser, projectId: Long, query: TaskListQuery): PageResponse<TaskResponse> {
        projects.requireOwned(user, projectId)
        val specification = TaskSpecifications.inProject(projectId)
            .and(TaskSpecifications.hasStatus(query.status))
            .and(TaskSpecifications.hasPriority(query.priority))
            .and(TaskSpecifications.assignedTo(query.assignee))
            .and(TaskSpecifications.sortedBy(sortResolver.resolve(query.sort)))
        val page = tasks.findAll(specification, PageRequest.of(query.page, query.size))
        return PageResponse.from(page.map(responses::create))
    }

    @Transactional
    fun create(user: AuthenticatedUser, projectId: Long, request: CreateTaskRequest): TaskResponse {
        val project = projects.requireOwned(user, projectId)
        val task = Task(
            title = request.title!!.trim(),
            description = request.description.trimToNull(),
            status = request.status ?: TaskStatus.TODO,
            priority = request.priority ?: TaskPriority.MEDIUM,
            dueDate = request.dueDate,
            assignee = resolveAssignee(request.assigneeId),
            project = project,
        )
        return responses.create(tasks.save(task))
    }

    @Transactional
    fun update(user: AuthenticatedUser, taskId: Long, request: UpdateTaskRequest): TaskResponse {
        val task = requireOwned(user, taskId)
        task.title = request.title!!.trim()
        task.description = request.description.trimToNull()
        task.priority = request.priority ?: task.priority
        task.dueDate = request.dueDate
        task.assignee = resolveAssignee(request.assigneeId)
        request.status?.let { changeStatus(task, it, user) }
        return flushAndRespond(task)
    }

    @Transactional
    fun updateStatus(user: AuthenticatedUser, taskId: Long, request: UpdateTaskStatusRequest): TaskResponse {
        val task = requireOwned(user, taskId)
        changeStatus(task, request.status!!, user)
        return flushAndRespond(task)
    }

    @Transactional
    fun delete(user: AuthenticatedUser, taskId: Long) {
        tasks.delete(requireOwned(user, taskId))
    }

    @Transactional(readOnly = true)
    fun activity(user: AuthenticatedUser, taskId: Long): List<TaskActivityResponse> {
        requireOwned(user, taskId)
        return activityLog.findAllByTaskIdOrderByOccurredAtDescIdDesc(taskId).map(TaskActivityResponse::from)
    }

    private fun changeStatus(task: Task, newStatus: TaskStatus, user: AuthenticatedUser) {
        if (task.status == newStatus) return
        val previousStatus = task.status
        task.status = newStatus
        events.publishEvent(TaskStatusChanged(task, previousStatus, newStatus, user.id))
    }

    /** Writes the change now, so that the response carries the new updatedAt. */
    private fun flushAndRespond(task: Task): TaskResponse {
        tasks.flush()
        return responses.create(task)
    }

    /** The single access check for tasks: a task can only be reached through a project the user owns. */
    private fun requireOwned(user: AuthenticatedUser, taskId: Long): Task =
        tasks.findOwnedTask(taskId, user.id) ?: throw NotFoundException("Task not found")

    private fun resolveAssignee(assigneeId: Long?): User? =
        assigneeId?.let { id ->
            users.findByIdOrNull(id) ?: throw FieldValidationException("assigneeId", "This user does not exist")
        }
}
