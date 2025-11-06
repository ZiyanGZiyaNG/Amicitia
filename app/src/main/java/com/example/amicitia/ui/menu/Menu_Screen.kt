package com.example.amicitia.ui.menu

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.amicitia.nav.Routes
import com.example.amicitia.presence.PresenceManager
import com.example.amicitia.ui.menu.chat.ChatRoute
import com.example.amicitia.ui.menu.home.HomeRoute
import com.example.amicitia.ui.menu.map.MapRoute
import com.example.amicitia.ui.menu.profile.ProfileRoute
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ktx.firestore
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos

/* ====== 品牌色系（與登入頁一致） ====== */
private val PrimaryBlue = Color(0xFF3F51B5)

/* ====== 動態漸層背景（與登入頁一致） ====== */
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
            repeatMode = RepeatMode.Reverse
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

/* ====== Menu Tabs ====== */
private object MenuTabs {
    const val HOME = "menu_home"
    const val MAP = "menu_map"
    const val CHAT = "menu_chat"
    const val PROFILE = "menu_profile"
}

@Composable
fun MenuScreen(
    outerNavController: NavController
) {
    val innerNav: NavHostController = rememberNavController()

    val auth = Firebase.auth
    val db = com.google.firebase.ktx.Firebase.firestore
    val uid = auth.currentUser?.uid

    val presenceManager = remember(uid) {
        if (uid != null) PresenceManager(uid, db) else null
    }

    val handleLogout: () -> Unit = {
        presenceManager?.stop()
        auth.signOut()
        outerNavController.navigate(Routes.LOGIN) {
            popUpTo(Routes.MENU) { inclusive = true }
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
                    outerNavController.navigate("sport/$sportKey")
                }
            )
        }
        composable(MenuTabs.MAP) {
            MapRoute()
        }
        composable(MenuTabs.CHAT) {
            ChatRoute(
                onOpenChat = { otherUid ->
                    outerNavController.navigate("chat_room/$otherUid") {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(MenuTabs.PROFILE) {
            ProfileRoute(
                onLogout = onLogout,
                onOpenSettings = { outerNavController.navigate(Routes.SETTINGS) } // ✅ 修好這裡
            )
        }
    }
}

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
                        navController.navigate(item.route) {
                            launchSingleTop = true
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            restoreState = true
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