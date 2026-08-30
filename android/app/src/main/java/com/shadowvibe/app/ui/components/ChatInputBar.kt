package com.shadowvibe.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shadowvibe.app.ui.theme.PurpleAccent

@Composable
fun ChatInputBar(
    onSend: (String) -> Unit,
    onAttach: () -> Unit = {},
    placeholder: String = "Сообщение..."
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .background(Color(0xFF121215))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onAttach,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = "Attach",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }

        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color.Gray
                )
            },
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E1E22)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E1E22),
                unfocusedContainerColor = Color(0xFF1E1E22),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = PurpleAccent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onSend(text.trim())
                    text = ""
                }
            },
            enabled = text.isNotBlank(),
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .then(
                    if (text.isNotBlank()) {
                        Modifier.background(PurpleAccent)
                    } else {
                        Modifier.background(Color(0xFF2A2A2E))
                    }
                )
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = if (text.isNotBlank()) Color.White else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
