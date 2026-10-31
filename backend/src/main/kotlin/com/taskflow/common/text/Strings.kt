package com.taskflow.common.text

import com.taskflow.common.error.FieldValidationException

/** Optional text fields store null rather than an empty or whitespace-only string. */
fun String?.trimToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/**
 * For required text. @NotBlank only recognises ordinary spaces, so text made entirely of other
 * whitespace, such as an em space, gets past it, and trimming would then leave an empty string.
 */
fun String.trimRequired(field: String, label: String): String =
    trim().ifEmpty { throw FieldValidationException(field, "$label is required") }
