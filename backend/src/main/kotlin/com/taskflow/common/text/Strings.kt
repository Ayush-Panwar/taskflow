package com.taskflow.common.text

/** Optional text fields store null rather than an empty or whitespace-only string. */
fun String?.trimToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
