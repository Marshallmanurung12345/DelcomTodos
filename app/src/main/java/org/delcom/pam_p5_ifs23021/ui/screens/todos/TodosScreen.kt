package org.delcom.pam_p5_ifs23021.ui.screens.todos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var statusFilter by remember { mutableStateOf(TodoStatusFilter.ALL) }
    var priorityFilter by remember { mutableStateOf(TodoPriorityFilter.ALL) }
    var todos by remember { mutableStateOf<List<ResponseTodoData>>(emptyList()) }
    var authToken by remember { mutableStateOf<String?>(null) }

    fun isDoneParam() = when (statusFilter) {
        TodoStatusFilter.DONE -> true
        TodoStatusFilter.NOT_DONE -> false
        else -> null
    }

    fun fetchTodos(token: String) {
        todoViewModel.getAllTodos(
            authToken = token,
            search = searchQuery.text.ifEmpty { null },
            isDone = isDoneParam(),
            priority = priorityFilter.value
        )
    }

    fun loadMore() {
        todoViewModel.loadMoreTodos(
            authToken = authToken ?: return,
            search = searchQuery.text.ifEmpty { null },
            isDone = isDoneParam(),
            priority = priorityFilter.value
        )
    }

    // Step 1: Inisialisasi token
    LaunchedEffect(Unit) {
        if (uiStateAuth.auth !is AuthUIState.Success) {
            RouteHelper.to(navController, ConstHelper.RouteNames.Home.path, true)
            return@LaunchedEffect
        }
        val token = (uiStateAuth.auth as AuthUIState.Success).data.authToken
        authToken = token
        fetchTodos(token)
    }

    // Step 2: Filter berubah → fetch ulang (hanya jika token sudah ada)
    LaunchedEffect(statusFilter, priorityFilter) {
        val token = authToken ?: return@LaunchedEffect
        isLoading = true
        fetchTodos(token)
    }

    // Step 3: Update list dari state
    LaunchedEffect(uiStateTodo.todos) {
        when (val state = uiStateTodo.todos) {
            is TodosUIState.Success -> {
                todos = state.data
                isLoading = false
            }
            is TodosUIState.Error -> {
                todos = emptyList()
                isLoading = false
            }
            is TodosUIState.Loading -> { /* tunggu */ }
        }
    }

    // Step 4: Handle logout
    LaunchedEffect(uiStateAuth.authLogout) {
        if (uiStateAuth.authLogout !is AuthLogoutUIState.Loading) {
            RouteHelper.to(navController, ConstHelper.RouteNames.AuthLogin.path, true)
        }
    }

    if (isLoading) {
        LoadingUI()
        return
    }

    val menuItems = listOf(
        TopAppBarMenuItem(
            text = "Profile", icon = Icons.Filled.Person,
            route = ConstHelper.RouteNames.Profile.path
        ),
        TopAppBarMenuItem(
            text = "Logout", icon = Icons.AutoMirrored.Filled.Logout,
            route = null,
            onClick = { authViewModel.logout(authToken ?: "") }
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBarComponent(
            navController = navController,
            title = "Todos",
            showBackButton = false,
            customMenuItems = menuItems,
            withSearch = true,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onSearchAction = {
                val token = authToken ?: return@TopAppBarComponent
                isLoading = true
                fetchTodos(token)
            }
        )
        Box(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.fillMaxSize()) {
                FilterBar(
                    statusFilter = statusFilter,
                    priorityFilter = priorityFilter,
                    onStatusFilterChange = { statusFilter = it },
                    onPriorityFilterChange = { priorityFilter = it }
                )
                TodosUI(
                    todos = todos,
                    onOpen = { id -> RouteHelper.to(navController, "todos/$id") },
                    isLoadingMore = uiStateTodo.isLoadingMore,
                    hasMoreData = uiStateTodo.hasMoreData,
                    onLoadMore = ::loadMore
                )
            }
            FloatingActionButton(
                onClick = { RouteHelper.to(navController, ConstHelper.RouteNames.TodosAdd.path) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Todo")
            }
        }
        BottomNavComponent(navController = navController)
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Status:", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TodoStatusFilter.entries.forEach { filter ->
                FilterChip(
                    selected = statusFilter == filter,
                    onClick = { onStatusFilterChange(filter) },
                    label = { Text(filter.label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Prioritas:", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TodoPriorityFilter.entries.forEach { filter ->
                val chipColor = when (filter) {
                    TodoPriorityFilter.HIGH -> Color(0xFFE53935)
                    TodoPriorityFilter.MEDIUM -> Color(0xFFFB8C00)
                    TodoPriorityFilter.LOW -> Color(0xFF43A047)
                    else -> MaterialTheme.colorScheme.primary
                }
                FilterChip(
                    selected = priorityFilter == filter,
                    onClick = { onPriorityFilterChange(filter) },
                    label = { Text(filter.label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun TodosUI(
    todos: List<ResponseTodoData>,
    onOpen: (String) -> Unit,
    isLoadingMore: Boolean = false,
    hasMoreData: Boolean = false,
    onLoadMore: () -> Unit = {}
) {
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - 3 && total > 0
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && hasMoreData && !isLoadingMore) onLoadMore()
    }

    if (todos.isEmpty() && !isLoadingMore) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Text(
                text = "Tidak ada data!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentPadding = PaddingValues(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 80.dp)
    ) {
        items(todos, key = { it.id }) { todo -> TodoItemUI(todo, onOpen) }

        if (isLoadingMore) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
        }

        if (!hasMoreData && todos.isNotEmpty()) {
            item {
                Text(
                    text = "Semua data telah dimuat",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: String?) {
    val safePriority = priority?.uppercase() ?: "LOW"
    val (label, bgColor, textColor) = when (safePriority) {
        "HIGH" -> Triple("🔴 High", Color(0xFFFFEBEE), Color(0xFFB71C1C))
        "MEDIUM" -> Triple("🟠 Medium", Color(0xFFFFF3E0), Color(0xFFE65100))
        else -> Triple("🟢 Low", Color(0xFFE8F5E9), Color(0xFF1B5E20))
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold, color = textColor)
    }
}

@Composable
fun TodoItemUI(todo: ResponseTodoData, onOpen: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onOpen(todo.id) },
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            AsyncImage(
                model = ToolsHelper.getTodoImage(todo.id, todo.updatedAt),
                contentDescription = todo.title,
                placeholder = painterResource(R.drawable.img_placeholder),
                error = painterResource(R.drawable.img_placeholder),
                modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = todo.title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = todo.description, style = MaterialTheme.typography.bodySmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (todo.isDone) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.tertiaryContainer
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (todo.isDone) "Selesai" else "Belum",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (todo.isDone) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    PriorityBadge(priority = todo.priority)
                }
            }
        }
    }
}