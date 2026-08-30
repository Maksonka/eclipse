package com.shadowvibe.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shadowvibe.app.ui.theme.PurpleAccent

@Composable
fun MessageBubble(
    isMine: Boolean,
    content: String?,
    timestamp: String,
    read: Boolean = false,
    attachmentFilename: String? = null,
    attachmentType: String? = null,
    senderName: String? = null,
    replyToContent: String? = null,
    replyToSender: String? = null,
    reactions: Map<String, List<String>> = emptyMap(),
    edited: Boolean = false,
    isDeleted: Boolean = false
) {
    val bubbleColor = if (isMine) PurpleAccent else Color(0xFF1E1E22)
    val textColor = Color.White
    val align = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isMine) {
        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    } else {
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = align
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (senderName != null) {
                Text(
                    text = senderName,
                    color = PurpleAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
                )
            }

            Column(
                modifier = Modifier
                    .clip(shape)
                    .background(bubbleColor)
                    .padding(10.dp)
            ) {
                if (replyToContent != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(PurpleAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            if (replyToSender != null) {
                                Text(
                                    text = replyToSender,
                                    color = PurpleAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = replyToContent,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                maxLines = 2
                            )
                        }
                    }
                }

                when (attachmentType) {
                    "image" -> {
                        AsyncImage(
                            model = "http://192.168.0.61:1010/uploads/messages/$attachmentFilename",
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        if (content != null && !isDeleted) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                    "audio" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(text = "\u25B6", color = textColor, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = attachmentFilename ?: "Audio",
                                color = textColor.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }
                        if (content != null && !isDeleted) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                    "video" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(text = "\u25B6", color = textColor, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = attachmentFilename ?: "Video",
                                color = textColor.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }
                        if (content != null && !isDeleted) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                    "file" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(text = "\uD83D\uDCC4", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = attachmentFilename ?: "File",
                                color = textColor.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                maxLines = 2
                            )
                        }
                        if (content != null && !isDeleted) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                if (isDeleted) {
                    Text(
                        text = "Удалено",
                        color = textColor.copy(alpha = 0.5f),
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp
                    )
                } else if (content != null) {
                    Text(
                        text = content,
                        color = textColor,
                        fontSize = 14.sp
                    )
                }

                if (reactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        reactions.forEach { (emoji, users) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$emoji ${users.size}",
                                    fontSize = 12.sp,
                                    color = textColor
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (edited && !isDeleted) {
                        Text(
                            text = "ред.",
                            color = textColor.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = timestamp,
                        color = textColor.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                    if (isMine) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (read) "\u2713\u2713" else "\u2713",
                            color = if (read) PurpleAccent.copy(alpha = 0.8f) else textColor.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
