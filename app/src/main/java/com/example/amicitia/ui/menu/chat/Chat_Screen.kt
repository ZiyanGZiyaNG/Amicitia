package com.example.amicitia.ui.menu.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BgDark = Color(0xFF1E1E1E)
private val PrimaryBlue = Color(0xFF3F51B5)
private val CardSolidGray = Color(0xFF2A2A2A)
private val AvatarBg = Color(0xFF3A3A3A)

private data class UserProfile(
    val nickname: String,
    val avatarUrl: String
)

@Composable
private fun AuthBackground(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(BgDark)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PrimaryBlue.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.88f),
                    radius = h * 0.75f
                )
            )
        }
    }
}

@Composable
fun ChatScreen(
    outerNavController: NavController,
    repo: ChatRepository = ChatRepository()
) {
    val uid = Firebase.auth.currentUser?.uid ?: return

    var rooms by remember { mutableStateOf<List<Room>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    val profileCache = remember { mutableStateMapOf<String, UserProfile>() }
    val inFlight = remember { mutableStateMapOf<String, Boolean>() }

    val scope = rememberCoroutineScope()

    LaunchedEffect(uid) {
        repo.observeRooms(uid).collect { list ->
            rooms = list
            loading = false
        }
    }

    LaunchedEffect(rooms) {
        val others = rooms.mapNotNull { r -> r.members.firstOrNull { it != uid } }.distinct()
        others.forEach { otherUid ->
            if (profileCache.containsKey(otherUid)) return@forEach
            if (inFlight[otherUid] == true) return@forEach

            inFlight[otherUid] = true
            repo.getUserProfileOnce(otherUid) { nickname, avatarUrl ->
                profileCache[otherUid] = UserProfile(nickname, avatarUrl)
                inFlight.remove(otherUid)
            }
        }
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
                text = "聊天",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            Spacer(Modifier.height(12.dp))

            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rooms, key = { it.roomId }) { room ->
                        val otherUid = room.members.firstOrNull { it != uid } ?: ""
                        val profile = profileCache[otherUid]
                        val nickname = profile?.nickname ?: "使用者"
                        val avatarUrl = profile?.avatarUrl ?: ""

                        ChatItemCard(
                            nickname = nickname,
                            avatarUrl = avatarUrl,
                            lastText = room.lastMessageText.orEmpty(),
                            lastAt = room.lastMessageAt?.toDate(),
                            onClick = {
                                // ✅ 進房間走外層 Nav：這樣聊天室就不會有底部 bar
                                outerNavController.navigate("room/${room.roomId}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatItemCard(
    nickname: String,
    avatarUrl: String,
    lastText: String,
    lastAt: Date?,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(shape)
            .background(CardSolidGray)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarCircle(nickname = nickname, avatarUrl = avatarUrl)

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = nickname,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                lastAt?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatTime(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.45f)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = lastText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AvatarCircle(
    nickname: String,
    avatarUrl: String
) {
    val initial = nickname.trim().firstOrNull()?.toString()?.uppercase(Locale.getDefault()) ?: "?"

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(AvatarBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}

private fun formatTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    return when {
        diff < 60_000 -> "剛剛"
        diff < 3_600_000 -> "${diff / 60_000} 分鐘"
        else -> SimpleDateFormat("MM/dd", Locale.getDefault()).format(date)
    }
}