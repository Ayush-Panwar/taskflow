package com.taskflow.user

import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(private val users: UserRepository) {

    @Transactional(readOnly = true)
    fun listAll(): List<UserSummary> =
        users.findAll(Sort.by("name")).map(UserSummary::from)
}
