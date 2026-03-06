package org.delcom.pam_p5_ifs23021.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import org.delcom.pam_p5_ifs23021.R
import org.delcom.pam_p5_ifs23021.helper.ConstHelper
import org.delcom.pam_p5_ifs23021.helper.RouteHelper
import org.delcom.pam_p5_ifs23021.helper.SuspendHelper
import org.delcom.pam_p5_ifs23021.helper.ToolsHelper
import org.delcom.pam_p5_ifs23021.helper.ToolsHelper.uriToMultipart
import org.delcom.pam_p5_ifs23021.network.todos.data.ResponseUserData
import org.delcom.pam_p5_ifs23021.ui.components.BottomNavComponent
import org.delcom.pam_p5_ifs23021.ui.components.LoadingUI
import org.delcom.pam_p5_ifs23021.ui.components.TopAppBarComponent
import org.delcom.pam_p5_ifs23021.ui.components.TopAppBarMenuItem
import org.delcom.pam_p5_ifs23021.ui.viewmodels.AuthLogoutUIState
import org.delcom.pam_p5_ifs23021.ui.viewmodels.AuthUIState
import org.delcom.pam_p5_ifs23021.ui.viewmodels.AuthViewModel
import org.delcom.pam_p5_ifs23021.ui.viewmodels.ProfileUIState
import org.delcom.pam_p5_ifs23021.ui.viewmodels.TodoActionUIState
import org.delcom.pam_p5_ifs23021.ui.viewmodels.TodoViewModel

