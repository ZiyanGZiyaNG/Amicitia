package com.example.amicitia.ui.menu.chat

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument

@Composable
fun ChatNavHost() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "rooms") {
        composable("rooms") {
            RoomsScreen(
                onOpenRoom = { roomId -> nav.navigate("room/$roomId") }
            )
        }
        composable(
            route = "room/{roomId}",
            arguments = listOf(navArgument("roomId") { type = NavType.StringType })
        ) { backStack ->
            val roomId = backStack.arguments?.getString("roomId") ?: return@composable
            ChatRoomScreen(
                roomId = roomId,
                onBack = { nav.popBackStack() }
            )
        }
    }
}