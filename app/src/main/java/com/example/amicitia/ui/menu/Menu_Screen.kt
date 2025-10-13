package com.example.amicitia.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.navigation.NavController
import com.example.amicitia.nav.Routes

private val PrimeColor = Color(0xFF3F51B5)
private val PrimaryBlue = Color(0xFF1E88E5)

/** 會自動判斷是否顯示返回鍵的 TopBar **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(
    title: String,
    navController: NavController,
    showBackOverride: Boolean? = null,         // 可強制顯/隱，預設自動判斷
    actions: @Composable RowScope.() -> Unit = {}
) {
    val canGoBack = navController.previousBackStackEntry != null
    val showBack = showBackOverride ?: canGoBack

    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFFF5F7FA),
            navigationIconContentColor = Color(0xFF4A4A4A),
            titleContentColor = Color(0xFF1F2937)
        )
    )
}

/** 一般頁面要回登入就用 Routes.LOGIN，不要用字串 "LoginScreen" **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(navController: NavController) {
    Scaffold(
        topBar = {
            BackTopBar(
                title = "標題",
                navController = navController,
                showBackOverride = true // 這頁確實有上一頁要顯示返回
            )
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            // 內容
        }
    }
}

/** Menu 作為登入後首頁：不顯示返回鍵，放一個登出在右上角（可選） **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(navController: NavController) {
    Scaffold(
        topBar = {
            BackTopBar(
                title = "選單",
                navController = navController,
                showBackOverride = false, // 首頁不顯示返回鍵
                actions = {
                    TextButton(
                        onClick = {
                            Firebase.auth.signOut()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.MENU) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    ) { Text("登出") }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFEFF6FF))
        ) {
            // Menu 畫面的內容
        }
    }
}