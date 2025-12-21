package com.example.amicitia.ui.menu.home.run

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.amicitia.R
import kotlin.random.Random

/* --------- 色彩統一 --------- */

private val BgDark = Color(0xFF1E1E1E)
private val PrimaryBlue = Color(0xFF3F51B5)
private val TitleText = Color.White.copy(alpha = 0.92f)
private val BodyText = Color.White.copy(alpha = 0.68f)

/* --------- Screen --------- */

@Composable
fun RunModeScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .systemBarsPadding()
    ) {
        // ✅ 只保留底部光暈
        BottomDecorBackground(
            modifier = Modifier.matchParentSize(),
            tint = PrimaryBlue
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "選擇跑步模式",
                style = MaterialTheme.typography.headlineSmall,
                color = TitleText
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "開始今天的訓練，選擇適合你的挑戰方式",
                style = MaterialTheme.typography.bodyMedium,
                color = BodyText
            )

            Spacer(Modifier.height(20.dp))

            RunModeGlassCard(
                title = "單人模式 SOLO",
                description = "獨自專注配速與里程，適合個人訓練與測試實力",
                iconRes = R.drawable.ic_run,
                isMulti = false,
                onClick = { navController.navigate("run_solo") }
            )

            Spacer(Modifier.height(14.dp))

            RunModeGlassCard(
                title = "多人模式 MULTI",
                description = "與好友一起開跑，比拼速度與堅持度，增加趣味與動力",
                iconRes = R.drawable.ic_run,
                isMulti = true,
                onClick = { navController.navigate("run_multi") }
            )
        }
    }
}

/* --------- 底部光暈 --------- */

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
                center = Offset(w * 0.5f, h * 0.90f),
                radius = h * 0.78f
            )
        )
    }
}

/* --------- 玻璃卡片 --------- */

@Composable
private fun RunModeGlassCard(
    title: String,
    description: String,
    iconRes: Int,
    isMulti: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .shadow(14.dp, shape, clip = false)
            .clip(shape)
            .drawBehind {
                val r = 24.dp.toPx()

                // 霧面底（避免中間白塊）
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.08f),
                    cornerRadius = CornerRadius(r, r)
                )

                // 斜向高光（很淡）
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    ),
                    cornerRadius = CornerRadius(r, r)
                )

                // 微霧點
                repeat(60) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.02f),
                        radius = Random.nextFloat() * 1.3f + 0.5f,
                        center = Offset(
                            Random.nextFloat() * size.width,
                            Random.nextFloat() * size.height
                        )
                    )
                }

                // 底部厚度
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.22f)
                        ),
                        startY = size.height * 0.25f,
                        endY = size.height
                    ),
                    cornerRadius = CornerRadius(r, r)
                )
            }
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeIcon(iconRes, isMulti, title)

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TitleText
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = BodyText
            )
        }
    }
}

/* --------- Icon --------- */

@Composable
private fun ModeIcon(
    iconRes: Int,
    isMulti: Boolean,
    title: String
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        PrimaryBlue,
                        Color(0xFF6366F1)
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isMulti) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = Color.White.copy(alpha = 0.45f),
                modifier = Modifier
                    .size(25.dp)
                    .offset(x = (-6).dp, y = (-6).dp)
            )
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier
                    .size(31.dp)
                    .offset(x = (6).dp, y = (6).dp)
            )
        } else {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(31.dp)
            )
        }
    }
}