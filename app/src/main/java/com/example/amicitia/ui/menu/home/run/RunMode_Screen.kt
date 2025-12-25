package com.example.amicitia.ui.menu.home.run

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/* --------- 色彩統一 --------- */
private val BgDark = Color(0xFF1E1E1E)
private val PrimaryBlue = Color(0xFF3F51B5)
private val SolidGray = Color(0xFF2A2A2A)
private val SolidBlack = Color(0xFF000000)

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



            RunModeSolidCard(
                title = "單人模式 SOLO",
                description = "獨自專注配速與里程，適合個人訓練與測試實力",
                icon = Icons.Outlined.DirectionsRun,
                badgeText = "SOLO",
                onClick = { navController.navigate("run_solo") }
            )

            Spacer(Modifier.height(14.dp))

            RunModeSolidCard(
                title = "多人模式 MULTI",
                description = "與好友一起開跑，比拼速度與堅持度，增加趣味與動力",
                icon = Icons.Outlined.Groups,
                badgeText = "MULTI",
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

/* --------- Solid Card（同 Home：shadow + clip + 無 ripple） --------- */
@Composable
private fun SolidColorCard(
    modifier: Modifier = Modifier,
    cornerDp: Dp = 24.dp,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerDp)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onClick() }
                } else Modifier
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/* --------- Mode Card（Solid） --------- */
@Composable
private fun RunModeSolidCard(
    title: String,
    description: String,
    icon: ImageVector,
    badgeText: String,
    onClick: () -> Unit
) {
    SolidColorCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp),
        cornerDp = 24.dp,
        contentPadding = 16.dp,
        backgroundColor = SolidGray,
        onClick = onClick
    ) {
        ModeIconOutline(
            icon = icon,
            title = title
        )

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
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

        Spacer(Modifier.width(12.dp))

        PillBadge(text = badgeText)
    }
}

/* --------- 右側 Badge（純黑） --------- */
@Composable
private fun PillBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(SolidBlack)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

/* --------- Icon（改成你要的：線條藍色、乾淨、無漸層背景） --------- */
@Composable
private fun ModeIconOutline(
    icon: ImageVector,
    title: String
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = PrimaryBlue,
            modifier = Modifier.size(32.dp)
        )
    }
}