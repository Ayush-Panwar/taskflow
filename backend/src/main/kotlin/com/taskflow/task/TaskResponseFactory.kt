package com.taskflow.task

import com.taskflow.user.UserSummary
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate

/**
 * Factory for [TaskResponse]. It is a bean rather than a plain function because the response
 * includes a derived field, overdue, and working that out needs the shared clock.
 */
@Component
class TaskResponseFactory(private val clock: Clock) {

    fun create(task: Task) = TaskResponse(
        id = task.id,
        title = task.title,
        description = task.description,
        status = task.status,
        priority = task.priority,
        dueDate = task.dueDate,
        overdue = task.isOverdue(LocalDate.now(clock)),
        assignee = task.assignee?.let(UserSummary::from),
        projectId = task.project.id,
        createdAt = task.createdAt,
        updatedAt = task.updatedAt,
    )
}
