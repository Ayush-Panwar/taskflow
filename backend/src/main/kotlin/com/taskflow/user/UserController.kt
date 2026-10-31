package com.taskflow.user

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(private val userService: UserService) {

    /** Not in the original endpoint list: the task form and the assignee filter need people to choose from. */
    @GetMapping("/api/users")
    fun list(): List<UserResponse> = userService.listAll()
}
