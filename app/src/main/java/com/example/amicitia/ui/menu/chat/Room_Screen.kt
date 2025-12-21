package com.example.amicitia.ui.menu.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomsScreen(
    onOpenRoom: (roomId: String) -> Unit,
    vm: RoomsViewModel = viewModel()
) {
    val myUid = Firebase.auth.currentUser?.uid ?: return
    LaunchedEffect(myUid) { vm.start(myUid) }

    val rooms by vm.rooms.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Chats") }) }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(rooms, key = { it.roomId }) { room ->
                RoomRow(
                    room = room,
                    myUid = myUid,
                    hasUnread = vm.hasUnread(room, myUid),
                    onClick = { onOpenRoom(room.roomId) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun RoomRow(
    room: Room,
    myUid: String,
    hasUnread: Boolean,
    onClick: () -> Unit
) {
    val otherUid = room.members.firstOrNull { it != myUid } ?: "unknown"
    val preview = room.lastMessageText ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = otherUid)
            Spacer(Modifier.height(4.dp))
            Text(
                text = preview,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (hasUnread) {
            AssistChip(onClick = { }, label = { Text("New") })
        }
    }
}