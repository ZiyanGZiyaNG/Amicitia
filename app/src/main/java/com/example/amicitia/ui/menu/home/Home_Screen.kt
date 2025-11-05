package com.example.amicitia.ui.menu.home
//
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/* -------------------- 公開的 Route：處理點擊邏輯 -------------------- */

@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    onSportSelected: (String) -> Unit
) {
    HomeScreen(
        modifier = modifier,
        onSportSelected = onSportSelected
    )
}

/* -------------------- 純 UI：運動選擇網格 -------------------- */

private data class SportItem(
    val key: String,
    val icon: ImageVector,
    val label: String
)

private val sports = listOf(
    SportItem("tennis", Icons.Outlined.SportsTennis, "網球"),
    SportItem("run", Icons.Outlined.DirectionsRun, "跑步"),
    SportItem("basketball", Icons.Outlined.SportsBasketball, "籃球"),
    SportItem("soccer", Icons.Outlined.SportsSoccer, "足球"),
    SportItem("volleyball", Icons.Outlined.SportsVolleyball, "排球"),
    SportItem("badminton", Icons.Outlined.SportsHandball, "羽球")
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onSportSelected: (String) -> Unit = {}
) {
    val chunked = remember { sports.chunked(3) } // 每排三個

    Column(
        modifier = modifier
            .fillMaxSize()
            // 🔑 不要再設整頁背景色，讓動態漸層透出
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            text = "選擇你的運動",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(12.dp))

        // 下面的區塊佔滿剩下所有高度
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                chunked.forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        rowItems.forEach { sport ->
                            ElevatedCard(
                                onClick = { onSportSelected(sport.key) },
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = Color(0xFFF7F7FA) // 卡片保留淺色，對比漸層
                                ),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = sport.icon,
                                        contentDescription = sport.label,
                                        modifier = Modifier.size(36.dp),
                                        tint = Color(0xFF222222)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = sport.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF222222)
                                    )
                                }
                            }
                        }

                        // 若最後一列不滿三個，補空白
                        repeat(3 - rowItems.size) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}