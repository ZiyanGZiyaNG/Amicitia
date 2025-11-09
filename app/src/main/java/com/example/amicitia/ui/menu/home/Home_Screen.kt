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
    val chunked = remember { sports.chunked(3) }

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
                                    containerColor = Color(0xFFF7F7FA)
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