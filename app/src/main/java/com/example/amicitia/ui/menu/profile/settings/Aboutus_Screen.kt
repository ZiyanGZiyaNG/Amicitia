package com.example.amicitia.ui.menu.profile.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.amicitia.R
import kotlin.math.PI
import kotlin.math.cos

private val BgDark = Color(0xFF1E1E1E)
private val PrimaryBlue = Color(0xFF3F51B5)

private val TitleText = Color.White.copy(alpha = 0.92f)
private val BodyText = Color.White.copy(alpha = 0.72f)

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
        AuthBackground(Modifier.matchParentSize())

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "關於我們",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TitleText
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = TitleText
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TitleText,
                        navigationIconContentColor = TitleText
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
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = BodyText
                    )
                }

                item {
                    Text(
                        text = "開發者們",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = TitleText
                    )
                }

                val members = listOf(
                    DevMember("Ziyang", "主導手機軟題化的人。哈哈屁眼", R.drawable.ziyang),
                    DevMember(
                        "泥巴",
                        "你說得對，但是你說的也不完全對。從某種角度來說，你說的有一點對，可是從另一個角度看，你說得不對。也不能說是完全不對，只能說離完全對之間還有一點不對。如果忽略這點不對，那你說的當然是對的，可是以一個更嚴謹的態度去審視你說的對不對，那麼你說的又不是對的了。",
                        R.drawable.dirt
                    ),
                    DevMember("Jason", "《關於我這隻Sb綠豬可能要脫單這件事》團隊主輔 vibe coder + 一個完全不稱職的隊長", R.drawable.json),
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
    val shape = RoundedCornerShape(18.dp)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 10.dp
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
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    ),
                    color = TitleText
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = member.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 3
                )
            }
        }
    }
}

/* ---------------- 深色背景（BgDark + 底部光暈 + 很淡霧面層） ---------------- */

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
    // ✅ 動畫必須在 Composable context 先算好，再丟給 Canvas 畫（不能放進 Canvas{} 裡）
    val infinite = rememberInfiniteTransition(label = "about_bg")
    val tRaw by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "about_bg_t"
    )
    val t = (1f - cos(tRaw * PI).toFloat()) / 2f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 底部藍光暈（你原本那個）
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

        // 超淡霧面層（只讓背景不死黑，不會糊內容）
        val c1 = Color(0xFF0B1220).copy(alpha = 0.18f + 0.06f * t)
        val c2 = Color(0xFF111827).copy(alpha = 0.18f + 0.06f * (1f - t))
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(c1, c2),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )
    }
}