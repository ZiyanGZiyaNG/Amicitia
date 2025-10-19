@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.amicitia.ui.menu

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination

private object Routes {
    const val Home = "home"
    const val Search = "search"
    const val Likes = "likes"
    const val Profile = "profile"
    const val Create = "create"
    const val Login = "login"   // ← 登入頁路由
}

@Composable
fun MenuScreen(navController: NavController) {
    BottomBar(navController)
}

@Composable
private fun BottomBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun navigateSingleTop(route: String) {
        if (currentRoute == route) return
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        floatingActionButton = {
            Surface(
                onClick = { navigateSingleTop(Routes.Create) },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF3F4F6),
                contentColor = Color.Black,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .size(width = 64.dp, height = 40.dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp))
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Add, contentDescription = "新增")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = currentRoute == Routes.Home,
                    onClick = { navigateSingleTop(Routes.Home) },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = "首頁") },
                    label = null,
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.Search,
                    onClick = { navigateSingleTop(Routes.Search) },
                    icon = { Icon(Icons.Outlined.Search, contentDescription = "搜尋") },
                    label = null,
                    alwaysShowLabel = false
                )

                Spacer(Modifier.weight(1f, fill = true))

                NavigationBarItem(
                    selected = currentRoute == Routes.Likes,
                    onClick = { navigateSingleTop(Routes.Likes) },
                    icon = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = "喜歡") },
                    label = null,
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.Profile,
                    onClick = { navigateSingleTop(Routes.Profile) },
                    icon = { Icon(Icons.Outlined.Person, contentDescription = "個人") },
                    label = null,
                    alwaysShowLabel = false
                )
            }
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 反回登入的按鈕
            Button(
                onClick = { navController.navigate(Routes.Login) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "返回登入")
                Spacer(Modifier.width(8.dp))
                Text("返回登入")
            }

            // 目前頁面顯示文字
            Text(
                text = "目前路由：${currentRoute ?: Routes.Home}",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}