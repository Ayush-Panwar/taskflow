package com.taskflow.task

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface TaskRepository : JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    /** Loads each page together with its assignees in one query, avoiding one extra query per task. */
    @EntityGraph(attributePaths = ["assignee"])
    override fun findAll(spec: Specification<Task>?, pageable: Pageable): Page<Task>

    /** Returns null both when the task does not exist and when its project belongs to someone else. */
    @Query(
        """
        select t from Task t
        join fetch t.project p
        left join fetch t.assignee
        where t.id = :taskId and p.owner.id = :ownerId
        """,
    )
    fun findOwnedTask(taskId: Long, ownerId: Long): Task?

    @Query(
        """
        select t.status as status, count(t) as count
        from Task t
        where t.project.id = :projectId
        group by t.status
        """,
    )
    fun countByStatus(projectId: Long): List<StatusCount>

    /** Must stay in line with [Task.isOverdue]. */
    @Query(
        """
        select count(t) from Task t
        where t.project.id = :projectId
          and t.status <> com.taskflow.task.TaskStatus.DONE
          and t.dueDate < :today
        """,
    )
    fun countOverdue(projectId: Long, today: LocalDate): Long

    interface StatusCount {
        val status: TaskStatus
        val count: Long
    }
}
