package com.example.amicitia.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.amicitia.ui.login.LoginScreen
import com.example.amicitia.ui.menu.MenuScreen
import com.example.amicitia.ui.menu.chat.ChatRoomScreen   // ← 直接匯入這個函式
import com.example.amicitia.ui.register.RegisterScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MENU = "menu"
    const val CHAT_ROOM = "chat_room/{otherUid}"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) { LoginScreen(navController) }
        composable(Routes.REGISTER) { RegisterScreen(navController) }
        composable(Routes.MENU) { MenuScreen(outerNavController = navController) }

        composable(
            route = Routes.CHAT_ROOM,
            arguments = listOf(navArgument("otherUid") { type = NavType.StringType })
        ) { backStackEntry ->
            val otherUid = backStackEntry.arguments?.getString("otherUid") ?: return@composable
            ChatRoomScreen(
                otherUid = otherUid,
                onBack = { navController.popBackStack() }
            )
        }
    }
}