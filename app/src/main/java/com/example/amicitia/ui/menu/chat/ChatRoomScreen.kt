package com.example.amicitia.ui.menu.chat

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val ChatPrimaryBlue = Color(0xFF3F51B5)
private val ChatBubbleMine = Color(0xFFB4C6FF)
private val ChatBubbleOther = Color(0xFFFFFFFF)
private val ChatBackground = Color(0xFFEFF3FF)

data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val userId: String = "",
    val userName: String = "",
    val createdAt: Timestamp? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    peerId: String,
    peerName: String,
    onBack: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = Firebase.auth
    val me = auth.currentUser

    val messages = remember { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val chatId = remember(me?.uid, peerId) {
        buildChatId(me?.uid ?: "", peerId)
    }

    DisposableEffect(chatId) {
        val ref = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)

        val listen = ref.addSnapshotListener { snap, e ->
            if (e != null) return@addSnapshotListener
            val list = snap?.documents?.map {
                ChatMessage(
                    id = it.id,
                    text = it.getString("text") ?: "",
                    userId = it.getString("userId") ?: "",
                    userName = it.getString("userName") ?: "",
                    createdAt = it.getTimestamp("createdAt")
                )
            } ?: emptyList()
            messages.clear()
            messages.addAll(list)
        }
        onDispose { listen.remove() }
    }

    suspend fun send() {
        if (input.isBlank()) return
        val meUser = me ?: return

        sending = true
        try {
            db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(
                    mapOf(
                        "text" to input,
                        "userId" to meUser.uid,
                        "userName" to (meUser.displayName ?: meUser.email ?: "我"),
                        "createdAt" to Timestamp.now()
                    )
                )
                .await()
            input = ""
        } catch (e: Exception) {
            Log.e("ChatSend", "send failed", e)
        }
        sending = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(peerName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ChatPrimaryBlue,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(ChatBackground)
                .padding(padding)
        ) {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages) { msg ->
                    ChatMessageBubble(
                        msg,
                        isMine = msg.userId == me?.uid
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("輸入訊息…") },
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(Modifier.width(8.dp))

                FilledIconButton(
                    onClick = { if (!sending) scope.launch { send() } },
                    shape = CircleShape,
                    enabled = input.isNotBlank()
                ) {
                    Icon(Icons.Filled.Send, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(msg: ChatMessage, isMine: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isMine) ChatBubbleMine else ChatBubbleOther,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp,
        ) {
            Text(
                msg.text,
                Modifier.padding(12.dp),
                color = Color(0xFF0F172A)
            )
        }
    }
}

private fun buildChatId(a: String, b: String): String {
    return listOf(a, b).sorted().joinToString("_")
}