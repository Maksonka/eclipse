package com.shadowvibe.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shadowvibe.app.ui.theme.PurpleAccent

@Composable
fun UserAvatar(
    username: String,
    avatarUrl: String? = null,
    size: Dp = 40.dp,
    isOnline: Boolean = false
) {
    Box(modifier = Modifier.size(size)) {
        if (avatarUrl != null) {
            AsyncImage(
                model = "http://192.168.0.61:1010/uploads/$avatarUrl",
                contentDescription = username,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(PurpleAccent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = username.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontSize = (size.value * 0.45f).sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.3f)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-2).dp, y = (-2).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E))
            )
        }
    }
}
