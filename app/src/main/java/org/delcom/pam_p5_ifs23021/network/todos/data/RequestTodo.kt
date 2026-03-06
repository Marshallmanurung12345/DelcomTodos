package org.delcom.pam_p5_ifs23021.network.todos.data

import kotlinx.serialization.Serializable

enum class TodoPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}

@Serializable
data class RequestTodo (
    val title: String,
    val description: String,
    val isDone: Boolean = false,
    val priority: String = TodoPriority.LOW.name
)