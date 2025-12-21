package com.example.amicitia.ui.menu

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
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
import com.example.amicitia.ui.menu.chat.ChatNavHost
import com.example.amicitia.ui.menu.home.HomeRoute
import com.example.amicitia.ui.menu.home.run.MultiRunScreen
import com.example.amicitia.ui.menu.home.run.RunModeScreen
import com.example.amicitia.ui.menu.home.run.RunSoloScreen
import com.example.amicitia.ui.menu.map.MapRoute
import com.example.amicitia.ui.menu.profile.ProfileRoute
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

private val BgDark = Color(0xFF1E1E1E)
private val PrimaryBlue = Color(0xFF3F51B5)

private object MenuTabs {
    const val HOME = "menu_home"
    const val MAP = "menu_map"
    const val CHAT = "menu_chat"
    const val PROFILE = "menu_profile"
}

private data class BottomItem(val route: String, val icon: ImageVector, val label: String)

private val bottomItems = listOf(
    BottomItem(MenuTabs.HOME, Icons.Outlined.Home, "首頁"),
    BottomItem(MenuTabs.MAP, Icons.Outlined.Map, "地圖"),
    BottomItem(MenuTabs.CHAT, Icons.Outlined.ChatBubbleOutline, "聊天"),
    BottomItem(MenuTabs.PROFILE, Icons.Outlined.Person, "個人")
)

/* ---------------- Background（跟 Register 一樣：BgDark + 底部光暈） ---------------- */

@Composable
private fun AuthBackground(
    modifier: Modifier = Modifier,
    successProgress: Float = 0f
) {
    Box(
        modifier = modifier.background(BgDark)
    ) {
        BottomDecorBackground(
            modifier = Modifier.matchParentSize(),
            tint = PrimaryBlue,
            alphaFactor = 1f - successProgress
        )
    }
}

@Composable
private fun BottomDecorBackground(
    modifier: Modifier = Modifier,
    tint: Color,
    alphaFactor: Float = 1f
) {
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    tint.copy(alpha = 0.14f * alphaFactor),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.88f),
                radius = h * 0.75f
            )
        )
    }
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
        AuthBackground(modifier = Modifier.matchParentSize())

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = { GlassBottomBar(navController = innerNav) }
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

/* ---------------- NavHost ---------------- */

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
        composable(MenuTabs.CHAT) { ChatNavHost() }

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



@Composable
private fun GlassBottomBar(navController: NavHostController) {
    val route = currentRoute(navController)
    val pillShape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // 外殼：只負責陰影與裁切
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(18.dp, pillShape, clip = false)
                .clip(pillShape)
        ) {
            // ✅ 1) 底層背景板（這層才 blur，裡面不要放 icon/文字）
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.graphicsLayer {
                                renderEffect = RenderEffect
                                    .createBlurEffect(30f, 30f, Shader.TileMode.CLAMP)
                                    .asComposeRenderEffect()
                            }
                        } else Modifier
                    )
                    .drawBehind {
                        val r = 28.dp.toPx()

                        // 霧面底
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.14f),
                            cornerRadius = CornerRadius(r, r)
                        )

                        // 斜向高光
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.20f),
                                    Color.Transparent
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height)
                            ),
                            cornerRadius = CornerRadius(r, r)
                        )

                        // 底部厚度
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.18f)
                                ),
                                startY = size.height * 0.25f,
                                endY = size.height
                            ),
                            cornerRadius = CornerRadius(r, r)
                        )
                    }
                    .border(1.dp, Color.White.copy(alpha = 0.28f), pillShape)
                    .padding(1.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), pillShape)
            )

            // ✅ 2) 上層內容（完全不 blur，所以 icon 不會糊）
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                modifier = Modifier.matchParentSize()
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
                            selectedIconColor = PrimaryBlue,
                            unselectedIconColor = Color.White.copy(alpha = 0.78f),
                            indicatorColor = Color.White.copy(alpha = 0.10f)
                        ),
                        alwaysShowLabel = false
                    )
                }
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
        tint = if (selected) PrimaryBlue else Color.White.copy(alpha = 0.78f)
    )
}