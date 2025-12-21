package com.example.amicitia.ui.menu.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.drawWithCache
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
    Box(modifier = modifier.background(BgDark)) {
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
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerDp)

    // ✅ 讓「微粒」固定，不要每次重組都換一張貼圖
    val seed = remember { Random.nextInt() }
    val stableRandom = remember(seed) { Random(seed) }

    val interactionSource = remember { MutableInteractionSource() }
    val glassBase = Color.White.copy(alpha = 0.12f)

    Row(
        modifier = modifier
            // ✅ 陰影交給 shadow，避免 Surface 的狀態層造成「中間淡淡矩形」
            .shadow(
                elevation = 14.dp,
                shape = shape,
                clip = false
            )
            // ✅ 內容裁切成圓角
            .clip(shape)
            // ✅ 玻璃底色
            .background(glassBase)
            // ✅ 關掉 ripple / pressed overlay（矩形來源）
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onClick() }
                } else Modifier
            )
            // ✅ 高光/厚度/微粒都在同一層畫，且已被 clip(shape) 裁切
            .drawWithCache {
                val r = cornerDp.toPx()

                val highlight = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )

                val depth = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.14f)
                    ),
                    startY = size.height * 0.35f,
                    endY = size.height
                )

                val dots = List(60) {
                    Triple(
                        stableRandom.nextFloat() * size.width,
                        stableRandom.nextFloat() * size.height,
                        stableRandom.nextFloat() * 1.4f + 0.4f
                    )
                }

                onDrawBehind {
                    // 斜向高光
                    drawRoundRect(
                        brush = highlight,
                        cornerRadius = CornerRadius(r, r)
                    )

                    // 底部厚度
                    drawRoundRect(
                        brush = depth,
                        cornerRadius = CornerRadius(r, r)
                    )

                    // 微粒
                    dots.forEach { (x, y, rad) ->
                        drawCircle(
                            color = Color.White.copy(alpha = 0.018f),
                            radius = rad,
                            center = Offset(x, y)
                        )
                    }
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
            .height(96.dp),
        onClick = onClick
    ) {
        Icon(
            painter = icon,
            contentDescription = name,
            tint = PrimaryBlue,
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