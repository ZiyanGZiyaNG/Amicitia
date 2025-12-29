package com.example.amicitia.ui.menu.home.run

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController


@Composable
fun RunMultiSoloLikeScreen(
    outerNavController: NavHostController,
    sessionId: String
) {
    RunSoloScreen(navController = outerNavController)
}