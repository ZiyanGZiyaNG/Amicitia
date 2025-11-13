package com.example.amicitia.ui.menu.chat

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

private val ChatPrimaryBlue = Color(0xFF3F51B5)
private val ChatBackground = Color(0xFFEFF3FF)

data class Friend(
    val id: String,
    val name: String,
    val online: Boolean = false
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatSelected: (Friend) -> Unit
) {
    val auth = Firebase.auth
    val currentUid = auth.currentUser?.uid
    val db = Firebase.firestore

    val friends = remember { mutableStateListOf<Friend>() }

    DisposableEffect(Unit) {
        val reg = db.collection("users")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Log.e("ChatList", "error", e)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    if (doc.id == currentUid) return@mapNotNull null
                    Friend(
                        id = doc.id,
                        name = doc.getString("name")
                            ?: doc.getString("displayName")
                            ?: doc.getString("email")
                            ?: "未知用戶",
                        online = doc.getBoolean("online") ?: false
                    )
                } ?: emptyList()
                friends.clear()
                friends.addAll(list)
            }
        onDispose { reg.remove() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("選擇聊天對象") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ChatBackground)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(12.dp)
        ) {
            items(friends, key = { it.id }) { friend ->
                FriendItem(friend = friend, onClick = { onChatSelected(friend) })
            }
        }
    }
}

@Composable
private fun FriendItem(friend: Friend, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ChatPrimaryBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friend.name.take(1),
                    color = ChatPrimaryBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = friend.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (friend.online) "在線" else "離線",
                    color = if (friend.online) Color(0xFF22C55E) else Color(0xFF64748B),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}