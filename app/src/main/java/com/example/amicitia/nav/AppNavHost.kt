package com.example.amicitia.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun AppNavHost(navController: NavHostController) {
    val auth = Firebase.auth

    var startDestination by remember { mutableStateOf(Routes.LOGIN) }
    LaunchedEffect(Unit) {
        startDestination = if (auth.currentUser != null) Routes.MENU else Routes.LOGIN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            // 這裡如果你 LoginScreen 參數不是 outerNavController，改回你專案原本的就好
            com.example.amicitia.ui.login.LoginScreen(
                outerNavController = navController
            )
        }

        composable(Routes.REGISTER) {
            com.example.amicitia.ui.register.RegisterScreen(
                outerNavController = navController
            )
        }

        composable(Routes.MENU) {
            com.example.amicitia.ui.menu.MenuScreen(outerNavController = navController)
        }
    }
}