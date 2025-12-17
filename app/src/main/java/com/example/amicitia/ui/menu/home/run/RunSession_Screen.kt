package com.example.amicitia.ui.menu.home.run

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSessionScreen(
    navController: NavHostController,
    sessionId: String
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("多人跑步房間") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("SessionId：$sessionId", style = MaterialTheme.typography.titleMedium)
            Text("這裡之後接你真正的多人跑步（定位、距離、同步）邏輯。")
        }
    }
}