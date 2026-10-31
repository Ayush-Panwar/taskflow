package com.taskflow.user

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Users")
class UserController(private val userService: UserService) {

    /** Not in the original endpoint list: the task form and the assignee filter need people to choose from. */
    @Operation(summary = "List the people tasks can be assigned to")
    @GetMapping("/api/users")
    fun list(): List<UserSummary> = userService.listAll()
}
