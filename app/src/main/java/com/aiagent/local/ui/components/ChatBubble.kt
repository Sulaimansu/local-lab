package com.aiagent.local.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aiagent.local.ui.theme.*

@Composable
fun ChatBubble(
    text: String,
    isUser: Boolean,
    isStreaming: Boolean = false,
    isSystem: Boolean = false,
    isTool: Boolean = false
) {
    val backgroundColor = when {
        isUser -> UserBubble.copy(alpha = 0.7f)
        isTool -> ToolBubble.copy(alpha = 0.7f)
        isSystem -> MaterialTheme.colorScheme.surfaceVariant
        else -> BotBubble.copy(alpha = 0.7f)
    }

    val textColor = when {
        isUser || isTool -> androidx.compose.ui.graphics.Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(backgroundColor)
                .padding(12.dp)
        ) {
            Text(
                text = text + if (isStreaming) "▊" else "",
                color = textColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}