package com.example.amicitia.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.amicitia.ui.home.HomeScreen
import com.example.amicitia.ui.login.LoginScreen
import com.example.amicitia.ui.register.RegisterScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = Routes.LOGIN // 預設 login，但可以從 MainActivity 傳 HOME
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) { LoginScreen(navController) }
        composable(Routes.REGISTER) { RegisterScreen(navController) }
        composable(Routes.HOME) { HomeScreen(navController) }
    }
}