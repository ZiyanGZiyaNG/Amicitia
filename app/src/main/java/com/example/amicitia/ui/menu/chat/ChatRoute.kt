package com.example.amicitia.ui.menu.chat

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun ChatRoute() {
    val chatNav = rememberNavController()

    NavHost(
        navController = chatNav,
        startDestination = "chat_list"
    ) {
        composable("chat_list") {
            ChatListScreen(
                onChatSelected = { friend ->
                    val encodedName = Uri.encode(friend.name)
                    chatNav.navigate("chat_room/${friend.id}/$encodedName")
                }
            )
        }

        composable(
            route = "chat_room/{peerId}/{peerName}",
            arguments = listOf(
                navArgument("peerId") { type = NavType.StringType },
                navArgument("peerName") { type = NavType.StringType }
            )
        ) { entry ->
            val id = entry.arguments?.getString("peerId") ?: ""
            val encoded = entry.arguments?.getString("peerName") ?: ""
            val name = Uri.decode(encoded)

            ChatRoomScreen(
                peerId = id,
                peerName = name,
                onBack = { chatNav.popBackStack() }
            )
        }
    }
}