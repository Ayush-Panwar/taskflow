package com.taskflow.task

import com.taskflow.project.Project
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
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "tasks")
class Task(
    @Column(nullable = false, length = 120)
    var title: String,

    @Column(length = 2000)
    var description: String?,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    var status: TaskStatus,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    var priority: TaskPriority,

    @Column(name = "due_date")
    var dueDate: LocalDate?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    var assignee: User?,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    var project: Project,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = createdAt

    /** Optimistic lock: a concurrent write to the same task fails instead of overwriting silently. */
    @Version
    var version: Long = 0

    /** Hibernate only calls this when a field actually changed, so a no-op save keeps the old timestamp. */
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }

    /** A finished task is never overdue, and a task due today is not overdue yet. */
    fun isOverdue(today: LocalDate): Boolean =
        status != TaskStatus.DONE && dueDate?.isBefore(today) == true
}
