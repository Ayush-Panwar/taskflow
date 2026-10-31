package com.taskflow.user

/** A user's own account details, returned only to that user when they register or log in. */
data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
) {
    companion object {
        fun from(user: User) = UserResponse(id = user.id, name = user.name, email = user.email)
    }
}

/**
 * What other people can see about a user. Email addresses stay private, because anyone can
 * register, and could otherwise collect the email of every account.
 */
data class UserSummary(
    val id: Long,
    val name: String,
) {
    companion object {
        fun from(user: User) = UserSummary(id = user.id, name = user.name)
    }
}
