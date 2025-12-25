package com.example.amicitia.ui.menu.home.chat

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

private val BgDark = Color(0xFF1E1E1E)
private val PrimaryBlue = Color(0xFF3F51B5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunTempChatScreen(
    navController: NavHostController,
    sessionId: String
) {
    val repo = remember { RunTempChatRepository() }
    val meUid = Firebase.auth.currentUser?.uid.orEmpty()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var input by remember { mutableStateOf("") }

    // 觀察訊息
    val messages by repo.observeMessages(sessionId).collectAsState(initial = emptyList())

    // 取對方 uid（用 room 文件的 members 最準，但你這裡先用訊息推也可）
    // 建議：你 ensureRoom 已寫 members，這裡簡化：從第一則非自己的訊息抓對方 uid
    val otherUid = remember(messages, meUid) {
        messages.firstOrNull { it.senderUid.isNotBlank() && it.senderUid != meUid }?.senderUid.orEmpty()
    }

    var otherNickname by remember { mutableStateOf("聊天") }
    var otherAvatarUrl by remember { mutableStateOf("") }

    LaunchedEffect(otherUid) {
        if (otherUid.isBlank()) return@LaunchedEffect
        repo.getUserProfileOnce(otherUid) { nick, avatar ->
            otherNickname = nick
            otherAvatarUrl = avatar
        }
    }

    fun pushSnack(msg: String) {
        scope.launch { snackbar.showSnackbar(msg, withDismissAction = true) }
    }

    Scaffold(
        containerColor = BgDark,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = otherAvatarUrl.ifBlank { null },
                            contentDescription = null,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = otherNickname,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("輸入訊息") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = {
                        val msg = input.trim()
                        if (msg.isBlank()) return@Button

                        scope.launch {
                            runCatching {
                                repo.sendMessage(sessionId = sessionId, senderUid = meUid, text = msg)
                            }.onFailure {
                                Log.e("RunTempChat", "send failed", it)
                                pushSnack("送出失敗：${it.message ?: "unknown"}")
                            }
                        }

                        input = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) { Text("送出") }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.id }) { m ->
                val mine = m.senderUid == meUid

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (mine) PrimaryBlue else Color.White.copy(alpha = 0.10f)
                    ) {
                        Text(
                            text = m.text,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}