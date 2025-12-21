package com.example.amicitia.ui.menu.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    roomId: String,
    onBack: () -> Unit,
    vm: ChatRoomViewModel = viewModel()
) {
    val myUid = Firebase.auth.currentUser?.uid ?: return

    LaunchedEffect(roomId) { vm.start(roomId) }
    LaunchedEffect(roomId, myUid) { vm.markRead(roomId, myUid) }

    val messagesDesc by vm.messagesDesc.collectAsState()
    val messagesAsc = remember(messagesDesc) { messagesDesc.asReversed() }

    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(messagesAsc.size) {
        if (messagesAsc.isNotEmpty()) {
            listState.animateScrollToItem(messagesAsc.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(12.dp)
            ) {
                items(messagesAsc, key = { it.id }) { msg ->
                    MessageBubble(msg = msg, isMe = msg.senderId == myUid)
                    Spacer(Modifier.height(8.dp))
                }
            }

            HorizontalDivider()

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
                    placeholder = { Text("輸入訊息...") },
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    val toSend = input
                    vm.send(roomId, myUid, toSend) { ok ->
                        if (ok) input = ""
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "send")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: Message, isMe: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 2.dp) {
            Text(
                text = msg.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}