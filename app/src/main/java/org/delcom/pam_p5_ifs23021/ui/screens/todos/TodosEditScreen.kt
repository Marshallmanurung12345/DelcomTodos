package org.delcom.pam_p5_ifs23021.ui.screens.todos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    // isLoadingData = true hanya saat fetch data awal
    var isLoadingData by remember { mutableStateOf(true) }
    var todo by remember { mutableStateOf<ResponseTodoData?>(null) }
    var authToken by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (uiStateAuth.auth !is AuthUIState.Success) {
            RouteHelper.to(navController, ConstHelper.RouteNames.Home.path, true)
            return@LaunchedEffect
        }
        authToken = (uiStateAuth.auth as AuthUIState.Success).data.authToken
        todoViewModel.resetTodoChange()

        // Kalau data sudah ada dari DetailScreen, langsung pakai
        val current = uiStateTodo.todo
        if (current is TodoUIState.Success) {
            todo = current.data
            isLoadingData = false
        } else {
            todoViewModel.getTodoById(authToken!!, todoId)
        }
    }

    LaunchedEffect(uiStateTodo.todo) {
        when (val state = uiStateTodo.todo) {
            is TodoUIState.Success -> {
                todo = state.data
                isLoadingData = false
            }
            is TodoUIState.Error -> {
                if (isLoadingData) {
                    isLoadingData = false
                    RouteHelper.back(navController)
                }
            }
            else -> {}
        }
    }

    // Handle hasil simpan — TIDAK pakai isLoadingData agar LaunchedEffect ini tetap aktif
    LaunchedEffect(uiStateTodo.todoChange) {
        when (val state = uiStateTodo.todoChange) {
            is TodoActionUIState.Success -> {
                SuspendHelper.showSnackBar(snackbarHost, SuspendHelper.SnackBarType.SUCCESS, state.message)
                RouteHelper.to(
                    navController = navController,
                    destination = ConstHelper.RouteNames.TodosDetail.path.replace("{todoId}", todoId),
                    popUpTo = ConstHelper.RouteNames.TodosDetail.path.replace("{todoId}", todoId),
                    removeBackStack = true
                )
            }
            is TodoActionUIState.Error -> {
                SuspendHelper.showSnackBar(snackbarHost, SuspendHelper.SnackBarType.ERROR, state.message)
            }
            else -> {}
        }
    }

    // Hanya show LoadingUI saat data belum ada
    if (isLoadingData || todo == null) {
        LoadingUI()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBarComponent(
            navController = navController,
            title = "Ubah Data",
            showBackButton = true,
            showMenu = false
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            TodosEditUI(
                todo = todo!!,
                isSaving = uiStateTodo.todoChange is TodoActionUIState.Loading &&
                        uiStateTodo.todoChange != TodoActionUIState.Loading, // false by default
                onSave = { title, description, isDone, priority ->
                    todoViewModel.putTodo(
                        authToken = authToken!!,
                        todoId = todoId,
                        title = title,
                        description = description,
                        isDone = isDone,
                        priority = priority
                    )
                }
            )
        }
        BottomNavComponent(navController = navController)
    }
}

@Composable
fun TodosEditUI(
    todo: ResponseTodoData,
    isSaving: Boolean = false,
    onSave: (String, String, Boolean, String) -> Unit
) {
    val alertState = remember { mutableStateOf(AlertState()) }
    var dataTitle by remember { mutableStateOf(todo.title) }
    var dataDescription by remember { mutableStateOf(todo.description) }
    var dataIsDone by remember { mutableStateOf(todo.isDone) }
    // Uppercase agar cocok dengan enum name (API bisa kirim "low" atau "LOW")
    var dataPriority by remember { mutableStateOf(todo.priority.uppercase().ifEmpty { TodoPriority.LOW.name }) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = dataTitle,
                onValueChange = { dataTitle = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next)
            )

            // Status
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Status", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = dataIsDone, onClick = { dataIsDone = true })
                    Text("Selesai")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = !dataIsDone, onClick = { dataIsDone = false })
                    Text("Belum")
                }
            }

            // Prioritas
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Prioritas", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TodoPriority.values().forEach { priority ->
                        val color = priorityColor(priority.name)
                        FilterChip(
                            selected = dataPriority == priority.name,
                            onClick = { dataPriority = priority.name },
                            label = { Text(priority.label, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Deskripsi
            OutlinedTextField(
                value = dataDescription,
                onValueChange = { dataDescription = it },
                label = { Text("Deskripsi") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5, minLines = 3,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done)
            )
        }

        // FAB Simpan
        FloatingActionButton(
            onClick = {
                if (isSaving) return@FloatingActionButton
                if (dataTitle.isEmpty()) {
                    AlertHelper.show(alertState, AlertType.ERROR, "Judul tidak boleh kosong!")
                    return@FloatingActionButton
                }
                if (dataDescription.isEmpty()) {
                    AlertHelper.show(alertState, AlertType.ERROR, "Deskripsi tidak boleh kosong!")
                    return@FloatingActionButton
                }
                onSave(dataTitle, dataDescription, dataIsDone, dataPriority)
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = if (isSaving) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp), strokeWidth = 2.dp)
            } else {
                Icon(imageVector = Icons.Default.Save, contentDescription = "Simpan")
            }
        }
    }

    if (alertState.value.isVisible) {
        AlertDialog(
            onDismissRequest = { AlertHelper.dismiss(alertState) },
            title = { Text(alertState.value.type.title) },
            text = { Text(alertState.value.message) },
            confirmButton = {
                TextButton(onClick = { AlertHelper.dismiss(alertState) }) { Text("OK") }
            }
        )
    }
}