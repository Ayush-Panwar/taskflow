package com.taskflow.task.activity

import com.taskflow.task.TaskStatus
import com.taskflow.user.UserResponse
import java.time.Instant

data class TaskActivityResponse(
    val id: Long,
    val actor: UserResponse,
    val fromStatus: TaskStatus,
    val toStatus: TaskStatus,
    val occurredAt: Instant,
) {
    companion object {
        fun from(activity: TaskActivity) = TaskActivityResponse(
            id = activity.id,
            actor = UserResponse.from(activity.actor),
            fromStatus = activity.fromStatus,
            toStatus = activity.toStatus,
            occurredAt = activity.occurredAt,
        )
    }
}
