package org.delcom.pam_p5_ifs23021.ui.viewmodels

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import org.delcom.pam_p5_ifs23021.network.data.ResponseMessage
import org.delcom.pam_p5_ifs23021.network.todos.data.RequestTodo
import org.delcom.pam_p5_ifs23021.network.todos.data.RequestUserChange
import org.delcom.pam_p5_ifs23021.network.todos.data.RequestUserChangePassword
import org.delcom.pam_p5_ifs23021.network.todos.data.ResponseTodo
import org.delcom.pam_p5_ifs23021.network.todos.data.ResponseTodoAdd
import org.delcom.pam_p5_ifs23021.network.todos.data.ResponseTodoData
import org.delcom.pam_p5_ifs23021.network.todos.data.ResponseTodos
import org.delcom.pam_p5_ifs23021.network.todos.data.ResponseUser
import org.delcom.pam_p5_ifs23021.network.todos.data.ResponseUserData
import org.delcom.pam_p5_ifs23021.network.todos.service.ITodoRepository
import javax.inject.Inject

sealed interface ProfileUIState {
    data class Success(val data: ResponseUserData) : ProfileUIState
    data class Error(val message: String) : ProfileUIState
    object Loading : ProfileUIState
}

sealed interface TodosUIState {
    data class Success(val data: List<ResponseTodoData>) : TodosUIState
    data class Error(val message: String) : TodosUIState
    object Loading : TodosUIState
}

sealed interface TodoUIState {
    data class Success(val data: ResponseTodoData) : TodoUIState
    data class Error(val message: String) : TodoUIState
    object Loading : TodoUIState
}

sealed interface TodoActionUIState {
    data class Success(val message: String) : TodoActionUIState
    data class Error(val message: String) : TodoActionUIState
    object Loading : TodoActionUIState
}

data class UIStateTodo(
    val profile: ProfileUIState = ProfileUIState.Loading,
    val todos: TodosUIState = TodosUIState.Loading,
    var todo: TodoUIState = TodoUIState.Loading,
    var todoAdd: TodoActionUIState = TodoActionUIState.Loading,
    var todoChange: TodoActionUIState = TodoActionUIState.Loading,
    var todoDelete: TodoActionUIState = TodoActionUIState.Loading,
    var todoChangeCover: TodoActionUIState = TodoActionUIState.Loading,
    var profileChange: TodoActionUIState = TodoActionUIState.Loading,
    var profileChangePassword: TodoActionUIState = TodoActionUIState.Loading,
    var profileChangePhoto: TodoActionUIState = TodoActionUIState.Loading,
    // Pagination state
    val currentPage: Int = 1,
    val hasMoreData: Boolean = true,
    val isLoadingMore: Boolean = false,
    val allTodos: List<ResponseTodoData> = emptyList()
)

