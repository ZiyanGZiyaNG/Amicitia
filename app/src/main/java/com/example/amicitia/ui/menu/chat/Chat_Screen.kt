package com.example.amicitia.ui.menu.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/* -------------------------
   Theme Colors（對齊 Home）
-------------------------- */

private val BgDark = Color(0xFF1E1E1E)
private val PrimaryBlue = Color(0xFF3F51B5)

/* -------------------------
   Background（完全同 Home）
-------------------------- */

@Composable
private fun AuthBackground(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(BgDark)) {
        BottomDecorBackground(
            modifier = Modifier.matchParentSize(),
            tint = PrimaryBlue
        )
    }
}

@Composable
private fun BottomDecorBackground(
    modifier: Modifier = Modifier,
    tint: Color
) {
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    tint.copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.88f),
                radius = h * 0.75f
            )
        )
    }
}

/* -------------------------
   Liquid Glass Card（同 Home）
-------------------------- */

@Composable
private fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerDp: Dp = 24.dp,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerDp)

    val seed = remember { Random.nextInt() }
    val stableRandom = remember(seed) { Random(seed) }

    val interactionSource = remember { MutableInteractionSource() }
    val glassBase = Color.White.copy(alpha = 0.12f)

    Row(
        modifier = modifier
            .shadow(14.dp, shape, clip = false)
            .clip(shape)
            .background(glassBase)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onClick() }
                } else Modifier
            )
            .drawWithCache {
                val r = cornerDp.toPx()

                val highlight = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )

                val depth = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.14f)
                    ),
                    startY = size.height * 0.35f,
                    endY = size.height
                )

                val dots = List(60) {
                    Triple(
                        stableRandom.nextFloat() * size.width,
                        stableRandom.nextFloat() * size.height,
                        stableRandom.nextFloat() * 1.4f + 0.4f
                    )
                }

                onDrawBehind {
                    drawRoundRect(brush = highlight, cornerRadius = CornerRadius(r, r))
                    drawRoundRect(brush = depth, cornerRadius = CornerRadius(r, r))
                    dots.forEach { (x, y, rad) ->
                        drawCircle(
                            color = Color.White.copy(alpha = 0.018f),
                            radius = rad,
                            center = Offset(x, y)
                        )
                    }
                }
            }
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/* -------------------------
   Fake Chat Data（先撐 UI）
-------------------------- */

private data class ChatItem(
    val name: String,
    val lastMessage: String,
    val time: String
)

/* -------------------------
   Chat Screen（重點）
-------------------------- */

@Composable
fun ChatScreen() {

    val chats = remember {
        listOf(
            ChatItem("阿明", "晚點要不要跑步？", "2 min"),
            ChatItem("小華", "OK", "Yesterday"),
            ChatItem("羽球社", "下週練習時間調整", "Mon"),
            ChatItem("新朋友", "尚未開始對話", "")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        AuthBackground(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Chats",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chats) { chat ->
                    ChatItemCard(chat = chat)
                }
            }
        }
    }
}

@Composable
private fun ChatItemCard(
    chat: ChatItem
) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
        onClick = {
            // 之後接 ChatNav → Room
        }
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = chat.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = chat.lastMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 1
            )
        }

        if (chat.time.isNotEmpty()) {
            Text(
                text = chat.time,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f)
            )
        }
    }
}