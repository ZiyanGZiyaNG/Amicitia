package com.example.amicitia.ui.menu.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.SportsBasketball
import androidx.compose.material.icons.rounded.SportsHandball
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material.icons.rounded.SportsVolleyball
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.amicitia.R
import com.example.amicitia.SportStats
import com.example.amicitia.SportsRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

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

private data class SportItem(
    val key: String,          // Firestore document id
    val icon: ImageVector,    // 預設 icon（badminton 另處理）
    val label: String
)

private val sports = listOf(
    SportItem("tennis", Icons.Rounded.SportsTennis, "網球"),
    SportItem("run", Icons.Rounded.DirectionsRun, "跑步"),
    SportItem("basketball", Icons.Rounded.SportsBasketball, "籃球"),
    SportItem("soccer", Icons.Rounded.SportsSoccer, "足球"),
    SportItem("volleyball", Icons.Rounded.SportsVolleyball, "排球"),
    SportItem("badminton", Icons.Rounded.SportsHandball, "羽球")
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onSportSelected: (String) -> Unit = {}
) {
    val repo = remember { SportsRepository() }
    val auth = Firebase.auth

    var statsMap by remember { mutableStateOf<Map<String, SportStats>>(emptyMap()) }

    // 進入畫面時讀取所有運動分數
    LaunchedEffect(auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        val result = mutableMapOf<String, SportStats>()
        for (sport in sports) {
            val stat = repo.getSportStats(uid, sport.key)
            result[sport.key] = stat
        }
        statsMap = result
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            text = "選擇你的運動",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(12.dp))

        // 下面這整塊平均填滿整個畫面
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            sports.forEachIndexed { index, sport ->
                val stats = statsMap[sport.key]
                val score = stats?.totalScore ?: 1000.0

                ElevatedCard(
                    onClick = { onSportSelected(sport.key) },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color(0xFFF7F7FA)
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 左邊：icon + 運動名稱
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (sport.key == "badminton") {
                                Icon(
                                    painter = painterResource(id = R.drawable.badminton),
                                    contentDescription = sport.label,
                                    modifier = Modifier.size(38.dp),
                                    tint = Color.Unspecified
                                )
                            } else {
                                Icon(
                                    imageVector = sport.icon,
                                    contentDescription = sport.label,
                                    modifier = Modifier.size(38.dp),
                                    tint = Color(0xFF222222)
                                )
                            }

                            Text(
                                text = sport.label,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFF222222)
                            )
                        }

                        // 右邊：分數
                        Text(
                            text = "分數 ${score.toInt()}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize * 1.05f
                            ),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // 卡片之間的間距（最後一張就不要）
                if (index < sports.lastIndex) {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}