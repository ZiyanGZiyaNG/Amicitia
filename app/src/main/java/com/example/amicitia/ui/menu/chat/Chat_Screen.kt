package com.example.amicitia.ui.menu.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ChatMsg(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null
)

@Composable
fun ChatRoomScreen(
    otherUid: String,
    onBack: () -> Unit
) {
    require(otherUid.isNotBlank()) { "otherUid is blank" }

    val myUid = Firebase.auth.currentUser?.uid ?: ""
    val db = Firebase.firestore
    val chatId = remember(myUid, otherUid) { listOf(myUid, otherUid).sorted().joinToString("_") }

    var msgs by remember { mutableStateOf<List<ChatMsg>>(emptyList()) }
    var input by remember { mutableStateOf("") }

    // 監聽訊息
    DisposableEffect(chatId) {
        val reg = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                msgs = snap?.documents?.map { d ->
                    ChatMsg(
                        id = d.id,
                        senderId = d.getString("senderId") ?: "",
                        text = d.getString("text") ?: "",
                        timestamp = d.getTimestamp("timestamp")
                    )
                } ?: emptyList()
            }
        onDispose { reg.remove() }
    }

    fun sendMessage() {
        val txt = input.trim()
        if (txt.isEmpty() || myUid.isEmpty()) return
        input = ""

        val chatRef = db.collection("chats").document(chatId)
        val msgRef  = chatRef.collection("messages").document()

        // 分兩步寫入：先建 chat，再寫 message（避免 PERMISSION_DENIED）
        CoroutineScope(Dispatchers.IO).launch {
            try {
                chatRef.set(
                    mapOf(
                        "participants" to listOf(myUid, otherUid),
                        "lastMessage" to txt,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()

                msgRef.set(
                    mapOf(
                        "senderId" to myUid,
                        "text" to txt,
                        "timestamp" to FieldValue.serverTimestamp(),
                        "read" to false
                    )
                ).await()
            } catch (e: Exception) {
                input = txt // 失敗把文字放回去，避免黑洞
                e.printStackTrace()
            }
        }
    }

    // 簡單的工具列＋訊息區＋輸入列
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("返回") }
            Spacer(Modifier.width(8.dp))
            Text(text = otherUid, style = MaterialTheme.typography.titleLarge)
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(msgs) { m ->
                val mine = m.senderId == myUid
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = m.text,
                            color = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            textAlign = if (mine) TextAlign.End else TextAlign.Start
                        )
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("輸入訊息…") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { sendMessage() }) { Text("送出") }
        }
    }
}