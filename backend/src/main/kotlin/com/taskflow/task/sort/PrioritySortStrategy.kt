package com.taskflow.task.sort

import com.taskflow.task.Task
import com.taskflow.task.TaskPriority
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Root
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

/**
 * Sorts by business importance using [TaskPriority.rank]. Sorting the text column itself would give
 * the alphabetical order HIGH, LOW, MEDIUM. The default direction puts the most important tasks first.
 */
@Component
class PrioritySortStrategy : TaskSortStrategy {
    override val field = "priority"
    override val defaultDirection = Sort.Direction.DESC

    override fun orders(root: Root<Task>, cb: CriteriaBuilder, direction: Sort.Direction): List<Order> {
        val priority = root.get<TaskPriority>("priority")
        val rank = TaskPriority.entries
            .fold(cb.selectCase<Int>()) { case, value ->
                case.`when`(cb.equal(priority, value), cb.literal(value.rank))
            }
            .otherwise(cb.literal(0))
        return listOf(cb.order(rank, direction))
    }
}
