package com.taskflow.task

import com.taskflow.project.Project
import com.taskflow.task.sort.TaskSort
import com.taskflow.user.User
import org.springframework.data.jpa.domain.Specification

/**
 * Specification pattern: each filter is a small condition that can be combined with the others.
 * A condition that returns null is skipped, which is what makes every filter optional.
 */
object TaskSpecifications {

    fun inProject(projectId: Long) = Specification<Task> { root, _, cb ->
        cb.equal(root.get<Project>("project").get<Long>("id"), projectId)
    }

    fun hasStatus(status: TaskStatus?) = Specification<Task> { root, _, cb ->
        status?.let { cb.equal(root.get<TaskStatus>("status"), it) }
    }

    fun hasPriority(priority: TaskPriority?) = Specification<Task> { root, _, cb ->
        priority?.let { cb.equal(root.get<TaskPriority>("priority"), it) }
    }

    fun assignedTo(userId: Long?) = Specification<Task> { root, _, cb ->
        userId?.let { cb.equal(root.get<User>("assignee").get<Long>("id"), it) }
    }

    /**
     * Applies the ordering and adds no condition. Plain Spring Data sorting cannot express these
     * orderings, because they need CASE expressions.
     *
     * The id is always the last sort key. Without it, tasks with equal values could swap places
     * between queries and appear on two pages or on none.
     */
    fun sortedBy(sort: TaskSort) = Specification<Task> { root, query, cb ->
        query?.orderBy(sort.strategy.orders(root, cb, sort.direction) + cb.asc(root.get<Long>("id")))
        null
    }
}
