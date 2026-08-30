package com.shadowvibe.app.ui.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.shadowvibe.app.data.api.ApiClient
import com.shadowvibe.app.data.model.WatchRoomPreview
import com.shadowvibe.app.data.model.WatchRoomState
import com.shadowvibe.app.ui.Screen
import com.shadowvibe.app.ui.theme.PurpleAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchHubScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    var rooms by remember { mutableStateOf<List<WatchRoomPreview>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreate by remember { mutableStateOf(false) }
    var newRoomName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    suspend fun refresh() {
        try {
            val resp = ApiClient.api.getWatchRooms()
            if (resp.isSuccessful) rooms = resp.body() ?: emptyList()
        } catch (_: Exception) {}
        isLoading = false
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Совместный просмотр", color = Color.White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                Column(modifier = Modifier.navigationBarsPadding().padding(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = joinCode,
                            onValueChange = { joinCode = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Код комнаты", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Go),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleAccent,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                cursorColor = PurpleAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    errorMsg = ""
                                    if (joinCode.isBlank()) return@launch
                                    try {
                                        val resp = ApiClient.api.joinWatchRoom(mapOf("roomCode" to joinCode.trim().uppercase()))
                                        if (resp.isSuccessful) {
                                            val room = resp.body()
                                            if (room?.roomId != null) {
                                                navController.navigate(Screen.WatchRoom.createRoute(room.roomId!!, room.roomCode))
                                            }
                                        } else {
                                            errorMsg = "Комната не найдена"
                                        }
                                    } catch (_: Exception) { errorMsg = "Ошибка сети" }
                                }
                            },
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                        ) {
                            Text("Войти", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (errorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PurpleAccent)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Button(
                            onClick = { showCreate = !showCreate },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Создать комнату", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (showCreate) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    OutlinedTextField(
                                        value = newRoomName,
                                        onValueChange = { newRoomName = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Название комнаты", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PurpleAccent,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            cursorColor = PurpleAccent,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    val resp = ApiClient.api.createWatchRoom(mapOf("name" to (newRoomName.ifBlank { "Комната" }), "visibility" to "public"))
                                                    if (resp.isSuccessful) {
                                                        val room = resp.body()
                                                        if (room?.roomId != null) {
                                                            navController.navigate(Screen.WatchRoom.createRoute(room.roomId!!, room.roomCode))
                                                        }
                                                    }
                                                } catch (_: Exception) {}
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(46.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Создать", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            "Открытые комнаты",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    if (rooms.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                Text("Пока нет открытых комнат", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            }
                        }
                    }
                    items(rooms) { room ->
                        WatchRoomCard(room) {
                            scope.launch {
                                try {
                                    val resp = ApiClient.api.joinWatchRoom(mapOf("roomCode" to room.roomCode))
                                    if (resp.isSuccessful) {
                                        resp.body()?.roomId?.let { id ->
                                            navController.navigate(Screen.WatchRoom.createRoute(id, room.roomCode))
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchRoomCard(room: WatchRoomPreview, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(PurpleAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(room.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("${room.hostUsername} • ${room.memberCount} уч.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, maxLines = 1)
                if (room.videoUrl != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Сейчас: ${room.status}", color = if (room.status == "PLAYING") Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Text(
                room.roomCode,
                color = PurpleAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(PurpleAccent.copy(alpha = 0.1f)).padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
