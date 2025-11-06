package com.example.amicitia.ui.menu.profile

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.amicitia.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRoute(
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit = {}, // 保留給未來導頁用，現在用 BottomSheet
    modifier: Modifier = Modifier
) {
    val user = Firebase.auth.currentUser
    var nickname by remember { mutableStateOf<String?>(null) }
    var bio by remember { mutableStateOf<String?>(null) }
    var recentActivities by remember { mutableStateOf(listOf<String>()) }
    var totalMinutes by remember { mutableStateOf(0L) }
    var streakDays by remember { mutableStateOf(0L) }

    // BottomSheet 狀態
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSettingsSheet by remember { mutableStateOf(false) }

    // 齒輪尺寸＋緩衝 → 讓內容整體往下，避免重疊
    val gearSize = 28.dp
    val gearMargin = 12.dp
    val contentTopPadding = gearSize + gearMargin * 2 // ≈ 52.dp

    // 讀取 Firestore
    LaunchedEffect(user?.uid) {
        user?.uid?.let { uid ->
            Firebase.firestore.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    nickname = doc.getString("nickname")
                    bio = doc.getString("bio")
                    @Suppress("UNCHECKED_CAST")
                    recentActivities = doc.get("recentActivities") as? List<String> ?: emptyList()
                    totalMinutes = doc.getLong("totalExerciseMinutes") ?: 0L
                    streakDays = doc.getLong("streakDays") ?: 0L
                }
        }
    }

    // ===== 主畫面 =====
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // 內容：整體往下墊高
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentTopPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header：頭像 + 三個統計
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_profile_placeholder),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = contentTopPadding / 2) // 靠右但不撞齒輪
                ) {
                    StatItem(title = "運動紀錄", value = recentActivities.size.toString())
                    StatItem(title = "總時間", value = totalMinutes.toString()) // 不加「分」
                    StatItem(title = "連續運動", value = streakDays.toString())
                }
            }

            // 暱稱 / 自介
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = nickname ?: "未設定暱稱",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bio.takeUnless { it.isNullOrBlank() } ?: "這位使用者還沒有自我介紹。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // 最近運動
            Text(
                text = "最近運動",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (recentActivities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暫無運動紀錄",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    recentActivities.forEach { activity ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = activity,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 登出
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("登出", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
        }

        // 右上角齒輪：提高 zIndex，確保可點
        IconButton(
            onClick = { showSettingsSheet = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(gearSize)
                .zIndex(2f)
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "設定",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    // ===== 設定 BottomSheet：動態漸層 + 玻璃質感，與全站一致 =====
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState,
            containerColor = Color.Transparent,
            scrimColor = Color.Black.copy(alpha = 0.25f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // 動態漸層背景
                AnimatedGradientBackground(modifier = Modifier.matchParentSize())

                // 霧面覆層 + 圓角
                Surface(
                    color = Color.White.copy(alpha = 0.28f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.matchParentSize()
                ) {}

                // 內容
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        "設定",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    SettingRow("帳號與安全", Icons.Rounded.Lock) { /* TODO */ }
                    SettingRow("通知偏好", Icons.Rounded.Notifications) { /* TODO */ }
                    SettingRow("隱私與可見度", Icons.Rounded.PrivacyTip) { /* TODO */ }
                    SettingRow("主題", Icons.Rounded.DarkMode) { /* TODO */ }
                    SettingRow("關於我們", Icons.Rounded.Info) { /* TODO */ }

                    Spacer(Modifier.height(6.dp)) // 沒有「關閉」按鈕；用下滑或點外面關閉
                }
            }
        }
    }
}

@Composable
private fun StatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val glass = Color.White.copy(alpha = 0.55f)
    val border = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val iconTint = MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = glass,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconTint)
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

/* ===== 共用：動態漸層背景（與登入/首頁一致） ===== */
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
    val t = (1f - kotlin.math.cos(tRaw * Math.PI).toFloat()) / 2f
    val c1 = lerp(aStart, bStart, t)
    val c2 = lerp(aMid,   bMid,   t)
    val c3 = lerp(aEnd,   bEnd,   t)

    Box(
        modifier = modifier.drawBehind {
            val sx = size.width  * (0.15f + 0.35f * t)
            val sy = size.height * (0.10f + 0.25f * (1f - t))
            val ex = size.width  * (0.85f - 0.35f * t)
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