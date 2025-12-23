package com.example.amicitia.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.amicitia.ui.login.LoginScreen
import com.example.amicitia.ui.menu.MenuScreen
import com.example.amicitia.ui.menu.chat.ChatRoomScreen
import com.example.amicitia.ui.menu.home.run.RunSessionScreen
import com.example.amicitia.ui.menu.profile.settings.AboutUsScreen
import com.example.amicitia.ui.menu.profile.settings.AccountScreen
import com.example.amicitia.ui.menu.profile.settings.NotifyScreen
import com.example.amicitia.ui.menu.profile.settings.PrivacyScreen
import com.example.amicitia.ui.register.RegisterScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MENU = "menu"

    const val ACCOUNT = "account"
    const val NOTIFY = "notify"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"
    const val MAP = "map"
}

@Composable
fun AppNavHost(
    navController: NavHostController
) {
    val auth = Firebase.auth
    val start = if (auth.currentUser == null) Routes.LOGIN else Routes.MENU

    DisposableEffect(auth, navController) {
        val listener = FirebaseAuth.AuthStateListener { a ->
            val to = if (a.currentUser == null) Routes.LOGIN else Routes.MENU
            navController.navigate(to) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    NavHost(
        navController = navController,
        startDestination = start
    ) {
        composable(Routes.LOGIN) { LoginScreen(navController) }
        composable(Routes.REGISTER) { RegisterScreen(navController) }

        composable(Routes.MENU) { MenuScreen(outerNavController = navController) }

        composable(Routes.ACCOUNT) { AccountScreen(navController) }
        composable(Routes.NOTIFY) { NotifyScreen(navController) }
        composable(Routes.PRIVACY) { PrivacyScreen(navController) }
        composable(Routes.ABOUT) { AboutUsScreen(navController) }

        composable("run_session/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            RunSessionScreen(navController = navController, sessionId = sessionId)
        }
        composable("room/{roomId}") { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            ChatRoomScreen(
                roomId = roomId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}