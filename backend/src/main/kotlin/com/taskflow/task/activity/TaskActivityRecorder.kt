package com.taskflow.task.activity

import com.taskflow.task.Task
import com.taskflow.task.TaskStatus
import com.taskflow.user.UserRepository
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.Clock

/** Published by TaskService whenever a task's status really changes. */
data class TaskStatusChanged(
    val task: Task,
    val from: TaskStatus,
    val to: TaskStatus,
    val actorId: Long,
)

/**
 * Observer pattern: TaskService announces a change and does not know who is listening. A new reaction,
 * such as sending a notification, would be another listener with no change to TaskService.
 *
 * Plain @EventListener runs immediately and inside the caller's transaction, so the status change and
 * its log entry are saved together or not at all.
 */
@Component
class TaskActivityRecorder(
    private val activity: TaskActivityRepository,
    private val users: UserRepository,
    private val clock: Clock,
) {
    @EventListener
    fun on(event: TaskStatusChanged) {
        activity.save(
            TaskActivity(
                task = event.task,
                actor = users.getReferenceById(event.actorId),
                fromStatus = event.from,
                toStatus = event.to,
                occurredAt = clock.instant(),
            ),
        )
    }
}
