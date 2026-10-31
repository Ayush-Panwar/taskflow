package com.taskflow.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "taskflow.jwt")
data class JwtProperties(
    /** Signing secret from the environment. If blank, a random one is generated at startup. */
    val secret: String = "",
    val expiry: Duration = Duration.ofHours(1),
    val issuer: String = "taskflow",
)
