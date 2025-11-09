package com.example.amicitia.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.amicitia.ui.login.LoginScreen
import com.example.amicitia.ui.menu.MenuScreen
import com.example.amicitia.ui.menu.chat.ChatRoomScreen
import com.example.amicitia.ui.menu.profile.settings.AboutUsScreen
import com.example.amicitia.ui.register.RegisterScreen
import com.example.amicitia.ui.menu.profile.settings.AccountScreen
import com.example.amicitia.ui.menu.profile.settings.NotifyScree
import com.example.amicitia.ui.menu.profile.settings.PrivacyScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MENU = "menu"
    const val CHAT_ROOM = "chat_room/{otherUid}"
    const val ACCOUNT = "account"
    const val NOTIFY = "notify"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"
}

@Composable
fun AppNavHost(
    navController: NavHostController
) {
    val auth = Firebase.auth

    // 先以當前狀態決定起點，避免進 App 的瞬間閃空白
    val start = if (auth.currentUser == null) Routes.LOGIN else Routes.MENU

    // 監聽登入狀態改變：任何畫面呼叫 signOut() 後，這裡會自動把整個圖清掉並導到 login
    DisposableEffect(auth, navController) {
        val listener = FirebaseAuth.AuthStateListener { a ->
            val to = if (a.currentUser == null) Routes.LOGIN else Routes.MENU
            // 清整個圖（根層 pop），再推目標，避免掉回桌面
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
        composable(Routes.LOGIN)    { LoginScreen(navController) }
        composable(Routes.REGISTER) { RegisterScreen(navController) }

        // 主框架（含內層 BottomBar 與分頁 NavHost）
        composable(Routes.MENU)     { MenuScreen(outerNavController = navController) }

        // Profile → 設定面板外層頁面
        composable(Routes.ACCOUNT)  { AccountScreen(navController) }
        composable(Routes.NOTIFY)   { NotifyScree(navController) }
        composable(Routes.PRIVACY)  { PrivacyScreen(navController) }
        composable(Routes.ABOUT)    { AboutUsScreen(navController) }

        // 聊天室
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