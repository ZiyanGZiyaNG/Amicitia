package com.example.amicitia.ui.menu.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
private val ChatPrimaryBlue = Color(0xFF3F51B5)
private val ChatBackground = Color(0xFFEFF3FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentChatsScreen(
    onOpenChat: (peerId: String, peerName: String) -> Unit,
    onSearchClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("最近聊天") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "搜尋使用者",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ChatPrimaryBlue,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChatBackground)
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "目前沒有聊天紀錄",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )
        }
    }
}