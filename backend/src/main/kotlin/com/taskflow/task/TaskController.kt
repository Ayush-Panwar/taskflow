package com.taskflow.task

import com.taskflow.common.PageResponse
import com.taskflow.security.AuthenticatedUser
import com.taskflow.task.activity.TaskActivityResponse
import jakarta.validation.Valid
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
class TaskController(private val taskService: TaskService) {

    @GetMapping("/api/projects/{id}/summary")
    fun summary(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable("id") projectId: Long,
    ): ProjectSummaryResponse = taskService.summary(user, projectId)

    @GetMapping("/api/projects/{id}/tasks")
    fun list(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable("id") projectId: Long,
        @Valid @ModelAttribute query: TaskListQuery,
    ): PageResponse<TaskResponse> = taskService.list(user, projectId, query)

    @PostMapping("/api/projects/{id}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable("id") projectId: Long,
        @Valid @RequestBody request: CreateTaskRequest,
    ): TaskResponse = taskService.create(user, projectId, request)

    @PutMapping("/api/tasks/{id}")
    fun update(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable("id") taskId: Long,
        @Valid @RequestBody request: UpdateTaskRequest,
    ): TaskResponse = taskService.update(user, taskId, request)

    @PatchMapping("/api/tasks/{id}/status")
    fun updateStatus(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable("id") taskId: Long,
        @Valid @RequestBody request: UpdateTaskStatusRequest,
    ): TaskResponse = taskService.updateStatus(user, taskId, request)

    @DeleteMapping("/api/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable("id") taskId: Long,
    ) = taskService.delete(user, taskId)

    @GetMapping("/api/tasks/{id}/activity")
    fun activity(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable("id") taskId: Long,
    ): List<TaskActivityResponse> = taskService.activity(user, taskId)
}
