package com.shadowvibe.app.ui.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.shadowvibe.app.data.api.ApiClient
import com.shadowvibe.app.data.model.WatchRoomChatMessageDto
import com.shadowvibe.app.data.model.WatchRoomState
import com.shadowvibe.app.data.ws.StompClient
import com.shadowvibe.app.ui.theme.PurpleAccent
import kotlinx.coroutines.launch

private val PurpleLight = Color(0xFFA78BFA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchRoomScreen(navController: NavHostController, roomId: Long, roomCode: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }

    var room by remember { mutableStateOf<WatchRoomState?>(null) }
    var isHost by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<WatchRoomChatMessageDto>>(emptyList()) }
    var chatInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var members by remember { mutableStateOf<List<String>>(emptyList()) }
    val listState = rememberLazyListState()

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }
    val stomp = remember { StompClient(ApiClient.httpClient) }

    fun sendControl(status: String, positionMs: Long?, videoUrl: String?, restart: Boolean) {
        if (!stomp.isConnected()) return
        val payload = mutableMapOf<String, Any>("roomId" to roomId)
        payload["status"] = status
        if (positionMs != null) payload["positionMs"] = positionMs
        if (videoUrl != null) payload["videoUrl"] = videoUrl
        if (restart) payload["restart"] = true
        stomp.send("/app/room.control", gson.toJson(payload))
    }

    fun applyState(state: WatchRoomState) {
        room = state
        members = state.members
        isHost = state.hostUsername == currentUser
        val newUrl = state.videoUrl
        val currentUrl = player.currentMediaItem?.mediaId ?: ""
        if (newUrl != null && (newUrl != currentUrl || state.restart)) {
            player.setMediaItem(MediaItem.fromUri(newUrl))
            player.prepare()
            player.seekTo(state.positionMs)
        }
        if (newUrl != null && state.status == "PLAYING") {
            player.play()
        } else if (newUrl != null) {
            player.pause()
        }
    }

    LaunchedEffect(roomId) {
        try {
            val meResp = ApiClient.api.getMe()
            currentUser = meResp.body()?.username ?: ""
        } catch (_: Exception) {}

        stomp.connect(ApiClient.getCookieString(), onConnected = {
            stomp.subscribe("/topic/room.$roomId") { payload ->
                try {
                    val obj = JsonParser.parseString(payload).asJsonObject
                    val state = WatchRoomState(
                        roomId = obj.get("roomId")?.asLong ?: roomId,
                        roomCode = roomCode,
                        name = obj.get("name")?.asString ?: room?.name ?: "",
                        hostUsername = obj.get("hostUsername")?.asString ?: "",
                        videoUrl = obj.get("videoUrl")?.takeIf { !it.isJsonNull }?.asString,
                        status = obj.get("status")?.asString ?: "PAUSED",
                        positionMs = obj.get("positionMs")?.asLong ?: 0,
                        updatedAtMs = obj.get("updatedAtMs")?.asLong ?: 0,
                        restart = obj.get("restart")?.asBoolean ?: false,
                        members = obj.get("members")?.takeIf { it.isJsonArray }?.asJsonArray?.map { it.asString } ?: emptyList()
                    )
                    applyState(state)
                } catch (_: Exception) {}
            }
            stomp.subscribe("/topic/room.$roomId.chat") { payload ->
                try {
                    val msg = gson.fromJson(payload, WatchRoomChatMessageDto::class.java)
                    messages = messages + msg
                    scope.launch { listState.animateScrollToItem(messages.size - 1) }
                } catch (_: Exception) {}
            }
            stomp.subscribe("/user/queue/room-state") { payload ->
                try {
                    val obj = JsonParser.parseString(payload).asJsonObject
                    val state = WatchRoomState(
                        roomId = obj.get("roomId")?.asLong ?: roomId,
                        roomCode = roomCode,
                        name = obj.get("name")?.asString ?: room?.name ?: "",
                        hostUsername = obj.get("hostUsername")?.asString ?: "",
                        videoUrl = obj.get("videoUrl")?.takeIf { !it.isJsonNull }?.asString,
                        status = obj.get("status")?.asString ?: "PAUSED",
                        positionMs = obj.get("positionMs")?.asLong ?: 0,
                        updatedAtMs = obj.get("updatedAtMs")?.asLong ?: 0,
                        restart = obj.get("restart")?.asBoolean ?: false,
                        members = obj.get("members")?.takeIf { it.isJsonArray }?.asJsonArray?.map { it.asString } ?: emptyList()
                    )
                    applyState(state)
                } catch (_: Exception) {}
            }
            stomp.send("/app/room.request-state", gson.toJson(mapOf("roomId" to roomId)))
        }, onError = {})
    }

    DisposableEffect(roomId) {
        onDispose {
            stomp.disconnect()
            player.release()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(room?.name ?: "Комната", color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(roomCode, color = PurpleAccent, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            try { stomp.send("/app/room.leave", gson.toJson(mapOf("roomId" to roomId))) } catch (_: Exception) {}
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${members.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isHost) {
                                val playing = player.isPlaying
                                if (playing) {
                                    sendControl("PAUSED", player.currentPosition, null, false)
                                    player.pause()
                                } else {
                                    sendControl("PLAYING", player.currentPosition, null, false)
                                    player.play()
                                }
                            }
                        },
                        enabled = isHost,
                        colors = IconButtonDefaults.iconButtonColors(disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Icon(
                            if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isHost) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (room?.status == "PLAYING") "Воспроизведение" else "Пауза",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    if (!isHost) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("(управляет хост)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }
            }

            if (isHost) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("URL видео (mp4/webm)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val url = urlInput.trim()
                                if (url.isNotEmpty()) {
                                    sendControl("PLAYING", 0, url, true)
                                    urlInput = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Сменить", color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                                Text("Чат комнаты", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            }
                        }
                    }
                    items(messages) { msg ->
                        val mine = msg.senderUsername == currentUser
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
                        ) {
                            val shape = if (mine) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                            Column(
                                modifier = Modifier
                                    .widthIn(max = 260.dp)
                                    .clip(shape)
                                    .background(if (mine) PurpleAccent else MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                if (!mine) {
                                    Text(msg.senderUsername, color = PurpleLight, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                                Text(msg.content ?: "", color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)),
                            placeholder = { Text("Сообщение...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                cursorColor = PurpleAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                val content = chatInput.trim()
                                if (content.isNotEmpty()) {
                                    stomp.send("/app/room.message", gson.toJson(mapOf("roomId" to roomId, "content" to content)))
                                    chatInput = ""
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = PurpleAccent)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
