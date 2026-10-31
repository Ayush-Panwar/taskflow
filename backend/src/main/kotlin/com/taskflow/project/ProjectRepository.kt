package com.taskflow.project

import org.springframework.data.jpa.repository.JpaRepository

interface ProjectRepository : JpaRepository<Project, Long> {
    fun findAllByOwnerIdOrderByCreatedAtDesc(ownerId: Long): List<Project>

    /** Returns null both when the project does not exist and when someone else owns it. */
    fun findByIdAndOwnerId(id: Long, ownerId: Long): Project?
}
