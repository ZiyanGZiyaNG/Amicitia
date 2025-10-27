package com.example.amicitia.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.amicitia.ui.login.LoginScreen
import com.example.amicitia.ui.register.RegisterScreen
import com.example.amicitia.ui.menu.MenuScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MENU = "menu"

    // 下面四個是 tab 用的「代號」，MenuScreen 內部用，不是這個 NavHost 的 route
    const val HOME = "home"
    const val MAP = "map"
    const val CHAT = "chat"
    const val PROFILE = "profile"
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
        composable(Routes.LOGIN) {
            LoginScreen(navController)
        }
        composable(Routes.REGISTER) {
            RegisterScreen(navController)
        }
        composable(Routes.MENU) {
            MenuScreen(navController)
        }
    }
}