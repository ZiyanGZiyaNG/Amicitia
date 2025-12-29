package com.example.amicitia.ui.menu

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

private object MenuTabs {
    const val HOME = "home"
    const val MAP = "map"
    const val CHAT = "chat"
    const val PROFILE = "profile"
}

private data class BottomItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

private val BottomItems = listOf(
    BottomItem(MenuTabs.HOME, Icons.Outlined.Home, "首頁"),
    BottomItem(MenuTabs.MAP, Icons.Outlined.Map, "地圖"),
    BottomItem(MenuTabs.CHAT, Icons.Outlined.ChatBubbleOutline, "聊天"),
    BottomItem(MenuTabs.PROFILE, Icons.Outlined.Person, "個人")
)

private val BgDark = Color(0xFF1E1E1E)
private val PrimaryBlue = Color(0xFF3F51B5)

@Composable
private fun AuthBackground(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(BgDark)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PrimaryBlue.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.88f),
                    radius = h * 0.75f
                )
            )
        }
    }
}

@Composable
fun MenuScreen(outerNavController: NavHostController) {
    val innerNav = rememberNavController()
    val auth = Firebase.auth

    val handleLogout: () -> Unit = remember {
        {
            auth.signOut()
            outerNavController.navigate(com.example.amicitia.nav.Routes.LOGIN) {
                popUpTo(outerNavController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AuthBackground(Modifier.matchParentSize())

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = { BottomBar(innerNav) }
        ) { padding ->
            MenuNavHost(
                navController = innerNav,
                outerNavController = outerNavController,
                modifier = Modifier.padding(padding),
                onLogout = handleLogout
            )
        }
    }
}

@Composable
private fun MenuNavHost(
    navController: NavHostController,
    outerNavController: NavHostController,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = MenuTabs.HOME,
        modifier = modifier
    ) {
        composable(MenuTabs.HOME) {
            com.example.amicitia.ui.menu.home.HomeRoute(
                onSportSelected = { sportKey ->
                    when (sportKey) {
                        "run" -> navController.navigate("run_mode")
                        else -> {}
                    }
                }
            )
        }

        composable(MenuTabs.MAP) {
            com.example.amicitia.ui.menu.map.MapRoute()
        }

        composable(MenuTabs.CHAT) {
            com.example.amicitia.ui.menu.home.chat.ChatNavHost(
                outerNavController = outerNavController
            )
        }

        composable(MenuTabs.PROFILE) {
            com.example.amicitia.ui.menu.profile.ProfileRoute(
                outerNavController = outerNavController,
                onLogout = onLogout
            )
        }

        // ---------------- Run flow 全部放內層 graph ----------------

        composable("run_mode") {
            com.example.amicitia.ui.menu.home.run.RunModeScreen(navController = navController)
        }

        composable("run_solo") {
            com.example.amicitia.ui.menu.home.run.RunSoloScreen(navController = navController)
        }

        // ✅ 關鍵：把「內層 navController」當作 MultiRunScreen 的 outerNavController 傳進去
        // 這樣 MultiRunScreen 內 navigate("run_temp_chat/...") 一定找得到
        composable("run_multi") {
            com.example.amicitia.ui.menu.home.run.MultiRunScreen(outerNavController = navController)
        }

        composable(route = "run_temp_chat/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            com.example.amicitia.ui.menu.home.chat.RunTempChatScreen(
                navController = navController,
                sessionId = sessionId
            )
        }

        // 你選 a：多人完成後要跳「仿 Solo 畫面」就走這條
        composable(route = "run_multi_solo_like/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            com.example.amicitia.ui.menu.home.run.RunMultiSoloLikeScreen(
                outerNavController = navController,
                sessionId = sessionId
            )
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination?.route

    val show = current in setOf(MenuTabs.HOME, MenuTabs.MAP, MenuTabs.CHAT, MenuTabs.PROFILE)
    if (!show) {
        Spacer(Modifier.height(0.dp))
        return
    }

    val shape = RoundedCornerShape(22.dp)
    NavigationBar(
        modifier = Modifier
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clip(shape),
        containerColor = BgDark.copy(alpha = 0.92f)
    ) {
        BottomItems.forEach { item ->
            val selected = current == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(MenuTabs.HOME) { saveState = true }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = null,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryBlue,
                    unselectedIconColor = Color.White.copy(alpha = 0.70f),
                    indicatorColor = Color.White.copy(alpha = 0.06f)
                )
            )
        }
    }
}