package com.example.amicitia.ui.menu.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun ChatNavHost(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "rooms",
        modifier = modifier
    ) {
        composable(route = "rooms") {
            RoomsScreen(
                onOpenRoom = {
                    navController.navigate("room")
                }
            )
        }

        composable(route = "room") {
            ChatRoomScreen(
                roomId = "test_room_001",
                onBack = { navController.popBackStack() }
            )
        }
    }
}