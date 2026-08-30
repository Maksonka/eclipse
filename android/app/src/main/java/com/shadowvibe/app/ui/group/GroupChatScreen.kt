package com.shadowvibe.app.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.shadowvibe.app.data.api.ApiClient
import com.shadowvibe.app.data.model.GroupDetail
import com.shadowvibe.app.data.model.GroupMessage
import com.shadowvibe.app.data.ws.StompClient
import com.google.gson.Gson
import kotlinx.coroutines.launch

private val PurpleAccent = Color(0xFF8B5CF6)
private val DarkSurface = Color(0xFF1A1A2E)
private val DarkBackground = Color(0xFF0F0F23)
private val DarkCard = Color(0xFF16213E)
private val SenderColors = listOf(
    Color(0xFF8B5CF6),
    Color(0xFF06B6D4),
    Color(0xFFF59E0B),
    Color(0xFF10B981),
    Color(0xFFEF4444),
    Color(0xFFEC4899),
    Color(0xFF3B82F6),
    Color(0xFFF97316)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(navController: NavHostController, groupId: Long) {
    var messages by remember { mutableStateOf<List<GroupMessage>>(emptyList()) }
    var group by remember { mutableStateOf<GroupDetail?>(null) }
    var inputText by remember { mutableStateOf("") }
    var myUserId by remember { mutableLongStateOf(0L) }
    var currentUser by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }
    val stomp = remember { StompClient(ApiClient.httpClient) }
    val groupIdStr = groupId.toString()

    LaunchedEffect(groupId) {
        try {
            val response = ApiClient.api.getMe()
            val me = response.body()
            myUserId = me?.id ?: 0L
            currentUser = me?.username ?: ""
        } catch (_: Exception) {}

        try {
            val response = ApiClient.api.getGroupDetail(groupIdStr)
            group = response.body()
        } catch (_: Exception) {}

        try {
            val response = ApiClient.api.getGroupMessages(groupIdStr)
            messages = response.body() ?: emptyList()
        } catch (_: Exception) {}

        stomp.connect(ApiClient.getCookieString(), onConnected = {
            stomp.subscribe("/topic.group.$groupId") { payload ->
                try {
                    val msg = gson.fromJson(payload, GroupMessage::class.java)
                    messages = messages + msg
                    scope.launch {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                    try {
                        scope.launch { ApiClient.api.markGroupRead(groupIdStr) }
                    } catch (_: Exception) {}
                } catch (_: Exception) {}
            }
        }, onError = {})
    }

    DisposableEffect(Unit) {
        onDispose {
            stomp.disconnect()
        }
    }

    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun getSenderColor(senderUsername: String): Color {
        return SenderColors[Math.abs(senderUsername.hashCode()) % SenderColors.size]
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PurpleAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = group?.name?.firstOrNull()?.uppercase() ?: "G",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = group?.name ?: "Группа",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${group?.members?.size ?: 0} участников",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (group?.isCreator != true) {
                        IconButton(onClick = {
                            scope.launch {
                                try {
                                    ApiClient.api.leaveGroup(groupIdStr)
                                    stomp.disconnect()
                                    navController.popBackStack()
                                } catch (_: Exception) {}
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = null,
                                tint = Color.Red
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        bottomBar = {
            Surface(
                color = DarkSurface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp)),
                        placeholder = {
                            Text("Сообщение...", color = Color.Gray)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = PurpleAccent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            val content = inputText.trim()
                            if (content.isNotEmpty()) {
                                val payload = mapOf(
                                    "groupId" to groupId,
                                    "content" to content
                                )
                                stomp.send(
                                    "/app/group.send",
                                    gson.toJson(payload)
                                )
                                inputText = ""
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = PurpleAccent
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(messages) { msg ->
                val isMyMessage = msg.senderUsername == currentUser
                val alignment = if (isMyMessage) Alignment.CenterEnd else Alignment.CenterStart
                val bubbleColor = if (isMyMessage) PurpleAccent else DarkCard
                val senderColor = getSenderColor(msg.senderUsername)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (isMyMessage) 48.dp else 4.dp,
                            end = if (isMyMessage) 4.dp else 48.dp
                        ),
                    contentAlignment = alignment
                ) {
                    Column(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMyMessage) 16.dp else 4.dp,
                                    bottomEnd = if (isMyMessage) 4.dp else 16.dp
                                )
                            )
                            .background(bubbleColor)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (!isMyMessage) {
                            Text(
                                text = msg.senderUsername,
                                color = senderColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Text(
                            text = msg.content ?: "",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
