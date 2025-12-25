package com.example.amicitia.ui.menu.home.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.amicitia.ui.menu.home.chat.RunTempChatRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunTempChatScreen(
    navController: NavHostController,
    sessionId: String,
    repo: RunTempChatRepository = RunTempChatRepository()
) {
    val me = Firebase.auth.currentUser?.uid.orEmpty()
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }

    val messages by repo.observeMessages(sessionId)
        .collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("約跑聊天室") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(messages) { m ->
                    val prefix = if (m.senderId == me) "我：" else "對方："
                    Text(prefix + m.text)
                    Spacer(Modifier.height(8.dp))
                }
            }

            Row {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("輸入訊息") }
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val text = input.trim()
                        if (text.isEmpty()) return@Button
                        input = ""
                        scope.launch {
                            repo.sendMessage(sessionId, me, text)
                        }
                    }
                ) {
                    Text("送出")
                }
            }
        }
    }
}