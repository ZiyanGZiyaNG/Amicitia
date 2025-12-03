package com.example.amicitia.ui.menu.profile.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlin.math.PI
import kotlin.math.cos
import com.example.amicitia.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutUsScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        AnimatedGradientBackground(
            aStart = Color(0xFFF3F6FF),
            aMid   = Color(0xFFEAF1FF),
            aEnd   = Color(0xFFDDE7FF),
            bStart = Color(0xFFE8F0FF),
            bMid   = Color(0xFFD6E3FF),
            bEnd   = Color(0xFFCBD9FF),
            durationMs = 4000,
            modifier = Modifier.matchParentSize()
        )

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("關於我們") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "我們是一群高中生，彼此都喜愛著寫程式和各種競賽。" +
                                "這個 Amicitia 的企劃發想是從 2025 年暑假的某一場黑客松所誕生的想法。" +
                                "雖然那次比賽我們沒獲得評審青睞，但我們仍覺得這個創意很有意義，" +
                                "因此在賽後決定把這個專案實作出來。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                item {
                    Text(
                        text = "開發者們",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val members = listOf(
                    DevMember("Ziyang", "主導手機軟題化的人。哈哈屁眼", R.drawable.ziyang),
                    DevMember("泥巴", "你說得對，但是你說的也不完全對。從某種角度來說，你說的有一點對，可是從另一個角度看，你說得不對。也不能說是完全不對，只能說離完全對之間還有一點不對。如果忽略這點不對，那你說的當然是對的，可是以一個更嚴謹的態度去審視你說的對不對，那麼你說的又不是對的了。 ", R.drawable.dirt),
                    DevMember("Jason", "《關於我這隻Sb綠豬可能要脫單這件事》團隊主輔 vibe coder +一個完全不稱職的隊長", R.drawable.json),
                    DevMember("Mina", "(陰暗的爬行)(尖叫)(扭曲)(陰暗的爬行)(尖叫)(扭曲)(陰暗的爬行)(尖叫)(爬行))扭動)(分裂)(陰暗地蠕動)", R.drawable.ziyang),
                    DevMember("Leo", "效能優化、動畫與可用性測試", R.drawable.ziyang),
                    DevMember("Iris", "文件、README 與 CI 配置", R.drawable.ziyang),
                    DevMember("Ken", "NavHost 架構與模組化", R.drawable.ziyang),
                    DevMember("Amy", "色彩系統與字體規範", R.drawable.ziyang)
                )

                itemsIndexed(members) { _, m ->
                    DeveloperCard(member = m)
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }
}

data class DevMember(
    val name: String,
    val description: String,
    val imageRes: Int?
)

@Composable
private fun DeveloperCard(member: DevMember) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            // 卡片改為不透明表面顏色
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (member.imageRes != null) {
                Image(
                    painter = painterResource(id = member.imageRes),
                    contentDescription = member.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            } else {
                // 改這段：整塊灰圓形、不透明
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFFCACDD6), CircleShape) // 深灰藍感
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = member.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    maxLines = 3
                )
            }
        }
    }
}

@Composable
private fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    aStart: Color = Color(0xFFF3F6FF),
    aMid:   Color = Color(0xFFEAF1FF),
    aEnd:   Color = Color(0xFFDDE7FF),
    bStart: Color = Color(0xFFE8F0FF),
    bMid:   Color = Color(0xFFD6E3FF),
    bEnd:   Color = Color(0xFFCBD9FF),
    durationMs: Int = 4000
) {
    val infinite = rememberInfiniteTransition()
    val tRaw by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val t = (1f - cos(tRaw * PI).toFloat()) / 2f
    val c1 = lerp(aStart, bStart, t)
    val c2 = lerp(aMid, bMid, t)
    val c3 = lerp(aEnd, bEnd, t)

    Box(
        modifier = modifier.drawBehind {
            val sx = size.width * (0.15f + 0.35f * t)
            val sy = size.height * (0.10f + 0.25f * (1f - t))
            val ex = size.width * (0.85f - 0.35f * t)
            val ey = size.height * (0.90f - 0.25f * (1f - t))
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(c1, c2, c3),
                    start = Offset(sx, sy),
                    end = Offset(ex, ey)
                )
            )
        }
    )
}