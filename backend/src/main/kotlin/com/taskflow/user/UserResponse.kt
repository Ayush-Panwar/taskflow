package com.taskflow.user

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
) {
    companion object {
        fun from(user: User) = UserResponse(id = user.id, name = user.name, email = user.email)
    }
}
