package com.example.amicitia.ui.menu

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.amicitia.nav.Routes
import com.example.amicitia.presence.PresenceManager
import com.example.amicitia.ui.menu.chat.ChatRoute
import com.example.amicitia.ui.menu.home.HomeRoute
import com.example.amicitia.ui.menu.home.run.MultiRunScreen
import com.example.amicitia.ui.menu.home.run.RunModeScreen
import com.example.amicitia.ui.menu.home.run.SoloRunScreen
import com.example.amicitia.ui.menu.map.MapRoute
import com.example.amicitia.ui.menu.profile.ProfileRoute
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.math.cos
import androidx.compose.animation.core.animateFloat

private val PrimaryBlue = Color(0xFF3F51B5)

// ========= 背景動畫 =========

@Composable
private fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    aStart: Color = Color(0xFFF3F6FF),
    aMid:   Color = Color(0xFFEAF1FF),
    aEnd:   Color = Color(0xFFDDE7FF),
    bStart: Color = Color(0xFFE8F0FF),
    bMid:   Color = Color(0xFFD6E3FF),
    bEnd:   Color = Color(0xFFCBD9FF),
) {
    val infinite = rememberInfiniteTransition()
    val tRaw by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )

    val t = (1f - cos(tRaw * Math.PI).toFloat()) / 2f

    val c1 = lerp(aStart, bStart, t)
    val c2 = lerp(aMid,   bMid,   t)
    val c3 = lerp(aEnd,   bEnd,   t)

    Box(
        modifier = modifier.drawBehind {
            val sx = size.width  * (0.15f + 0.35f * t)
            val sy = size.height * (0.10f + 0.25f * (1f - t))
            val ex = size.width  * (0.85f - 0.35f * t)
            val ey = size.height * (0.90f - 0.25f * (1f - t))

            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(c1, c2, c3),
                    start = Offset(sx, sy),
                    end = Offset(ex, ey)
                )
            )
        }
    )
}

// ========= 內層 tabs route =========

private object MenuTabs {
    const val HOME = "menu_home"
    const val MAP = "menu_map"
    const val CHAT = "menu_chat"
    const val PROFILE = "menu_profile"
}

// ========= Menu 主畫面 =========

@Composable
fun MenuScreen(
    outerNavController: NavController
) {
    val innerNav: NavHostController = rememberNavController()

    val auth = Firebase.auth
    val db = Firebase.firestore
    val uid = auth.currentUser?.uid

    val presenceManager = remember(uid) {
        if (uid != null) PresenceManager(uid, db) else null
    }

    DisposableEffect(uid) {
        presenceManager?.start()
        onDispose { presenceManager?.stop() }
    }

    val handleLogout: () -> Unit = {
        presenceManager?.stop()
        auth.signOut()
        outerNavController.navigate(Routes.LOGIN) {
            popUpTo(outerNavController.graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        AnimatedGradientBackground(modifier = Modifier.matchParentSize())

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = { BottomBar(innerNav) }
        ) { innerPadding ->
            MenuNavHost(
                navController = innerNav,
                outerNavController = outerNavController,
                modifier = Modifier.padding(innerPadding),
                onLogout = handleLogout
            )
        }
    }
}

// ========= 內層 NavHost =========

@Composable
private fun MenuNavHost(
    navController: NavHostController,
    outerNavController: NavController,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = MenuTabs.HOME,
        modifier = modifier
    ) {
        composable(MenuTabs.HOME) {
            HomeRoute(
                onSportSelected = { sportKey ->
                    when (sportKey) {
                        "run" -> navController.navigate("run_mode")
                        else  -> {
                            // 其他運動目前先不跳頁
                        }
                    }
                }
            )
        }

        composable(MenuTabs.MAP) {
            MapRoute()
        }

        composable(MenuTabs.CHAT) {
            ChatRoute()
        }

        composable(MenuTabs.PROFILE) {
            ProfileRoute(
                outerNavController = outerNavController,
                onLogout = onLogout,
            )
        }

        composable("run_mode") {
            RunModeScreen(navController)
        }

        composable("run_solo") {
            SoloRunScreen(navController)
        }

        composable("run_multi") {
            MultiRunScreen(navController)
        }
    }
}

// ========= BottomBar 狀態 =========

@Composable
private fun currentRoute(navController: NavHostController): String? {
    val entry by navController.currentBackStackEntryAsState()
    return entry?.destination?.route
}

private data class BottomItem(
    val route: String,
    val icon: ImageVector,
    val label: String,
    val badgeCount: Int? = null
)

private val bottomItems = listOf(
    BottomItem(MenuTabs.HOME, Icons.Outlined.Home, "首頁"),
    BottomItem(MenuTabs.MAP, Icons.Outlined.Map, "地圖"),
    BottomItem(MenuTabs.CHAT, Icons.Outlined.ChatBubbleOutline, "聊天"),
    BottomItem(MenuTabs.PROFILE, Icons.Outlined.Person, "個人")
)

// ========= BottomBar 本體 =========

@Composable
private fun BottomBar(navController: NavHostController) {
    val route = currentRoute(navController)

    Surface(
        color = Color.White.copy(alpha = 0.75f),
        tonalElevation = 12.dp,
        shadowElevation = 16.dp,
        shape = MaterialTheme.shapes.large.copy(all = CornerSize(24.dp)),
        modifier = Modifier.padding(
            start = 24.dp,
            end = 24.dp,
            bottom = 12.dp
        )
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier
                .height(52.dp)
                .padding(horizontal = 8.dp)
        ) {
            bottomItems.forEach { item ->
                val selected = route == item.route

                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (item.route == MenuTabs.HOME) {
                            // 關鍵：不管現在在 run_mode / run_solo / run_multi / 其他 tab
                            // 一律把 stack 彈回 menu_home
                            navController.popBackStack(
                                route = MenuTabs.HOME,
                                inclusive = false
                            )
                        } else {
                            navController.navigate(item.route) {
                                launchSingleTop = true
                                popUpTo(MenuTabs.HOME) {
                                    saveState = true
                                }
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        AnimatedIcon(
                            selected = selected,
                            icon = item.icon,
                            contentDescription = item.label
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        unselectedIconColor = Color(0xFF64748B),
                        indicatorColor = Color.Transparent
                    ),
                    alwaysShowLabel = false
                )
            }
        }
    }
}

// ========= Bottom icon 動畫 =========

@Composable
private fun AnimatedIcon(
    selected: Boolean,
    icon: ImageVector,
    contentDescription: String?
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = tween(
            durationMillis = 180,
            easing = FastOutSlowInEasing
        ),
        label = "icon-scale"
    )

    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier.graphicsLayer(
            scaleX = scale,
            scaleY = scale
        ),
        tint = if (selected) PrimaryBlue else Color(0xFF64748B)
    )
}