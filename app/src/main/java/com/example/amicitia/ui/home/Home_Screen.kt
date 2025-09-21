package com.example.amicitia.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

// 定義運動種類
enum class Sport(val label: String) {
    Soccer("足球"),
    Basketball("籃球"),
    Baseball("棒球"),
    Badminton("羽球"),
    Tennis("網球"),
    Running("跑步"),
    Swimming("游泳")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selected by remember { mutableStateOf<Sport?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    fun selectSport(s: Sport) {
        selected = s
        scope.launch { snackbarHostState.showSnackbar("已選：${s.label}") }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(selected?.label ?: "選擇運動") },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        Sport.values().forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.label) },
                                onClick = {
                                    menuExpanded = false
                                    selectSport(s)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("快速選擇", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // 橫向可滑動 Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Sport.values().size) { idx ->
                    val s = Sport.values()[idx]
                    FilterChip(
                        selected = selected == s,
                        onClick = { selectSport(s) },
                        label = { Text(s.label) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            if (selected == null) {
                Text("還沒選擇運動，從上方選單或下方 Chips 選一個吧。")
            } else {
                Text("目前選擇：${selected!!.label}", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}