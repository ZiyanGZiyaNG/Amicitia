package com.example.amicitia.ui.menu.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Locale

data class ChatSummary(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String? = null,
    val updatedAt: Timestamp? = null
)

@Composable
fun RecentChatsScreen(
    onOpenChat: (otherUid: String) -> Unit
) {
    val myUid = Firebase.auth.currentUser?.uid
    val db = Firebase.firestore
    var chats by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }

    DisposableEffect(myUid) {
        if (myUid == null) return@DisposableEffect onDispose { }
        val reg = db.collection("chats")
            .whereArrayContains("participants", myUid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                chats = snapshot?.documents?.map { d ->
                    ChatSummary(
                        chatId = d.id,
                        participants = (d.get("participants") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        lastMessage = d.getString("lastMessage"),
                        updatedAt = d.getTimestamp("updatedAt")
                    )
                } ?: emptyList()
            }
        onDispose { reg.remove() }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("最近聊天", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))

        if (chats.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("目前沒有聊天紀錄")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(chats) { chat ->
                    val otherUid = chat.participants.firstOrNull { it != myUid } ?: "Unknown"
                    ChatRowItem(
                        title = otherUid, // 先顯示 uid，後面再換暱稱
                        preview = chat.lastMessage ?: "",
                        time = chat.updatedAt.toReadableTime(),
                        onClick = { onOpenChat(otherUid) }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun ChatRowItem(
    title: String,
    preview: String,
    time: String?,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(preview, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = { if (!time.isNullOrEmpty()) Text(time, style = MaterialTheme.typography.labelSmall) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    )
}

private fun Timestamp?.toReadableTime(): String? {
    this ?: return null
    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    return sdf.format(this.toDate())
}