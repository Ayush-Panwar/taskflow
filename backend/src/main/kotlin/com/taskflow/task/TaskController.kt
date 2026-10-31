package com.taskflow.task

import com.taskflow.common.PageResponse
import com.taskflow.security.AuthenticatedUser
import com.taskflow.task.activity.TaskActivityResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Tasks")
class TaskController(private val taskService: TaskService) {

    @Operation(summary = "Count tasks per status and the number overdue")
    @GetMapping("/api/projects/{id}/summary")
    fun summary(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable("id") projectId: Long,
    ): ProjectSummaryResponse = taskService.summary(user, projectId)

    @Operation(summary = "List a project's tasks, with filters, sorting and pagination")
    @GetMapping("/api/projects/{id}/tasks")
    fun list(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable("id") projectId: Long,
        @Valid @ModelAttribute @ParameterObject query: TaskListQuery,
    ): PageResponse<TaskResponse> = taskService.list(user, projectId, query)

    @Operation(summary = "Create a task")
    @PostMapping("/api/projects/{id}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable("id") projectId: Long,
        @Valid @RequestBody request: CreateTaskRequest,
    ): TaskResponse = taskService.create(user, projectId, request)

    @Operation(summary = "Update a task")
    @PutMapping("/api/tasks/{id}")
    fun update(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable("id") taskId: Long,
        @Valid @RequestBody request: UpdateTaskRequest,
    ): TaskResponse = taskService.update(user, taskId, request)

    @Operation(summary = "Change only the status of a task")
    @PatchMapping("/api/tasks/{id}/status")
    fun updateStatus(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable("id") taskId: Long,
        @Valid @RequestBody request: UpdateTaskStatusRequest,
    ): TaskResponse = taskService.updateStatus(user, taskId, request)

    @Operation(summary = "Delete a task")
    @DeleteMapping("/api/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable("id") taskId: Long,
    ) = taskService.delete(user, taskId)

    @Operation(summary = "See who changed a task's status and when, newest first")
    @GetMapping("/api/tasks/{id}/activity")
    fun activity(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable("id") taskId: Long,
    ): List<TaskActivityResponse> = taskService.activity(user, taskId)
}
