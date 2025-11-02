package com.example.amicitia.ui.menu.chat

import androidx.compose.runtime.Composable

@Composable
fun ChatRoute(
    onOpenChat: (otherUid: String) -> Unit
) {
    RecentChatsScreen(onOpenChat = onOpenChat)
}