package org.delcom.pam_p5_ifs23021.ui.screens.todos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.delcom.pam_p5_ifs23021.helper.AlertHelper
import org.delcom.pam_p5_ifs23021.helper.AlertState
import org.delcom.pam_p5_ifs23021.helper.AlertType
import org.delcom.pam_p5_ifs23021.helper.ConstHelper
import org.delcom.pam_p5_ifs23021.helper.RouteHelper
import org.delcom.pam_p5_ifs23021.helper.SuspendHelper
import org.delcom.pam_p5_ifs23021.network.todos.data.ResponseTodoData
import org.delcom.pam_p5_ifs23021.network.todos.data.TodoPriority
import org.delcom.pam_p5_ifs23021.ui.components.BottomNavComponent
import org.delcom.pam_p5_ifs23021.ui.components.LoadingUI
import org.delcom.pam_p5_ifs23021.ui.components.TopAppBarComponent
import org.delcom.pam_p5_ifs23021.ui.viewmodels.AuthUIState
import org.delcom.pam_p5_ifs23021.ui.viewmodels.AuthViewModel
import org.delcom.pam_p5_ifs23021.ui.viewmodels.TodoActionUIState
import org.delcom.pam_p5_ifs23021.ui.viewmodels.TodoUIState
import org.delcom.pam_p5_ifs23021.ui.viewmodels.TodoViewModel

@Composable
fun TodosEditScreen(
    navController: NavHostController,
    snackbarHost: SnackbarHostState,
    authViewModel: AuthViewModel,
    todoViewModel: TodoViewModel,
    todoId: String
) {
    val uiStateAuth by authViewModel.uiState.collectAsState()
    val uiStateTodo by todoViewModel.uiState.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    var todoData by remember { mutableStateOf<ResponseTodoData?>(null) }
    var authToken by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (uiStateAuth.auth is AuthUIState.Success) {
            authToken = (uiStateAuth.auth as AuthUIState.Success).data.authToken
            todoViewModel.resetTodoChange()
            todoViewModel.getTodoById(authToken!!, todoId)
        } else {
            RouteHelper.to(navController, ConstHelper.RouteNames.AuthLogin.path, true)
        }
    }

    LaunchedEffect(uiStateTodo.todo) {
        if (uiStateTodo.todo is TodoUIState.Success) {
            todoData = (uiStateTodo.todo as TodoUIState.Success).data
        } else if (uiStateTodo.todo is TodoUIState.Error) {
            RouteHelper.back(navController)
        }
    }

    LaunchedEffect(uiStateTodo.todoChange) {
        when (val state = uiStateTodo.todoChange) {
            is TodoActionUIState.Success -> {
                SuspendHelper.showSnackBar(snackbarHost, SuspendHelper.SnackBarType.SUCCESS, state.message)
                isLoading = false
                RouteHelper.back(navController) // Navigasi aman
            }
            is TodoActionUIState.Error -> {
                SuspendHelper.showSnackBar(snackbarHost, SuspendHelper.SnackBarType.ERROR, state.message)
                isLoading = false
            }
            else -> {}
        }
    }

    if (uiStateTodo.todo is TodoUIState.Loading || isLoading || todoData == null) {
        LoadingUI()
    } else {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            TopAppBarComponent(navController, "Ubah Data", true, false)
            Box(modifier = Modifier.weight(1f)) {
                TodosEditUI(
                    todo = todoData!!,
                    onSave = { title, desc, done, priority ->
                        isLoading = true
                        todoViewModel.putTodo(authToken!!, todoId, title, desc, done, priority)
                    }
                )
            }
            BottomNavComponent(navController)
        }
    }
}

@Composable
fun TodosEditUI(
    todo: ResponseTodoData,
    onSave: (String, String, Boolean, String) -> Unit
) {
    val alertState = remember { mutableStateOf(AlertState()) }
    var title by remember { mutableStateOf(todo.title) }
    var description by remember { mutableStateOf(todo.description) }
    var isDone by remember { mutableStateOf(todo.isDone) }
    var priority by remember { mutableStateOf(todo.priority.uppercase()) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Judul") }, modifier = Modifier.fillMaxWidth()
            )

            Column {
                Text("Status", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isDone, onClick = { isDone = true })
                    Text("Selesai")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = !isDone, onClick = { isDone = false })
                    Text("Belum")
                }
            }

            Column {
                Text("Prioritas", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TodoPriority.allValues().forEach { p ->
                        val isSelected = priority == p.name
                        val color = when(p) {
                            TodoPriority.HIGH -> Color(0xFFE53935)
                            TodoPriority.MEDIUM -> Color(0xFFFB8C00)
                            TodoPriority.LOW -> Color(0xFF43A047)
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { priority = p.name }, // Langsung update state
                            label = { Text(p.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Deskripsi") }, modifier = Modifier.fillMaxWidth().height(150.dp)
            )
        }

        FloatingActionButton(
            onClick = {
                if (title.isBlank() || description.isBlank()) {
                    AlertHelper.show(alertState, AlertType.ERROR, "Judul dan Deskripsi wajib diisi!")
                } else {
                    onSave(title, description, isDone, priority)
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Save, "Simpan")
        }
    }

    if (alertState.value.isVisible) {
        AlertDialog(
            onDismissRequest = { AlertHelper.dismiss(alertState) },
            title = { Text(alertState.value.type.title) },
            text = { Text(alertState.value.message) },
            confirmButton = { TextButton(onClick = { AlertHelper.dismiss(alertState) }) { Text("OK") } }
        )
    }
}
