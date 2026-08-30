package com.shadowvibe.app.ui.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import coil.compose.AsyncImage
import com.shadowvibe.app.data.api.ApiClient
import com.shadowvibe.app.data.model.ConversationPreview
import com.shadowvibe.app.data.model.GroupPreview
import com.shadowvibe.app.ui.Screen
import kotlinx.coroutines.launch

private val DarkBackground = Color(0xFF0E0E10)
private val SurfaceColor = Color(0xFF1E1E22)
private val CardColor = Color(0xFF18181B)
private val PurpleAccent = Color(0xFF8B5CF6)
private val TextGray = Color(0xFFA1A1AA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(navController: NavHostController) {
    var conversations by remember { mutableStateOf<List<ConversationPreview>>(emptyList()) }
    var groups by remember { mutableStateOf<List<GroupPreview>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadData() {
        scope.launch {
            try {
                conversations = ApiClient.api.getConversations().body() ?: emptyList()
                groups = ApiClient.api.getGroups().body() ?: emptyList()
            } catch (_: Exception) {
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    val filteredGroups = groups.filter {
        searchQuery.isBlank() || it.groupName.contains(searchQuery, ignoreCase = true)
    }
    val filteredConversations = conversations.filter {
        searchQuery.isBlank() || it.partnerUsername.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ShadowVibe",
                        color = PurpleAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.WatchHub.route) }) {
                        Icon(Icons.Default.PlayCircle, contentDescription = "Watch rooms", tint = PurpleAccent)
                    }
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextGray)
                    }
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                ApiClient.api.logout()
                            } catch (_: Exception) {}
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = TextGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.CreateGroup.route) },
                containerColor = PurpleAccent,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Group")
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                loadData()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Поиск...", color = TextGray) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextGray)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleAccent,
                            unfocusedBorderColor = Color(0xFF2E2E32),
                            focusedContainerColor = CardColor,
                            unfocusedContainerColor = CardColor,
                            cursorColor = PurpleAccent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                if (filteredGroups.isNotEmpty()) {
                    item {
                        SectionHeader("Группы")
                    }
                    items(filteredGroups) { group ->
                        GroupItem(group) {
                            navController.navigate(Screen.GroupChat.createRoute(group.groupId))
                        }
                    }
                }

                if (filteredConversations.isNotEmpty()) {
                    item {
                        SectionHeader("Чаты")
                    }
                    items(filteredConversations) { conversation ->
                        ConversationItem(conversation) {
                            navController.navigate(Screen.Chat.createRoute(conversation.partnerUsername))
                        }
                    }
                }

                if (!isLoading && filteredGroups.isEmpty() && filteredConversations.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "Нет диалогов" else "Ничего не найдено",
                                color = TextGray,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PurpleAccent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = TextGray,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun GroupItem(group: GroupPreview, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(CardColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SurfaceColor),
            contentAlignment = Alignment.Center
        ) {
            if (group.avatarFilename != null) {
                AsyncImage(
                    model = "http://192.168.0.61:1010/uploads/${group.avatarFilename}",
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    tint = PurpleAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.groupName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (group.lastMessagePreview.isNotBlank()) {
                Text(
                    text = if (group.lastMessageSender != null) "${group.lastMessageSender}: ${group.lastMessagePreview}" else group.lastMessagePreview,
                    color = TextGray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (group.unreadCount > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Badge(
                containerColor = PurpleAccent,
                contentColor = Color.White
            ) {
                Text(
                    text = if (group.unreadCount > 99) "99+" else group.unreadCount.toString(),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ConversationItem(conversation: ConversationPreview, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(CardColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SurfaceColor),
            contentAlignment = Alignment.Center
        ) {
            if (conversation.partnerAvatarFilename != null) {
                AsyncImage(
                    model = "http://192.168.0.61:1010/uploads/${conversation.partnerAvatarFilename}",
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = conversation.partnerUsername.firstOrNull()?.uppercase() ?: "?",
                    color = PurpleAccent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.partnerUsername,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (conversation.lastMessagePreview.isNotBlank()) {
                Text(
                    text = conversation.lastMessagePreview,
                    color = TextGray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = conversation.lastMessageTime,
                color = TextGray,
                fontSize = 12.sp
            )
            if (conversation.unreadCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Badge(
                    containerColor = PurpleAccent,
                    contentColor = Color.White
                ) {
                    Text(
                        text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
