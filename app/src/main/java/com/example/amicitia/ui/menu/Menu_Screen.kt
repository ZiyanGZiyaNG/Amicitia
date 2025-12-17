package com.example.amicitia.ui.menu

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import com.example.amicitia.nav.Routes
import com.example.amicitia.ui.menu.chat.ChatRoute
import com.example.amicitia.ui.menu.home.HomeRoute
import com.example.amicitia.ui.menu.home.run.MultiRunScreen
import com.example.amicitia.ui.menu.home.run.RunModeScreen
import com.example.amicitia.ui.menu.home.run.RunSoloScreen
import com.example.amicitia.ui.menu.map.MapRoute
import com.example.amicitia.ui.menu.profile.ProfileRoute
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

private val PrimaryPurple = Color(0xFF4F46E5)

@Composable
private fun AnimatedGradientBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "menu_bg")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "menu_pulse"
    )

    val drift by infiniteTransition.animateFloat(
        initialValue = -0.04f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "menu_drift"
    )

    Box(
        modifier = modifier.drawBehind {
            val minDim = size.minDimension
            drawCircle(
                color = Color(0x667C3AED),
                radius = minDim * 0.45f * pulse,
                center = Offset(
                    x = size.width * (0.0f + drift),
                    y = size.height * (0.12f + drift * 0.5f)
                )
            )
            drawCircle(
                color = Color(0x664F46E5),
                radius = minDim * 0.55f * pulse,
                center = Offset(
                    x = size.width * (1.15f - drift * 0.5f),
                    y = size.height * (0.95f - drift)
                )
            )
        }
    )
}

private object MenuTabs {
    const val HOME = "menu_home"
    const val MAP = "menu_map"
    const val CHAT = "menu_chat"
    const val PROFILE = "menu_profile"
}

@Composable
fun MenuScreen(outerNavController: NavController) {
    val innerNav: NavHostController = rememberNavController()
    val auth = Firebase.auth

    val handleLogout: () -> Unit = remember {
        {
            auth.signOut()
            outerNavController.navigate(Routes.LOGIN) {
                popUpTo(outerNavController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
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
            val route = currentRoute(innerNav)
            val contentModifier =
                if (route == MenuTabs.MAP) Modifier.fillMaxSize()
                else Modifier.fillMaxSize().padding(innerPadding)

            MenuNavHost(
                navController = innerNav,
                outerNavController = outerNavController,
                modifier = contentModifier,
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
                    when (sportKey) {
                        "run" -> navController.navigate("run_mode")
                        else -> {}
                    }
                }
            )
        }

        composable(MenuTabs.MAP) { MapRoute() }
        composable(MenuTabs.CHAT) { ChatRoute() }

        composable(MenuTabs.PROFILE) {
            ProfileRoute(
                outerNavController = outerNavController,
                onLogout = onLogout
            )
        }

        composable("run_mode") { RunModeScreen(navController) }
        composable("run_solo") { RunSoloScreen(navController = navController) }
        composable("run_multi") { MultiRunScreen(navController) }
    }
}

@Composable
private fun currentRoute(navController: NavHostController): String? {
    val entry by navController.currentBackStackEntryAsState()
    return entry?.destination?.route
}

private data class BottomItem(val route: String, val icon: ImageVector, val label: String)

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
        color = Color.White,
        tonalElevation = 12.dp,
        shadowElevation = 16.dp,
        shape = MaterialTheme.shapes.large.copy(all = CornerSize(24.dp)),
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier.height(52.dp).padding(horizontal = 8.dp)
        ) {
            bottomItems.forEach { item ->
                val selected = route == item.route

                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (item.route == MenuTabs.HOME) {
                            navController.popBackStack(MenuTabs.HOME, inclusive = false)
                        } else {
                            navController.navigate(item.route) {
                                launchSingleTop = true
                                popUpTo(MenuTabs.HOME) { saveState = true }
                                restoreState = true
                            }
                        }
                    },
                    icon = { AnimatedIcon(selected, item.icon, item.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryPurple,
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
private fun AnimatedIcon(selected: Boolean, icon: ImageVector, contentDescription: String?) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "icon-scale"
    )

    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        tint = if (selected) PrimaryPurple else Color(0xFF64748B)
    )
}