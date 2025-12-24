package com.example.amicitia.ui.menu.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun ChatNavHost(
    outerNavController: NavController,
    modifier: Modifier = Modifier
) {
    ChatScreen(
        outerNavController = outerNavController
    )
}