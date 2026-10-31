package com.taskflow.task.sort

import com.taskflow.common.error.FieldValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.data.domain.Sort

class TaskSortResolverTest {

    private val dueDate = DueDateSortStrategy()
    private val priority = PrioritySortStrategy()
    private val resolver = TaskSortResolver(listOf(dueDate, priority))

    @Test
    fun `no sort parameter means soonest due date first`() {
        assertEquals(TaskSort(dueDate, Sort.Direction.ASC), resolver.resolve(null))
        assertEquals(TaskSort(dueDate, Sort.Direction.ASC), resolver.resolve("  "))
    }

    @Test
    fun `each strategy has its own default direction`() {
        assertEquals(TaskSort(dueDate, Sort.Direction.ASC), resolver.resolve("dueDate"))
        assertEquals(TaskSort(priority, Sort.Direction.DESC), resolver.resolve("priority"))
    }

    @Test
    fun `a direction can be given in any case`() {
        assertEquals(TaskSort(dueDate, Sort.Direction.DESC), resolver.resolve("dueDate,desc"))
        assertEquals(TaskSort(priority, Sort.Direction.ASC), resolver.resolve("priority,ASC"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["password", "project.owner.password", "id", "dueDate,sideways", "dueDate,asc,extra"])
    fun `anything else is rejected as a sort field error`(sort: String) {
        val exception = assertThrows<FieldValidationException> { resolver.resolve(sort) }

        assertEquals("sort", exception.fieldErrors.single().field)
    }

    @Test
    fun `the error lists the allowed fields`() {
        val exception = assertThrows<FieldValidationException> { resolver.resolve("title") }

        assertTrue(exception.fieldErrors.single().message.contains("dueDate, priority"))
    }
}
