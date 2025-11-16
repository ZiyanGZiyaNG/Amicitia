package com.example.amicitia.ui.menu.chat

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val RoomPrimaryBlue = Color(0xFF3F51B5)

// 玻璃感泡泡
private val GlassBubbleMine = Color.White.copy(alpha = 0.85f)
private val GlassBubbleOther = Color.White.copy(alpha = 0.70f)
private val BubbleBorderMine = RoomPrimaryBlue.copy(alpha = 0.35f)
private val BubbleBorderOther = RoomPrimaryBlue.copy(alpha = 0.18f)

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
    val chatId = remember(me?.uid, peerId) { buildChatId(me?.uid ?: "", peerId) }

    // 監聽訊息
    DisposableEffect(chatId) {
        val ref = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)

        val listener = ref.addSnapshotListener { snap, e ->
            if (e != null) {
                Log.e("ChatRoom", "listen error", e)
                return@addSnapshotListener
            }
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

        onDispose { listener.remove() }
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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // ✅ 跟個人頁一樣的整片柔和漸層背景
        SoftGradientBackground(modifier = Modifier.fillMaxSize())

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(peerName) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = RoomPrimaryBlue
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = RoomPrimaryBlue,
                        navigationIconContentColor = RoomPrimaryBlue
                    )
                )
            },
            // ✅ Scaffold 本身也透明，不再多一層實色底
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageBubble(
                            message = msg,
                            isMine = msg.userId == me?.uid
                        )
                    }
                }

                // ✅ 輸入列不要再整條實色，只是微透明白
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        placeholder = { Text("輸入訊息…") },
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.96f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                            disabledContainerColor = Color.White.copy(alpha = 0.7f),
                            focusedBorderColor = RoomPrimaryBlue.copy(alpha = 0.7f),
                            unfocusedBorderColor = RoomPrimaryBlue.copy(alpha = 0.35f),
                            cursorColor = RoomPrimaryBlue
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { if (!sending) scope.launch { send() } },
                        enabled = input.isNotBlank() && !sending,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = RoomPrimaryBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "送出"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isMine: Boolean
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isMine) GlassBubbleMine else GlassBubbleOther,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isMine) BubbleBorderMine else BubbleBorderOther
            )
        ) {
            Text(
                message.text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = Color(0xFF0F172A)
            )
        }
    }
}

private fun buildChatId(a: String, b: String): String {
    return listOf(a, b).sorted().joinToString("_")
}

// ✅ 靜態柔和漸層，跟個人頁一樣整片延伸，不會有上下兩條實色
@Composable
private fun SoftGradientBackground(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF4F6FF),
                    Color(0xFFE7EEFF),
                    Color(0xFFDCE6FF)
                )
            )
        )
    )
}