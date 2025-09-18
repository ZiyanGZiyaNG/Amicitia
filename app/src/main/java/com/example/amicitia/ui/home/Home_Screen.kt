/* 選單 (選運動) */
package com.example.amicitia.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

//
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home_Screen(onLogout: () -> Unit) {
    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("首頁") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("歡迎！註冊/登入成功。")
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onLogout) { Text("登出") }
        }
    }
}

// Wrapper 保持與 MainActivity 呼叫一致
@Composable
fun HomeScreen(onLogout: () -> Unit) = Home_Screen(onLogout)