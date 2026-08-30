package com.shadowvibe.app.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.shadowvibe.app.data.api.ApiClient
import com.shadowvibe.app.data.model.UserMap
import com.shadowvibe.app.ui.Screen
import com.shadowvibe.app.ui.theme.PurpleAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<UserMap?>(null) }
    var about by remember { mutableStateOf("") }
    var selectedAvatar: Uri? by remember { mutableStateOf(null) }
    var avatarFile: File? by remember { mutableStateOf(null) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedAvatar = uri
            avatarFile = uri.toFile(context)
        }
    }

    LaunchedEffect(Unit) {
        try {
            val resp = ApiClient.api.getMe()
            val me = resp.body()
            if (me != null) {
                user = me
                about = me.about ?: ""
            }
        } catch (_: Exception) {}
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Редактировать профиль", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.BottomEnd
            ) {
                if (selectedAvatar != null) {
                    AsyncImage(
                        model = selectedAvatar,
                        contentDescription = null,
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(PurpleAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user?.username?.firstOrNull()?.uppercase() ?: "?",
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { avatarPicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Avatаr", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = user?.username ?: "",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "О себе",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = about,
                onValueChange = { about = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Расскажите о себе...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                minLines = 4,
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    cursorColor = PurpleAccent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        saving = true
                        message = ""
                        try {
                            val aboutBody = about.trim().toRequestBody(
                                "text/plain".toMediaTypeOrNull()
                            )
                            var avatarPart: MultipartBody.Part? = null
                            if (avatarFile != null) {
                                val file = avatarFile!!
                                val reqBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                                avatarPart = MultipartBody.Part.createFormData("avatar", file.name, reqBody)
                            }
                            val resp = ApiClient.api.updateProfile(aboutBody, avatarPart)
                            if (resp.isSuccessful) {
                                isError = false
                                message = "Профиль обновлён"
                                delay(800)
                                navController.popBackStack()
                            } else {
                                val body = resp.errorBody()?.string()
                                isError = true
                                message = "Не удалось сохранить изменения"
                            }
                        } catch (e: Exception) {
                            isError = true
                            message = "Ошибка сети"
                        }
                        saving = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !saving && user != null,
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Сохранить", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun Uri.toFile(context: android.content.Context): File {
    val cachePath = context.cacheDir
    val name = "avatar_${System.currentTimeMillis()}.jpg"
    val file = File(cachePath, name)
    try {
        context.contentResolver.openInputStream(this)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
    } catch (_: Exception) {}
    return file
}
