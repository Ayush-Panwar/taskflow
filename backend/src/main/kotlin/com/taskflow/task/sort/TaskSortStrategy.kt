package com.taskflow.task.sort

import com.taskflow.task.Task
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Root
import org.springframework.data.domain.Sort

/**
 * Strategy pattern: one implementation per way of ordering tasks. Each has its own rules, and
 * [TaskSortResolver] picks one by name. Adding a sort option means adding one new @Component.
 */
interface TaskSortStrategy {
    /** The name clients use in the sort query parameter. */
    val field: String

    /** Used when the client does not ask for a direction. */
    val defaultDirection: Sort.Direction

    fun orders(root: Root<Task>, cb: CriteriaBuilder, direction: Sort.Direction): List<Order>
}

internal fun CriteriaBuilder.order(expression: Expression<*>, direction: Sort.Direction): Order =
    if (direction.isAscending) asc(expression) else desc(expression)