@Composable
fun ProfileScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    todoViewModel: TodoViewModel,
    snackbarHost: SnackbarHostState? = null
) {
    val uiStateAuth by authViewModel.uiState.collectAsState()
    val uiStateTodo by todoViewModel.uiState.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf<ResponseUserData?>(null) }
    var authToken by remember { mutableStateOf<String?>(null) }
    var photoTimestamp by remember { mutableStateOf(System.currentTimeMillis().toString()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        isLoading = true
        if (uiStateAuth.auth !is AuthUIState.Success) {
            RouteHelper.to(navController, ConstHelper.RouteNames.Home.path, true)
            return@LaunchedEffect
        }
        authToken = (uiStateAuth.auth as AuthUIState.Success).data.authToken
        if (uiStateTodo.profile is ProfileUIState.Success) {
            profile = (uiStateTodo.profile as ProfileUIState.Success).data
            isLoading = false
            return@LaunchedEffect
        }
        todoViewModel.getProfile(authToken ?: "")
    }

    LaunchedEffect(uiStateTodo.profile) {
        if (uiStateTodo.profile !is ProfileUIState.Loading) {
            isLoading = false
            if (uiStateTodo.profile is ProfileUIState.Success)
                profile = (uiStateTodo.profile as ProfileUIState.Success).data
            else
                RouteHelper.to(navController, ConstHelper.RouteNames.Home.path, true)
        }
    }

    LaunchedEffect(uiStateTodo.profileChange) {
        when (val state = uiStateTodo.profileChange) {
            is TodoActionUIState.Success -> {
                snackbarHost?.let { SuspendHelper.showSnackBar(it, SuspendHelper.SnackBarType.SUCCESS, state.message) }
                todoViewModel.resetProfileChange()
                todoViewModel.getProfile(authToken ?: "")
            }
            is TodoActionUIState.Error -> {
                snackbarHost?.let { SuspendHelper.showSnackBar(it, SuspendHelper.SnackBarType.ERROR, state.message) }
                todoViewModel.resetProfileChange()
            }
            else -> {}
        }
    }

    LaunchedEffect(uiStateTodo.profileChangePassword) {
        when (val state = uiStateTodo.profileChangePassword) {
            is TodoActionUIState.Success -> {
                snackbarHost?.let { SuspendHelper.showSnackBar(it, SuspendHelper.SnackBarType.SUCCESS, state.message) }
                todoViewModel.resetProfileChangePassword()
            }
            is TodoActionUIState.Error -> {
                snackbarHost?.let { SuspendHelper.showSnackBar(it, SuspendHelper.SnackBarType.ERROR, state.message) }
                todoViewModel.resetProfileChangePassword()
            }
            else -> {}
        }
    }

    LaunchedEffect(uiStateTodo.profileChangePhoto) {
        when (val state = uiStateTodo.profileChangePhoto) {
            is TodoActionUIState.Success -> {
                photoTimestamp = System.currentTimeMillis().toString()
                snackbarHost?.let { SuspendHelper.showSnackBar(it, SuspendHelper.SnackBarType.SUCCESS, state.message) }
                todoViewModel.resetProfileChangePhoto()
                todoViewModel.getProfile(authToken ?: "")
            }
            is TodoActionUIState.Error -> {
                snackbarHost?.let { SuspendHelper.showSnackBar(it, SuspendHelper.SnackBarType.ERROR, state.message) }
                todoViewModel.resetProfileChangePhoto()
            }
            else -> {}
        }
    }

    LaunchedEffect(uiStateAuth.authLogout) {
        if (uiStateAuth.authLogout !is AuthLogoutUIState.Loading)
            RouteHelper.to(navController, ConstHelper.RouteNames.AuthLogin.path, true)
    }

    if (isLoading || profile == null) { LoadingUI(); return }

    val menuItems = listOf(
        TopAppBarMenuItem(text = "Profile", icon = Icons.Filled.Person, route = ConstHelper.RouteNames.Profile.path),
        TopAppBarMenuItem(text = "Logout", icon = Icons.AutoMirrored.Filled.Logout, route = null,
            onClick = { authViewModel.logout(authToken ?: "") })
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBarComponent(
            navController = navController,
            title = "Profile",
            showBackButton = false,
            customMenuItems = menuItems
        )
        Box(modifier = Modifier.weight(1f)) {
            ProfileUI(
                profile = profile!!,
                photoTimestamp = photoTimestamp,
                onChangePhoto = { uri ->
                    val filePart = uriToMultipart(context, uri, "file")
                    todoViewModel.updateProfilePhoto(authToken ?: "", filePart)
                },
                onSaveProfile = { name, username, about ->
                    todoViewModel.updateProfile(authToken ?: "", name, username, about)
                },
                onChangePassword = { currentPw, newPw ->
                    todoViewModel.changePassword(authToken ?: "", currentPw, newPw)
                }
            )
        }
        BottomNavComponent(navController = navController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileUI(
    profile: ResponseUserData,
    photoTimestamp: String = "0",
    onChangePhoto: (Uri) -> Unit = {},
    onSaveProfile: (name: String, username: String, about: String) -> Unit = { _, _, _ -> },
    onChangePassword: (currentPw: String, newPw: String) -> Unit = { _, _ -> }
) {
    var showEditSheet by remember { mutableStateOf(false) }
    var showPasswordSheet by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { onChangePhoto(it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // === Header foto ===
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = ToolsHelper.getUserImage(profile.id, photoTimestamp),
                    contentDescription = "Photo Profil",
                    placeholder = painterResource(R.drawable.img_placeholder),
                    error = painterResource(R.drawable.img_placeholder),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Filled.CameraAlt, contentDescription = "Ubah foto",
                        tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Ketuk foto untuk mengubah", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(14.dp))
            Text(profile.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("@${profile.username}", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (profile.about.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = profile.about,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // === Action cards ===
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showEditSheet = true },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text("Ubah Informasi Akun", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Text("Nama, username, dan tentang", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().clickable { showPasswordSheet = true },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text("Ubah Kata Sandi", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Text("Perbarui kata sandi akun Anda", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            EditProfileSheetContent(
                profile = profile,
                onSave = { name, username, about -> onSaveProfile(name, username, about); showEditSheet = false },
                onDismiss = { showEditSheet = false }
            )
        }
    }

    if (showPasswordSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPasswordSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            ChangePasswordSheetContent(
                onSave = { currentPw, newPw -> onChangePassword(currentPw, newPw); showPasswordSheet = false },
                onDismiss = { showPasswordSheet = false }
            )
        }
    }
}

@Composable
fun EditProfileSheetContent(
    profile: ResponseUserData,
    onSave: (name: String, username: String, about: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var username by remember { mutableStateOf(profile.username) }
    var about by remember { mutableStateOf(profile.about) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Ubah Informasi Akun", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        HorizontalDivider()

        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Nama") }, modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        OutlinedTextField(
            value = username, onValueChange = { username = it },
            label = { Text("Username") }, modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        OutlinedTextField(
            value = about, onValueChange = { about = it },
            label = { Text("Tentang") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            maxLines = 4, shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Batal") }
            Button(
                onClick = { onSave(name, username, about) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                enabled = name.isNotEmpty() && username.isNotEmpty()
            ) {
                Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Simpan")
            }
        }
    }
}

@Composable
fun ChangePasswordSheetContent(
    onSave: (currentPw: String, newPw: String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Ubah Kata Sandi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        HorizontalDivider()

        OutlinedTextField(
            value = currentPassword, onValueChange = { currentPassword = it },
            label = { Text("Kata Sandi Saat Ini") }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, shape = RoundedCornerShape(12.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
        )
        OutlinedTextField(
            value = newPassword, onValueChange = { newPassword = it },
            label = { Text("Kata Sandi Baru") }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, shape = RoundedCornerShape(12.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
        )
        OutlinedTextField(
            value = confirmPassword, onValueChange = { confirmPassword = it },
            label = { Text("Konfirmasi Kata Sandi Baru") }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, shape = RoundedCornerShape(12.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
        )

        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Batal") }
            Button(
                onClick = {
                    when {
                        currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() ->
                            errorMessage = "Semua field wajib diisi"
                        newPassword != confirmPassword ->
                            errorMessage = "Kata sandi baru tidak cocok"
                        newPassword.length < 6 ->
                            errorMessage = "Kata sandi minimal 6 karakter"
                        else -> { errorMessage = ""; onSave(currentPassword, newPassword) }
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Simpan")
            }
        }
    }
}