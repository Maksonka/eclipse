package com.shadowvibe.app.ui.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.shadowvibe.app.data.api.ApiClient
import com.shadowvibe.app.ui.Screen
import kotlinx.coroutines.launch

private val PurpleAccent = Color(0xFF8B5CF6)
private val DarkSurface = Color(0xFF1A1A2E)
private val DarkBackground = Color(0xFF0F0F23)
private val DarkCard = Color(0xFF16213E)
private val DangerRed = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(navController: NavHostController) {
    var groupName by remember { mutableStateOf("") }
    var membersInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Новая группа",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Название группы",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    TextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Введите название", color = Color.Gray)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = PurpleAccent,
                            focusedIndicatorColor = PurpleAccent,
                            unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    HorizontalDivider(
                        color = Color.Gray.copy(alpha = 0.2f)
                    )

                    Text(
                        text = "Участники",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    TextField(
                        value = membersInput,
                        onValueChange = { membersInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("user1, user2, user3", color = Color.Gray)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = PurpleAccent,
                            focusedIndicatorColor = PurpleAccent,
                            unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage ?: "",
                    color = DangerRed,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val name = groupName.trim()
                    val membersList = membersInput
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    if (name.isBlank()) {
                        errorMessage = "Введите название группы"
                        return@Button
                    }

                    if (membersList.isEmpty()) {
                        errorMessage = "Добавьте хотя бы одного участника"
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    scope.launch {
                        try {
                            ApiClient.api.createGroup(
                                mapOf(
                                    "name" to name,
                                    "members" to membersList.joinToString(",")
                                )
                            )
                            navController.popBackStack()
                        } catch (e: Exception) {
                            errorMessage = "Ошибка создания группы"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurpleAccent,
                    disabledContainerColor = PurpleAccent.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Создать",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
