package com.taskflow.common.error

import java.time.Instant

/** The one error shape returned by every endpoint, including security failures. */
data class ApiError(
    val timestamp: Instant,
    val status: Int,
    val error: String,
    val code: String,
    val message: String,
    val path: String,
    val fieldErrors: List<FieldViolation> = emptyList(),
)

data class FieldViolation(
    val field: String,
    val message: String,
)
