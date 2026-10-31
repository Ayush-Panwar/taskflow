package com.taskflow.task.activity

import com.taskflow.task.Task
import com.taskflow.task.TaskStatus
import com.taskflow.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

/** One status change on a task: who made it and when. */
@Entity
@Table(name = "task_activity")
class TaskActivity(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false, updatable = false)
    var task: Task,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false, updatable = false)
    var actor: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, updatable = false, length = 20)
    var fromStatus: TaskStatus,

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, updatable = false, length = 20)
    var toStatus: TaskStatus,

    @Column(name = "occurred_at", nullable = false, updatable = false)
    var occurredAt: Instant,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}
