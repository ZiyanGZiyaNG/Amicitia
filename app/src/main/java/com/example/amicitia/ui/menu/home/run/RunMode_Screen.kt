package com.example.amicitia.ui.menu.home.run

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.amicitia.R

@Composable
fun RunModeScreen(navController: NavController) {

    val infiniteTransition = rememberInfiniteTransition(label = "run_bg")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val drift by infiniteTransition.animateFloat(
        initialValue = -0.04f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val minDim = size.minDimension

                drawCircle(
                    color = Color(0x667C3AED),
                    radius = minDim * 0.45f * pulse,
                    center = Offset(
                        x = size.width * (0.0f + drift),
                        y = size.height * (0.12f + drift * 0.5f)
                    )
                )

                drawCircle(
                    color = Color(0x664F46E5),
                    radius = minDim * 0.55f * pulse,
                    center = Offset(
                        x = size.width * (1.15f - drift * 0.5f),
                        y = size.height * (0.95f - drift)
                    )
                )
            }
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "選擇跑步模式",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "開始今天的訓練，選擇適合你的挑戰方式",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280)
            )

            Spacer(modifier = Modifier.height(40.dp))

            RunModeCard(
                title = "單人模式 SOLO",
                description = "獨自專注配速與里程，適合個人訓練與測試實力",
                iconRes = R.drawable.ic_run,
                isMulti = false,
                onClick = { navController.navigate("run_solo") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            RunModeCard(
                title = "多人模式 MULTI",
                description = "與好友一起開跑，比拼速度與堅持度，增加趣味與動力",
                iconRes = R.drawable.ic_run,
                isMulti = true,
                onClick = { navController.navigate("run_multi") }
            )
        }
    }
}

@Composable
private fun RunModeCard(
    title: String,
    description: String,
    iconRes: Int,
    isMulti: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 14.dp,
        color = Color.White.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Icon container
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF4F46E5),
                                Color(0xFF6366F1)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                if (isMulti) {
                    // 後面淡色的小 icon（多人模式）
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(26.dp)
                            .offset(x = (-6).dp, y = (-6).dp)
                    )

                    // 前面主要 icon
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .offset(x = (6).dp, y = (6).dp)
                    )

                } else {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}