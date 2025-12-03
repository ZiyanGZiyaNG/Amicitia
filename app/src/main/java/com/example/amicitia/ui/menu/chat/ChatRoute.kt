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
        startDestination = "recent_chats"
    ) {
        composable("recent_chats") {
            RecentChatsScreen(
                onOpenChat = { peerId, peerName ->
                    val encodedName = Uri.encode(peerName)
                    chatNav.navigate("chat_room/$peerId/$encodedName")
                },
                onSearchClick = {
                    chatNav.navigate("search_user")
                }
            )
        }

        composable("search_user") {
            SearchUserScreen(
                onBack = { chatNav.popBackStack() },
                onOpenChat = { peerId, peerName ->
                    val encodedName = Uri.encode(peerName)
                    chatNav.navigate("chat_room/$peerId/$encodedName")
                }
            )
        }

        composable(
            route = "chat_room/{peerId}/{peerName}",
            arguments = listOf(
                navArgument("peerId") { type = NavType.StringType },
                navArgument("peerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
            val encodedName = backStackEntry.arguments?.getString("peerName") ?: ""
            val peerName = Uri.decode(encodedName)

            ChatRoomScreen(
                peerId = peerId,
                peerName = peerName,
                onBack = { chatNav.popBackStack() }
            )
        }
    }
}