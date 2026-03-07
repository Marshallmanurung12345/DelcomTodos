package org.delcom.pam_p5_ifs23021.ui.screens.todos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import org.delcom.pam_p5_ifs23021.R
import org.delcom.pam_p5_ifs23021.helper.ConstHelper
import org.delcom.pam_p5_ifs23021.helper.RouteHelper
import org.delcom.pam_p5_ifs23021.helper.ToolsHelper
import org.delcom.pam_p5_ifs23021.network.todos.data.ResponseTodoData
import org.delcom.pam_p5_ifs23021.ui.components.BottomNavComponent
import org.delcom.pam_p5_ifs23021.ui.components.LoadingUI
import org.delcom.pam_p5_ifs23021.ui.components.TopAppBarComponent
import org.delcom.pam_p5_ifs23021.ui.components.TopAppBarMenuItem
import org.delcom.pam_p5_ifs23021.ui.viewmodels.AuthLogoutUIState
import org.delcom.pam_p5_ifs23021.ui.viewmodels.AuthUIState
import org.delcom.pam_p5_ifs23021.ui.viewmodels.AuthViewModel
import org.delcom.pam_p5_ifs23021.ui.viewmodels.TodoViewModel
import org.delcom.pam_p5_ifs23021.ui.viewmodels.TodosUIState

enum class TodoStatusFilter(val label: String) {
    ALL("Semua"), DONE("Selesai"), NOT_DONE("Belum")
}

enum class TodoPriorityFilter(val label: String, val value: String?) {
    ALL("Semua", null), HIGH("High", "HIGH"), MEDIUM("Medium", "MEDIUM"), LOW("Low", "LOW")
}

@Composable
fun TodosScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    todoViewModel: TodoViewModel
) {
    val uiStateAuth by authViewModel.uiState.collectAsState()
    val uiStateTodo by todoViewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var statusFilter by remember { mutableStateOf(TodoStatusFilter.ALL) }
    var priorityFilter by remember { mutableStateOf(TodoPriorityFilter.ALL) }
    var authToken by remember { mutableStateOf<String?>(null) }

    fun isDoneParam() = when (statusFilter) {
        TodoStatusFilter.DONE -> true
        TodoStatusFilter.NOT_DONE -> false
        else -> null
    }

    // Fungsi fetch data
    val fetchTodos = { token: String ->
        todoViewModel.getAllTodos(
            authToken = token,
            search = searchQuery.text.ifEmpty { null },
            isDone = isDoneParam(),
            priority = priorityFilter.value
        )
    }

    LaunchedEffect(Unit) {
        if (uiStateAuth.auth is AuthUIState.Success) {
            val token = (uiStateAuth.auth as AuthUIState.Success).data.authToken
            authToken = token
            fetchTodos(token)
        } else {
            RouteHelper.to(navController, ConstHelper.RouteNames.AuthLogin.path, true)
        }
    }

    // Filter berubah → Refresh data
    LaunchedEffect(statusFilter, priorityFilter) {
        authToken?.let { fetchTodos(it) }
    }

    val menuItems = listOf(
        TopAppBarMenuItem("Profile", Icons.Filled.Person, ConstHelper.RouteNames.Profile.path),
        TopAppBarMenuItem("Logout", Icons.AutoMirrored.Filled.Logout, null, onClick = {
            authToken?.let { authViewModel.logout(it) }
        })
    )

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBarComponent(
            navController = navController,
            title = "Todos",
            showBackButton = false,
            customMenuItems = menuItems,
            withSearch = true,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onSearchAction = { authToken?.let { fetchTodos(it) } }
        )

        FilterBar(
            statusFilter = statusFilter,
            priorityFilter = priorityFilter,
            onStatusFilterChange = { statusFilter = it },
            onPriorityFilterChange = { priorityFilter = it }
        )

        Box(modifier = Modifier.weight(1f)) {
            when (val state = uiStateTodo.todos) {
                is TodosUIState.Loading -> LoadingUI()
                is TodosUIState.Error -> ErrorMessage(state.message)
                is TodosUIState.Success -> {
                    TodosListUI(
                        todos = state.data,
                        isLoadingMore = uiStateTodo.isLoadingMore,
                        hasMoreData = uiStateTodo.hasMoreData,
                        onLoadMore = {
                            authToken?.let {
                                todoViewModel.loadMoreTodos(it, searchQuery.text.ifEmpty { null }, isDoneParam(), priorityFilter.value)
                            }
                        },
                        onItemClick = { id -> RouteHelper.to(navController, "todos/$id") }
                    )
                }
            }

            FloatingActionButton(
                onClick = { RouteHelper.to(navController, ConstHelper.RouteNames.TodosAdd.path) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Tambah")
            }
        }
        BottomNavComponent(navController = navController)
    }
}

@Composable
fun TodosListUI(
    todos: List<ResponseTodoData>,
    isLoadingMore: Boolean,
    hasMoreData: Boolean,
    onLoadMore: () -> Unit,
    onItemClick: (String) -> Unit
) {
    val listState = rememberLazyListState()

    // Cek scroll mendekati akhir
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 2
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && hasMoreData && !isLoadingMore) {
            onLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 80.dp)
    ) {
        items(todos, key = { it.id }) { todo ->
            TodoItemUI(todo, onItemClick)
        }

        if (isLoadingMore) {
            item {
                Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
fun ErrorMessage(message: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
    }
}

@Composable
fun FilterBar(
    statusFilter: TodoStatusFilter,
    priorityFilter: TodoPriorityFilter,
    onStatusFilterChange: (TodoStatusFilter) -> Unit,
    onPriorityFilterChange: (TodoPriorityFilter) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Status:", style = MaterialTheme.typography.labelMedium)
            TodoStatusFilter.entries.forEach { filter ->
                FilterChip(
                    selected = statusFilter == filter,
                    onClick = { onStatusFilterChange(filter) },
                    label = { Text(filter.label) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Prioritas:", style = MaterialTheme.typography.labelMedium)
            TodoPriorityFilter.entries.forEach { filter ->
                FilterChip(
                    selected = priorityFilter == filter,
                    onClick = { onPriorityFilterChange(filter) },
                    label = { Text(filter.label) }
                )
            }
        }
    }
}

@Composable
fun TodoItemUI(todo: ResponseTodoData, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick(todo.id) },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ToolsHelper.getTodoImage(todo.id, todo.updatedAt),
                contentDescription = null,
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.img_placeholder)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(todo.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(todo.description, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(todo.isDone)
                    PriorityBadge(todo.priority)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(isDone: Boolean) {
    val color = if (isDone) Color(0xFF43A047) else Color(0xFFFB8C00)
    Surface(color = color.copy(0.1f), shape = RoundedCornerShape(4.dp)) {
        Text(
            if (isDone) "Selesai" else "Belum",
            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
fun PriorityBadge(priority: String?) {
    val color = when (priority?.uppercase()) {
        "HIGH" -> Color(0xFFE53935)
        "MEDIUM" -> Color(0xFFFB8C00)
        else -> Color(0xFF43A047)
    }
    Surface(color = color.copy(0.1f), shape = RoundedCornerShape(4.dp)) {
        Text(
            priority ?: "LOW",
            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
