package com.example.amicitia.ui.menu.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.navigation.NavHostController
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

/* ---------------- Data ---------------- */

private data class ChatRoomItem(
    val roomId: String,
    val lastText: String,
    val lastAt: Timestamp?
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
    navController: NavHostController
) {
    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = remember { FirebaseFirestore.getInstance() }

    var rooms by remember { mutableStateOf<List<ChatRoomItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val reg: ListenerRegistration =
            db.collection("rooms")
                .orderBy("lastMessageAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, _ ->
                    if (snap == null) return@addSnapshotListener

                    rooms = snap.documents.mapNotNull { doc ->
                        val deletedFor = doc.get("deletedFor") as? Map<*, *>
                        if (deletedFor?.get(uid) == true) return@mapNotNull null

                        ChatRoomItem(
                            roomId = doc.id,
                            lastText = doc.getString("lastMessageText") ?: "",
                            lastAt = doc.getTimestamp("lastMessageAt")
                        )
                    }
                    loading = false
                }

        onDispose { reg.remove() }
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
                    items(rooms) { room ->
                        ChatItemCard(
                            room = room,
                            onClick = {
                                navController.navigate("room/${room.roomId}")
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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "聊天室",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = room.lastText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        room.lastAt?.let {
            Text(
                text = formatTime(it),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f)
            )
        }
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