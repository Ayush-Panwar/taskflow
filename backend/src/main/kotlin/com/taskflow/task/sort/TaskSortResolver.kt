package com.taskflow.task.sort

import com.taskflow.common.error.FieldValidationException
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

data class TaskSort(val strategy: TaskSortStrategy, val direction: Sort.Direction)

/**
 * Turns the sort query parameter into a strategy. Spring injects every [TaskSortStrategy] bean, and
 * only those can be selected, so clients can never sort by arbitrary columns such as a password hash.
 */
@Component
class TaskSortResolver(strategies: List<TaskSortStrategy>) {

    private val strategiesByField = strategies.associateBy { it.field }

    fun resolve(sort: String?): TaskSort {
        val parts = sort?.takeIf { it.isNotBlank() }?.split(",")?.map { it.trim() }
            ?: return TaskSort(defaultStrategy(), defaultStrategy().defaultDirection)
        val strategy = strategiesByField[parts[0]]
            ?: throw FieldValidationException("sort", "Sort must be one of: ${strategiesByField.keys.sorted().joinToString(", ")}")
        return TaskSort(strategy, directionFrom(parts, strategy))
    }

    private fun directionFrom(parts: List<String>, strategy: TaskSortStrategy): Sort.Direction = when {
        parts.size == 1 -> strategy.defaultDirection
        parts.size == 2 && parts[1].equals("asc", ignoreCase = true) -> Sort.Direction.ASC
        parts.size == 2 && parts[1].equals("desc", ignoreCase = true) -> Sort.Direction.DESC
        else -> throw FieldValidationException("sort", "Sort direction must be asc or desc")
    }

    private fun defaultStrategy(): TaskSortStrategy = strategiesByField.getValue(DEFAULT_FIELD)

    private companion object {
        const val DEFAULT_FIELD = "dueDate"
    }
}
