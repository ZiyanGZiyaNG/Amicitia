package com.example.amicitia.ui.menu.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.amicitia.R
import com.example.amicitia.SportStats
import com.example.amicitia.SportsRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlin.random.Random

private val BgDark = Color(0xFF1E1E1E)
private val PrimaryBlue = Color(0xFF3F51B5)

@Composable
private fun AuthBackground(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(BgDark)
    ) {
        BottomDecorBackground(
            modifier = Modifier.matchParentSize(),
            tint = PrimaryBlue
        )
    }
}

@Composable
private fun BottomDecorBackground(
    modifier: Modifier = Modifier,
    tint: Color
) {
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    tint.copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.88f),
                radius = h * 0.75f
            )
        )
    }
}


@Composable
private fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerDp: Dp = 24.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerDp)

    Row(
        modifier = modifier
            .shadow(14.dp, shape, clip = false)
            .clip(shape)
            .drawBehind {
                val r = cornerDp.toPx()

                drawRoundRect(
                    color = Color.White.copy(alpha = 0.22f),
                    cornerRadius = CornerRadius(r, r)
                )

                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    cornerRadius = CornerRadius(r, r)
                )

                repeat(80) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.03f),
                        radius = Random.nextFloat() * 1.5f + 0.5f,
                        center = Offset(
                            Random.nextFloat() * size.width,
                            Random.nextFloat() * size.height
                        )
                    )
                }
            }
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

data class SportMeta(
    val key: String,
    val name: String,
    val icon: Painter
)


@Composable
fun HomeRoute(
    onSportSelected: (String) -> Unit
) {
    val repo = remember { SportsRepository() }
    val uid = Firebase.auth.currentUser?.uid
    var statsMap by remember { mutableStateOf<Map<String, SportStats>>(emptyMap()) }

    val sports = listOf(
        SportMeta("tennis", "網球", painterResource(R.drawable.ic_tennis)),
        SportMeta("run", "跑步", painterResource(R.drawable.ic_run)),
        SportMeta("basketball", "籃球", painterResource(R.drawable.ic_basketball)),
        SportMeta("football", "足球", painterResource(R.drawable.ic_football)),
        SportMeta("volleyball", "排球", painterResource(R.drawable.ic_volleyball)),
        SportMeta("badminton", "羽球", painterResource(R.drawable.ic_badminton))
    )

    LaunchedEffect(uid) {
        uid ?: return@LaunchedEffect
        repo.ensureAllSportsExist(uid)
        statsMap = sports.associate { it.key to repo.getSportStats(uid, it.key) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        AuthBackground(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "選擇你的運動",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sports) { sport ->
                    val score = statsMap[sport.key]?.totalScore?.toInt() ?: 1000

                    SportCard(
                        icon = sport.icon,
                        name = sport.name,
                        score = score,
                        onClick = { onSportSelected(sport.key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SportCard(
    icon: Painter,
    name: String,
    score: Int,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clickable { onClick() }
    ) {
        Icon(
            painter = icon,
            contentDescription = name,
            tint = Color(0xFF3F51B5),
            modifier = Modifier.size(44.dp)
        )

        Spacer(Modifier.width(16.dp))

        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "分數 $score",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f)
            )
        }
    }
}