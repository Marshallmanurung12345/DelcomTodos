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
    var todoChangeCover: TodoActionUIState = TodoActionUIState.Loading
)

@HiltViewModel
@Keep
class TodoViewModel @Inject constructor(
    private val repository: ITodoRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UIStateTodo())
    val uiState = _uiState.asStateFlow()

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

    fun getAllTodos(authToken: String, search: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(todos = TodosUIState.Loading) }
            val result = runCatching { repository.getTodos(authToken, search) }
            _uiState.update { state ->
                val nextState = result.fold(
                    onSuccess = { response: ResponseMessage<ResponseTodos?> ->
                        val todos = response.data?.todos
                        if (response.status == "success" && todos != null) {
                            TodosUIState.Success(todos)
                        } else {
                            TodosUIState.Error(response.message)
                        }
                    },
                    onFailure = { error ->
                        TodosUIState.Error(error.message ?: "Unknown error")
                    }
                )
                state.copy(todos = nextState)
            }
        }
    }

    fun postTodo(
        authToken: String,
        title: String,
        description: String,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(todoAdd = TodoActionUIState.Loading) }
            val result = runCatching {
                repository.postTodo(
                    authToken = authToken,
                    request = RequestTodo(title = title, description = description)
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
        isDone: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(todoChange = TodoActionUIState.Loading) }
            val result = runCatching {
                repository.putTodo(
                    authToken = authToken,
                    todoId = todoId,
                    request = RequestTodo(title = title, description = description, isDone = isDone)
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
