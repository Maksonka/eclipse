package com.shadowvibe.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.shadowvibe.app.data.api.ApiClient
import com.shadowvibe.app.data.model.ChatMessage
import com.shadowvibe.app.data.ws.StompClient
import com.shadowvibe.app.ui.Screen
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private val DarkBackground = Color(0xFF0E0E10)
private val SurfaceColor = Color(0xFF1E1E22)
private val PurpleAccent = Color(0xFF8B5CF6)
private val TextGray = Color(0xFFA1A1AA)
private val OnlineGreen = Color(0xFF22C55E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavHostController, username: String) {
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableStateOf(0) }
    var isTyping by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }
    val stomp = remember { StompClient(ApiClient.httpClient) }

    LaunchedEffect(username, retryKey) {
        try {
            withTimeoutOrNull(15000L) {
                val meResponse = ApiClient.api.getMe()
                if (!meResponse.isSuccessful) {
                    loadError = "Ошибка авторизации ($username)"
                    return@withTimeoutOrNull
                }
                currentUser = meResponse.body()?.username ?: ""
                val response = ApiClient.api.getConversationMessages(username)
                if (!response.isSuccessful) {
                    loadError = "Не удалось загрузить переписку (код ${response.code()})"
                    return@withTimeoutOrNull
                }
                messages = response.body()?.messages ?: emptyList()
                if (messages.isNotEmpty()) {
                    listState.scrollToItem(messages.size - 1)
                }
                stomp.connect(ApiClient.getCookieString(), onConnected = {
                    stomp.subscribe("/user/queue/messages") { payload ->
                        try {
                            val msg = gson.fromJson(payload, ChatMessage::class.java)
                            val partner = if (msg.receiverUsername == currentUser) msg.senderUsername else msg.receiverUsername
                            if (partner == username && msg.senderUsername.isNotBlank()) {
                                messages = messages + msg
                                val target = messages.size - 1
                                scope.launch { listState.animateScrollToItem(target) }
                            }
                        } catch (_: Exception) {}
                    }
                    stomp.subscribe("/user/queue/typing") { payload ->
                        try {
                            val obj = gson.fromJson(payload, com.google.gson.JsonObject::class.java)
                            val typingUser = obj.get("username")?.asString ?: obj.get("senderUsername")?.asString ?: ""
                            val typing = obj.get("typing")?.asBoolean ?: false
                            isTyping = typing && typingUser == username
                        } catch (_: Exception) {}
                    }
                    stomp.send("/app/chat.read", gson.toJson(mapOf("partnerUsername" to username)))
                }, onError = {})
            } ?: run {
                loadError = "Таймаут загрузки. Проверьте подключение к серверу"
            }
        } catch (_: Exception) {
            loadError = "Не удалось загрузить переписку"
        } finally {
            isLoading = false
        }
    }

    DisposableEffect(Unit) {
        onDispose { stomp.disconnect() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(SurfaceColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(username.firstOrNull()?.uppercase() ?: "?", color = PurpleAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(username, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(OnlineGreen))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PurpleAccent)
                }
            } else if (loadError != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = loadError!!, color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 24.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { loadError = null; isLoading = true; retryKey++ }, colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)) {
                            Text("Повторить", color = Color.White)
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (isTyping) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(4.dp), contentAlignment = Alignment.CenterStart) {
                                Surface(color = SurfaceColor, shape = RoundedCornerShape(16.dp)) {
                                    Text("печатает...", color = TextGray, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    items(messages) { msg ->
                        val isMine = msg.senderUsername == currentUser
                        ChatBubble(msg, isMine, username)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Surface(color = SurfaceColor, tonalElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = TextGray)
                        }
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Сообщение...", color = TextGray) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (inputText.isNotBlank()) {
                                    val content = inputText.trim()
                                    inputText = ""
                                    stomp.send("/app/chat.send", gson.toJson(mapOf("receiverUsername" to username, "content" to content)))
                                }
                            }),
                            singleLine = false,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleAccent,
                                unfocusedBorderColor = Color(0xFF2E2E32),
                                focusedContainerColor = DarkBackground,
                                unfocusedContainerColor = DarkBackground,
                                cursorColor = PurpleAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = {
                            if (inputText.isNotBlank()) {
                                val content = inputText.trim()
                                inputText = ""
                                stomp.send("/app/chat.send", gson.toJson(mapOf("receiverUsername" to username, "content" to content)))
                            }
                        }, enabled = inputText.isNotBlank()) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (inputText.isNotBlank()) PurpleAccent else TextGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, isSent: Boolean, partnerUsername: String) {
    val alignment = if (isSent) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isSent) PurpleAccent else SurfaceColor
    val shape = if (isSent) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    val textColor = if (isSent) Color.White else Color(0xFFE4E4E7)

    if (message.deleted) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), contentAlignment = alignment) {
            Text("Удалено", color = TextGray, fontSize = 13.sp)
        }
        return
    }

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), contentAlignment = alignment) {
        Column(modifier = Modifier.widthIn(max = 280.dp)) {
            Surface(color = bubbleColor, shape = shape) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    when (message.attachmentType) {
                        "image" -> {
                            AsyncImage(
                                model = "http://192.168.0.61:1010/uploads/messages/${message.attachmentFilename}",
                                contentDescription = "Image",
                                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            if (!message.content.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = message.content, color = textColor, fontSize = 14.sp)
                            }
                        }
                        "audio", "video" -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = textColor, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = message.content ?: message.attachmentType?.uppercase() ?: "Media", color = textColor, fontSize = 14.sp)
                            }
                        }
                        "file" -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.InsertDriveFile, contentDescription = "File", tint = textColor, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = message.attachmentOriginalName ?: message.content ?: "Файл", color = textColor, fontSize = 14.sp)
                            }
                        }
                        else -> {
                            Text(text = message.content ?: "", color = textColor, fontSize = 14.sp)
                        }
                    }
                }
            }
            Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = message.timestamp.takeLast(5), color = TextGray, fontSize = 11.sp)
                if (isSent) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (message.read) Icons.Default.DoneAll else Icons.Default.Done,
                        contentDescription = if (message.read) "Read" else "Sent",
                        tint = if (message.read) PurpleAccent else TextGray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
