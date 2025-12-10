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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.amicitia.R
import com.example.amicitia.nav.Routes
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRoute(
    outerNavController: NavController,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val user = Firebase.auth.currentUser
    var nickname by remember { mutableStateOf<String?>(null) }
    var bio by remember { mutableStateOf<String?>(null) }
    var recentActivities by remember { mutableStateOf(listOf<String>()) }
    var totalMinutes by remember { mutableStateOf(0L) }
    var streakDays by remember { mutableStateOf(0L) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSettingsSheet by remember { mutableStateOf(false) }

    val gearSize = 28.dp
    val gearMargin = 12.dp
    val contentTopPadding = gearSize + gearMargin * 2

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentTopPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 頭像 + 統計
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
                    modifier = Modifier.padding(end = contentTopPadding / 2)
                ) {
                    StatItem(title = "運動紀錄", value = recentActivities.size.toString())
                    StatItem(title = "總時間", value = totalMinutes.toString())
                    StatItem(title = "連續運動", value = streakDays.toString())
                }
            }

            // 暱稱 + 自介
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = nickname ?: "未設定暱稱",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bio.takeUnless { it.isNullOrBlank() } ?: "這位使用者還沒有自我介紹。",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 最近運動標題
            Text(
                text = "最近運動",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 最近運動內容 / 空狀態
            if (recentActivities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暫無運動紀錄",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp
                        )
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
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // 右上角設定 icon
        IconButton(
            onClick = { showSettingsSheet = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .zIndex(2f)
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "設定",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }

    // 底部設定 Sheet
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
                AnimatedGradientBackground(modifier = Modifier.matchParentSize())

                Surface(
                    color = Color.White.copy(alpha = 0.28f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.matchParentSize()
                ) {}

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        "設定",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    SettingRow("帳號與安全", Icons.Rounded.Lock) {
                        outerNavController.navigate(Routes.ACCOUNT)
                    }
                    SettingRow("通知偏好", Icons.Rounded.Notifications) {
                        outerNavController.navigate(Routes.NOTIFY)
                    }
                    SettingRow("隱私與可見度", Icons.Rounded.Visibility) {
                        outerNavController.navigate(Routes.PRIVACY)
                    }
                    SettingRow("關於我們", Icons.Rounded.Info) {
                        outerNavController.navigate(Routes.ABOUT)
                    }
                    SettingRow("登出", Icons.Rounded.Logout) {
                        showSettingsSheet = false
                        Firebase.auth.signOut()
                    }

                    Spacer(Modifier.height(6.dp))
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
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            ),
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
            Icon(imageVector = icon, contentDescription = null, tint = iconTint)
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
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
    val t = (1f - kotlin.math.cos(tRaw * Math.PI).toFloat()) / 2f
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