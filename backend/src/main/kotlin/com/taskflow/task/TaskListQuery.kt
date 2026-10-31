package com.taskflow.task

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/** The query parameters of GET /api/projects/{id}/tasks. */
data class TaskListQuery(
    val status: TaskStatus? = null,

    val priority: TaskPriority? = null,

    /** A user id. */
    val assignee: Long? = null,

    /** "dueDate" or "priority", optionally with a direction, such as "priority,asc". */
    val sort: String? = null,

    // The upper limit keeps page times size within what the database query's offset can hold.
    @field:Min(value = 0, message = "Page must be 0 or greater")
    @field:Max(value = 1_000_000, message = "Page is too large")
    val page: Int = 0,

    @field:Min(value = 1, message = "Size must be between 1 and 100")
    @field:Max(value = 100, message = "Size must be between 1 and 100")
    val size: Int = 20,
)
