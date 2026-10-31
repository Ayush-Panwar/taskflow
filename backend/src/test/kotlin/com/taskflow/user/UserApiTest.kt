package com.taskflow.user

import com.taskflow.support.ApiTest
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get

class UserApiTest : ApiTest() {

    @Test
    fun `lists people by name, without revealing their email addresses`() {
        val user = newUser("Asha")

        mockMvc.get("/api/users") { header("Authorization", bearer(user.token)) }.andExpect {
            status { isOk() }
            jsonPath("$[*].name") { value(hasItem("Asha")) }
            jsonPath("$[*].id") { value(hasItem(user.id.toInt())) }
            jsonPath("$[0].email") { doesNotExist() }
            jsonPath("$[0].password") { doesNotExist() }
        }
    }
}
