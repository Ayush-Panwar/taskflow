package com.taskflow.task

import com.taskflow.user.UserResponse
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate

data class CreateTaskRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 120, message = "Title must be at most 120 characters")
    val title: String? = null,

    @field:Size(max = 2000, message = "Description must be at most 2000 characters")
    val description: String? = null,

    /** Defaults to TODO. */
    val status: TaskStatus? = null,

    /** Defaults to MEDIUM. */
    val priority: TaskPriority? = null,

    @field:FutureOrPresent(message = "Due date cannot be in the past")
    val dueDate: LocalDate? = null,

    val assigneeId: Long? = null,
)

/**
 * Replaces a task's editable fields, so an omitted description, due date or assignee is cleared.
 *
 * This is a separate class from [CreateTaskRequest] because the past-date rule applies only on create.
 * With a shared class, an overdue task could not even have its title edited.
 */
data class UpdateTaskRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 120, message = "Title must be at most 120 characters")
    val title: String? = null,

    @field:Size(max = 2000, message = "Description must be at most 2000 characters")
    val description: String? = null,

    /** Left unchanged when omitted. */
    val status: TaskStatus? = null,

    /** Left unchanged when omitted. */
    val priority: TaskPriority? = null,

    val dueDate: LocalDate? = null,

    val assigneeId: Long? = null,
)

data class UpdateTaskStatusRequest(
    @field:NotNull(message = "Status is required")
    val status: TaskStatus? = null,
)

data class TaskResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val status: TaskStatus,
    val priority: TaskPriority,
    val dueDate: LocalDate?,
    /** Decided by the server, so the highlighted cards and the summary count always agree. */
    val overdue: Boolean,
    val assignee: UserResponse?,
    val projectId: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)
