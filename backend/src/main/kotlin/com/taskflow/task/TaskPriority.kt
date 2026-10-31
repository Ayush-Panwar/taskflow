package com.taskflow.task

/**
 * [rank] defines the business order used for sorting. Priorities are stored as text,
 * so sorting the column directly would give the alphabetical order HIGH, LOW, MEDIUM.
 */
enum class TaskPriority(val rank: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
}
