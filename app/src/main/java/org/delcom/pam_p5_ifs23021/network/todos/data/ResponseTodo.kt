package org.delcom.pam_p5_ifs23021.network.todos.data

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class ResponseTodos (
    val todos: List<ResponseTodoData>
)

@Serializable
data class ResponseTodo (
    val todo: ResponseTodoData
)

@Serializable
data class ResponseTodoData(
    val id: String = "",
    @SerializedName("user_id")
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    @SerializedName("is_done")
    val isDone: Boolean = false,
    val priority: String = TodoPriority.LOW.name,
    val cover: String? = null,
    @SerializedName("created_at")
    val createdAt: String = "",
    @SerializedName("updated_at")
    var updatedAt: String = ""
)

@Serializable
data class ResponseTodoAdd (
    val todoId: String
)


