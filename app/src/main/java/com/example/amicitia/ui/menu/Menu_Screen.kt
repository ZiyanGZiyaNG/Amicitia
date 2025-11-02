package com.example.amicitia.ui.menu

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.amicitia.ui.menu.chat.ChatRoute
import com.example.amicitia.ui.menu.home.HomeRoute
import com.example.amicitia.ui.menu.map.MapRoute
import com.example.amicitia.ui.menu.profile.ProfileRoute
import com.example.amicitia.presence.PresenceManager
import com.example.amicitia.nav.Routes
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ktx.firestore

// 內層 tab 用的 route key
private object MenuTabs {
    const val HOME = "menu_home"
    const val MAP = "menu_map"
    const val CHAT = "menu_chat"
    const val PROFILE = "menu_profile"
}

@Composable
fun MenuScreen(
    outerNavController: NavController
)
{
    val innerNav: NavHostController = rememberNavController()

    val auth = Firebase.auth
    val db = com.google.firebase.ktx.Firebase.firestore
    val uid = auth.currentUser?.uid

    val presenceManager = remember(uid)
    {
        if (uid != null) PresenceManager(uid, db) else null
    }

    val handleLogout: () -> Unit =
    {
        presenceManager?.stop()

        auth.signOut()

        outerNavController.navigate(Routes.LOGIN)
        {
            popUpTo(Routes.MENU) { inclusive = true }
            launchSingleTop = true
        }
    }

    Scaffold(
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
        composable(MenuTabs.HOME)
        {
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
                onLogout = onLogout
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
    BottomItem(MenuTabs.CHAT, Icons.Outlined.ChatBubbleOutline, "聊天", badgeCount = null),
    BottomItem(MenuTabs.PROFILE, Icons.Outlined.Person, "個人")
)

@Composable
private fun BottomBar(navController: NavHostController) {
    val route = currentRoute(navController)

    Surface(
        color = Color.White.copy(alpha = 0.6f),
        tonalElevation = 12.dp,
        shadowElevation = 16.dp,
        shape = MaterialTheme.shapes.large.copy(
            all = androidx.compose.foundation.shape.CornerSize(24.dp)
        ),
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
                        Box(Modifier.padding(bottom = 2.dp)) {
                            val iconContent = @Composable {
                                AnimatedIcon(
                                    selected = selected,
                                    icon = item.icon,
                                    contentDescription = item.label
                                )
                            }

                            val count = item.badgeCount
                            if (count != null && count > 0) {
                                BadgedBox(badge = { Badge { Text("$count") } }) {
                                    iconContent()
                                }
                            } else {
                                iconContent()
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        unselectedIconColor = Color.Gray,
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
        )
    )
}