@HiltViewModel
@Keep
class TodoViewModel @Inject constructor(
    private val repository: ITodoRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UIStateTodo())
    val uiState = _uiState.asStateFlow()

    companion object {
        const val PAGE_SIZE = 10
    }

    fun getProfile(authToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(profile = ProfileUIState.Loading) }
            val result = runCatching { repository.getUserMe(authToken) }
            _uiState.update { state ->
                val nextState = result.fold(
                    onSuccess = { response: ResponseMessage<ResponseUser?> ->
                        val userData = response.data?.user
                        if (response.status == "success" && userData != null) {
                            ProfileUIState.Success(userData)
                        } else {
                            ProfileUIState.Error(response.message)
                        }
                    },
                    onFailure = { error ->
                        ProfileUIState.Error(error.message ?: "Unknown error")
                    }
                )
                state.copy(profile = nextState)
            }
        }
    }

    fun updateProfile(
        authToken: String,
        name: String,
        username: String,
        about: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(profileChange = TodoActionUIState.Loading) }
            val result = runCatching {
                repository.putUserMe(
                    authToken,
                    RequestUserChange(name = name, username = username, about = about)
                )
            }
            _uiState.update { state ->
                val nextState = result.fold(
                    onSuccess = { response ->
                        if (response.status == "success") {
                            // Update cached profile data
                            val updatedProfile = if (state.profile is ProfileUIState.Success) {
                                val cur = (state.profile as ProfileUIState.Success).data
                                ProfileUIState.Success(cur.copy(name = name, username = username, about = about))
                            } else state.profile
                            return@update state.copy(
                                profileChange = TodoActionUIState.Success(response.message),
                                profile = updatedProfile
                            )
                        } else {
                            TodoActionUIState.Error(response.message)
                        }
                    },
                    onFailure = { error -> TodoActionUIState.Error(error.message ?: "Unknown error") }
                )
                state.copy(profileChange = nextState)
            }
        }
    }

    fun changePassword(
        authToken: String,
        currentPassword: String,
        newPassword: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(profileChangePassword = TodoActionUIState.Loading) }
            val result = runCatching {
                repository.putUserMePassword(
                    authToken,
                    RequestUserChangePassword(newPassword = newPassword, password = currentPassword)
                )
            }
            _uiState.update { state ->
                val nextState = result.fold(
                    onSuccess = { response ->
                        if (response.status == "success") TodoActionUIState.Success(response.message)
                        else TodoActionUIState.Error(response.message)
                    },
                    onFailure = { error -> TodoActionUIState.Error(error.message ?: "Unknown error") }
                )
                state.copy(profileChangePassword = nextState)
            }
        }
    }

    fun updateProfilePhoto(authToken: String, file: MultipartBody.Part) {
        viewModelScope.launch {
            _uiState.update { it.copy(profileChangePhoto = TodoActionUIState.Loading) }
            val result = runCatching { repository.putUserMePhoto(authToken, file) }
            _uiState.update { state ->
                val nextState = result.fold(
                    onSuccess = { response ->
                        if (response.status == "success") TodoActionUIState.Success(response.message)
                        else TodoActionUIState.Error(response.message)
                    },
                    onFailure = { error -> TodoActionUIState.Error(error.message ?: "Unknown error") }
                )
                state.copy(profileChangePhoto = nextState)
            }
        }
    }

    /**
     * Load first page (resets pagination)
     */
    fun getAllTodos(
        authToken: String,
        search: String? = null,
        isDone: Boolean? = null,
        priority: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    todos = TodosUIState.Loading,
                    currentPage = 1,
                    hasMoreData = true,
                    allTodos = emptyList()
                )
            }
            val result = runCatching {
                repository.getTodos(
                    authToken = authToken,
                    search = search,
                    isDone = isDone,
                    priority = priority,
                    page = 1,
                    perPage = PAGE_SIZE
                )
            }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { response: ResponseMessage<ResponseTodos?> ->
                        val todos = response.data?.todos ?: emptyList()
                        val sorted = todos.sortedByPriority()
                        if (response.status == "success") {
                            state.copy(
                                todos = TodosUIState.Success(sorted),
                                allTodos = sorted,
                                currentPage = 1,
                                hasMoreData = todos.size >= PAGE_SIZE
                            )
                        } else {
                            state.copy(todos = TodosUIState.Error(response.message))
                        }
                    },
                    onFailure = { error ->
                        state.copy(todos = TodosUIState.Error(error.message ?: "Unknown error"))
                    }
                )
            }
        }
    }

    /**
     * Load next page (appends to existing list)
     */
    fun loadMoreTodos(
        authToken: String,
        search: String? = null,
        isDone: Boolean? = null,
        priority: String? = null
    ) {
        val currentState = _uiState.value
        if (!currentState.hasMoreData || currentState.isLoadingMore) return

        viewModelScope.launch {
            val nextPage = currentState.currentPage + 1
            _uiState.update { it.copy(isLoadingMore = true) }
            val result = runCatching {
                repository.getTodos(
                    authToken = authToken,
                    search = search,
                    isDone = isDone,
                    priority = priority,
                    page = nextPage,
                    perPage = PAGE_SIZE
                )
            }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { response ->
                        val newTodos = response.data?.todos ?: emptyList()
                        val sorted = newTodos.sortedByPriority()
                        if (response.status == "success") {
                            val combined = state.allTodos + sorted
                            state.copy(
                                todos = TodosUIState.Success(combined),
                                allTodos = combined,
                                currentPage = nextPage,
                                hasMoreData = newTodos.size >= PAGE_SIZE,
                                isLoadingMore = false
                            )
                        } else {
                            state.copy(isLoadingMore = false)
                        }
                    },
                    onFailure = {
                        state.copy(isLoadingMore = false)
                    }
                )
            }
        }
    }

    private fun List<ResponseTodoData>.sortedByPriority(): List<ResponseTodoData> {
        val priorityOrder = mapOf("HIGH" to 0, "MEDIUM" to 1, "LOW" to 2)
        return sortedBy { priorityOrder[it.priority.uppercase()] ?: 2 }
    }

    fun postTodo(
        authToken: String,
        title: String,
        description: String,
        priority: String = "LOW"
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(todoAdd = TodoActionUIState.Loading) }
            val result = runCatching {
                repository.postTodo(
                    authToken = authToken,
                    request = RequestTodo(title = title, description = description, priority = priority)
                )
            }
            _uiState.update { state ->
                val nextState = result.fold(
                    onSuccess = { response: ResponseMessage<ResponseTodoAdd?> ->
                        if (response.status == "success") {
                            TodoActionUIState.Success(response.message)
                        } else {
                            TodoActionUIState.Error(response.message)
                        }
                    },
                    onFailure = { error ->
                        TodoActionUIState.Error(error.message ?: "Unknown error")
                    }
                )
                state.copy(todoAdd = nextState)
            }
        }
    }

    fun resetTodoAdd() {
        _uiState.update { it.copy(todoAdd = TodoActionUIState.Loading) }
    }

    fun getTodoById(authToken: String, todoId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(todo = TodoUIState.Loading) }
            val result = runCatching { repository.getTodoById(authToken, todoId) }
            _uiState.update { state ->
                val nextState = result.fold(
                    onSuccess = { response: ResponseMessage<ResponseTodo?> ->
                        val todo = response.data?.todo
                        if (response.status == "success" && todo != null) {
                            TodoUIState.Success(todo)
                        } else {
                            TodoUIState.Error(response.message)
                        }
                    },
                    onFailure = { error ->
                        TodoUIState.Error(error.message ?: "Unknown error")
                    }
                )
                state.copy(todo = nextState)
            }
        }
    }

    fun putTodo(
        authToken: String,
        todoId: String,
        title: String,
        description: String,
        isDone: Boolean,
        priority: String = "LOW"
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(todoChange = TodoActionUIState.Loading) }
            val result = runCatching {
                repository.putTodo(
                    authToken = authToken,
                    todoId = todoId,
                    request = RequestTodo(
                        title = title,
                        description = description,
                        isDone = isDone,
                        priority = priority
                    )
                )
            }
            _uiState.update { state ->
                val nextState = result.fold(
                    onSuccess = { response: ResponseMessage<String?> ->
                        if (response.status == "success") {
                            TodoActionUIState.Success(response.message)
                        } else {
                            TodoActionUIState.Error(response.message)
                        }
                    },
                    onFailure = { error ->
                        TodoActionUIState.Error(error.message ?: "Unknown error")
                    }
                )
                state.copy(todoChange = nextState)
            }
        }
    }

    fun putTodoCover(
        authToken: String,
        todoId: String,
        file: MultipartBody.Part
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(todoChangeCover = TodoActionUIState.Loading) }
            val result = runCatching {
                repository.putTodoCover(authToken = authToken, todoId = todoId, file = file)
            }
            _uiState.update { state ->
                val nextState = result.fold(
                    onSuccess = { response: ResponseMessage<String?> ->
                        if (response.status == "success") {
                            TodoActionUIState.Success(response.message)
                        } else {
                            TodoActionUIState.Error(response.message)
                        }
                    },
                    onFailure = { error ->
                        TodoActionUIState.Error(error.message ?: "Unknown error")
                    }
                )
                state.copy(todoChangeCover = nextState)
            }
        }
    }

    fun deleteTodo(authToken: String, todoId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(todoDelete = TodoActionUIState.Loading) }
            val result = runCatching { repository.deleteTodo(authToken, todoId) }
            _uiState.update { state ->
                val nextState = result.fold(
                    onSuccess = { response: ResponseMessage<String?> ->
                        if (response.status == "success") {
                            TodoActionUIState.Success(response.message)
                        } else {
                            TodoActionUIState.Error(response.message)
                        }
                    },
                    onFailure = { error ->
                        TodoActionUIState.Error(error.message ?: "Unknown error")
                    }
                )
                state.copy(todoDelete = nextState)
            }
        }
    }
}