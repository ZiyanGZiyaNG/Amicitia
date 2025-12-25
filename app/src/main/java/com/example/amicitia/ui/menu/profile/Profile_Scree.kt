package com.example.amicitia.ui.menu.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.amicitia.nav.Routes
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale

private val BgDark = Color(0xFF1E1E1E)
private val PrimaryBlue = Color(0xFF3F51B5)

// ✅ 灰色系（Setting 選單也改灰底）
private val SolidGray = Color(0xFF2A2A2A)
private val RowGray = Color(0xFF242424)

private val TitleText = Color.White.copy(alpha = 0.92f)
private val BodyText = Color.White.copy(alpha = 0.70f)
private val MutedText = Color.White.copy(alpha = 0.65f)
private val DividerColor = Color.White.copy(alpha = 0.16f)

// Sheet 文字色
private val SheetTitleColor = Color.White.copy(alpha = 0.92f)
private val SheetRowText = Color.White.copy(alpha = 0.90f)

// ✅ 不改你的 icon 顏色：仍是 PrimaryBlue
private val SheetRowIcon = PrimaryBlue

// ✅ Row/卡片底色改灰（不要玻璃白霧）
private val SheetRowBg = SolidGray
private val SheetRowBorder = Color.White.copy(alpha = 0.10f)

// 跟 Chat 頁一致的頭像底色
private val AvatarBg = Color(0xFF3A3A3A)

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
    var avatarUrl by remember { mutableStateOf<String?>(null) } // ✅ 新增：抓 avatarUrl
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
                    avatarUrl = doc.getString("avatarUrl") // ✅ 新增
                    @Suppress("UNCHECKED_CAST")
                    recentActivities = doc.get("recentActivities") as? List<String> ?: emptyList()
                    totalMinutes = doc.getLong("totalExerciseMinutes") ?: 0L
                    streakDays = doc.getLong("streakDays") ?: 0L
                }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AuthBackground(modifier = Modifier.matchParentSize())

        Box(
            modifier = Modifier
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // ✅ 原本的 Image placeholder 改成跟 Chat 一樣的頭像樣式
                    ProfileAvatarCircle(
                        nickname = nickname ?: "使用者",
                        avatarUrl = avatarUrl.orEmpty(), // 先保留，暫不載圖
                        size = 96.dp
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
                        color = TitleText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = bio.takeUnless { it.isNullOrBlank() } ?: "這位使用者還沒有自我介紹。",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = BodyText
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(
                    thickness = 1.dp,
                    color = DividerColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "最近運動",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    ),
                    color = TitleText,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (recentActivities.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暫無運動紀錄",
                            color = BodyText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        recentActivities.forEach { activity ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = RowGray,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = activity,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                    color = SheetRowText,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

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
                    tint = PrimaryBlue,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState,
            containerColor = Color.Transparent,
            scrimColor = Color.Black.copy(alpha = 0.45f)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(BgDark)
                )
                BottomDecorBackground(
                    modifier = Modifier.matchParentSize(),
                    tint = PrimaryBlue
                )

                Surface(
                    color = SolidGray,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "設定",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            ),
                            color = SheetTitleColor,
                            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                        )

                        SettingRow("帳號與安全", Icons.Rounded.Lock) {
                            showSettingsSheet = false
                            outerNavController.navigate(Routes.ACCOUNT)
                        }
                        SettingRow("通知偏好", Icons.Rounded.Notifications) {
                            showSettingsSheet = false
                            outerNavController.navigate(Routes.NOTIFY)
                        }
                        SettingRow("隱私與可見度", Icons.Rounded.Visibility) {
                            showSettingsSheet = false
                            outerNavController.navigate(Routes.PRIVACY)
                        }
                        SettingRow("關於我們", Icons.Rounded.Info) {
                            showSettingsSheet = false
                            outerNavController.navigate(Routes.ABOUT)
                        }
                        SettingRow("登出", Icons.AutoMirrored.Rounded.Logout) {
                            showSettingsSheet = false
                            Firebase.auth.signOut()
                            onLogout()
                            outerNavController.navigate(Routes.LOGIN) { launchSingleTop = true }
                        }

                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}


@Composable
private fun ProfileAvatarCircle(
    nickname: String,
    avatarUrl: String,
    size: androidx.compose.ui.unit.Dp
) {
    val initial = nickname.trim().firstOrNull()?.toString()?.uppercase(Locale.getDefault()) ?: "?"

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(AvatarBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = Color.White
        )
    }
}


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
private fun StatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp
            ),
            color = TitleText
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            ),
            color = MutedText
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SheetRowBg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, SheetRowBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SheetRowIcon
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = SheetRowText
            )
            Spacer(Modifier.weight(1f))
        }
    }
}