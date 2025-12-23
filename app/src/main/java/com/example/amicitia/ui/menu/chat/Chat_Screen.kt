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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

/* ---------------- Theme ---------------- */

private val BgDark = Color(0xFF1E1E1E)
private val PrimaryBlue = Color(0xFF3F51B5)
private val CardSolidGray = Color(0xFF2A2A2A)
private val AvatarBg = Color(0xFF3A3A3A)

/* ---------------- Data ---------------- */

private data class ChatRoomItem(
    val roomId: String,
    val otherUid: String,
    val lastText: String,
    val lastAt: Timestamp?
)

private data class UserProfile(
    val nickname: String,
    val avatarUrl: String
)

/* ---------------- Background ---------------- */

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

/* ---------------- Screen ---------------- */

@Composable
fun ChatScreen(
    outerNavController: NavController,
    modifier: Modifier = Modifier
) {
    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = remember { FirebaseFirestore.getInstance() }

    var rooms by remember { mutableStateOf<List<ChatRoomItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    val profileCache = remember { mutableStateMapOf<String, UserProfile>() }
    val inFlight = remember { mutableStateMapOf<String, Boolean>() }

    DisposableEffect(Unit) {
        val reg: ListenerRegistration =
            db.collection("rooms")
                .orderBy("lastMessageAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, _ ->
                    if (snap == null) return@addSnapshotListener

                    rooms = snap.documents.mapNotNull { doc ->
                        val deletedFor = doc.get("deletedFor") as? Map<*, *>
                        if (deletedFor?.get(uid) == true) return@mapNotNull null

                        val membersRaw = doc.get("members") as? List<*>
                        val memberUids = membersRaw?.mapNotNull { it as? String }.orEmpty()
                        if (!memberUids.contains(uid)) return@mapNotNull null

                        val otherUid = memberUids.firstOrNull { it != uid } ?: return@mapNotNull null

                        ChatRoomItem(
                            roomId = doc.id,
                            otherUid = otherUid,
                            lastText = doc.getString("lastMessageText") ?: "",
                            lastAt = doc.getTimestamp("lastMessageAt")
                        )
                    }

                    loading = false
                }

        onDispose { reg.remove() }
    }

    LaunchedEffect(rooms) {
        val need = rooms.map { it.otherUid }.distinct()
        need.forEach { otherUid ->
            if (profileCache.containsKey(otherUid)) return@forEach
            if (inFlight[otherUid] == true) return@forEach

            inFlight[otherUid] = true

            db.collection("users")
                .document(otherUid)
                .get()
                .addOnSuccessListener { doc ->
                    val nickname = doc.getString("nickname").orEmpty().ifBlank { "使用者" }
                    val avatarUrl = doc.getString("avatarUrl").orEmpty()
                    profileCache[otherUid] = UserProfile(nickname = nickname, avatarUrl = avatarUrl)
                }
                .addOnFailureListener {
                    profileCache[otherUid] = UserProfile(nickname = "使用者", avatarUrl = "")
                }
                .addOnCompleteListener {
                    inFlight.remove(otherUid)
                }
        }
    }

    Box(
        modifier = modifier
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
                        val profile = profileCache[room.otherUid]
                        ChatItemCard(
                            room = room,
                            nickname = profile?.nickname ?: "使用者",
                            avatarUrl = profile?.avatarUrl ?: "",
                            onClick = {
                                outerNavController.navigate("room/${room.roomId}")
                            }
                        )
                    }
                }
            }
        }
    }
}

/* ---------------- Card ---------------- */

@Composable
private fun ChatItemCard(
    room: ChatRoomItem,
    nickname: String,
    avatarUrl: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .shadow(0.dp)
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
        AvatarCircle(
            nickname = nickname,
            avatarUrl = avatarUrl
        )

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

                room.lastAt?.let {
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
                text = room.lastText,
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
    val size = 52.dp
    val initial = nickname.trim().firstOrNull()?.toString()?.uppercase(Locale.getDefault()) ?: "?"

    Box(
        modifier = Modifier
            .size(size)
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

private fun formatTime(ts: Timestamp): String {
    val date = ts.toDate()
    val now = Date()
    val diff = now.time - date.time

    return when {
        diff < 60_000 -> "剛剛"
        diff < 3_600_000 -> "${diff / 60_000} 分鐘"
        else -> SimpleDateFormat("MM/dd", Locale.getDefault()).format(date)
    }
}