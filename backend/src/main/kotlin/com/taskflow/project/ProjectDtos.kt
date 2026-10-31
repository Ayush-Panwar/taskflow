package com.taskflow.project

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateProjectRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 100, message = "Name must be at most 100 characters")
    val name: String? = null,

    @field:Size(max = 1000, message = "Description must be at most 1000 characters")
    val description: String? = null,
)

data class ProjectResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val createdAt: Instant,
) {
    companion object {
        fun from(project: Project) = ProjectResponse(
            id = project.id,
            name = project.name,
            description = project.description,
            createdAt = project.createdAt,
        )
    }
}
