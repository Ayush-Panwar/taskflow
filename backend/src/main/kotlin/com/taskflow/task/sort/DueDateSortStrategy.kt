package com.taskflow.task.sort

import com.taskflow.task.Task
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Root
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Tasks without a due date always come last, in both directions. Databases disagree about where
 * NULL sorts by default, so the rule is stated explicitly rather than left to the database.
 */
@Component
class DueDateSortStrategy : TaskSortStrategy {
    override val field = "dueDate"
    override val defaultDirection = Sort.Direction.ASC

    override fun orders(root: Root<Task>, cb: CriteriaBuilder, direction: Sort.Direction): List<Order> {
        val dueDate = root.get<LocalDate>("dueDate")
        val hasNoDueDate = cb.selectCase<Int>()
            .`when`(cb.isNull(dueDate), cb.literal(1))
            .otherwise(cb.literal(0))
        return listOf(cb.asc(hasNoDueDate), cb.order(dueDate, direction))
    }
}
