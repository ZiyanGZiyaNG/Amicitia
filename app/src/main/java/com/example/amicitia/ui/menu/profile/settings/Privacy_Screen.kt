package com.example.amicitia.ui.menu.profile.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.PI
import kotlin.math.cos

private val BgDark = Color(0xFF1E1E1E)
private val PrimaryBlue = Color(0xFF3F51B5)

// ✅ 改成灰色卡片/按鈕/區塊（不動你的 icon tint：PrimaryBlue 仍照常用）
private val SolidGray = Color(0xFF2A2A2A)
private val RowGray = Color(0xFF242424)

private val TitleText = Color.White.copy(alpha = 0.92f)
private val BodyText = Color.White.copy(alpha = 0.82f)
private val MutedText = Color.White.copy(alpha = 0.62f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid

    var showOnline by rememberSaveable { mutableStateOf(true) }
    var allowFriendRequests by rememberSaveable { mutableStateOf(true) }
    var shareActivity by rememberSaveable { mutableStateOf(false) }
    var profileVisibility by rememberSaveable { mutableStateOf("所有人") }

    val visibilityOptions = listOf("所有人", "僅好友", "僅自己")
    var expanded by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uid) {
        if (uid != null) {
            try {
                val doc = db.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    showOnline = doc.getBoolean("showOnline") ?: true
                    allowFriendRequests = doc.getBoolean("allowFriendRequests") ?: true
                    shareActivity = doc.getBoolean("shareActivity") ?: false
                    profileVisibility = when (doc.getString("profileVisibility")) {
                        "friends" -> "僅好友"
                        "private" -> "僅自己"
                        else -> "所有人"
                    }
                }
            } catch (e: Exception) {
                scope.launch { snackbar.showSnackbar("載入失敗：${e.message ?: "未知錯誤"}") }
            }
        } else {
            scope.launch { snackbar.showSnackbar("尚未登入") }
        }
        isLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .systemBarsPadding()
    ) {
        DarkGlowBackground(modifier = Modifier.matchParentSize())

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "隱私與可見度",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            ),
                            color = TitleText
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = TitleText
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    GraySettingCard(title = "可見度設定") {
                        SettingSwitchRow(
                            title = "顯示上線狀態",
                            desc = "讓好友知道你目前是否在線上",
                            checked = showOnline,
                            onCheckedChange = { showOnline = it }
                        )
                        SettingSwitchRow(
                            title = "允許好友邀請",
                            desc = "允許其他使用者傳送好友邀請給你",
                            checked = allowFriendRequests,
                            onCheckedChange = { allowFriendRequests = it }
                        )
                        SettingSwitchRow(
                            title = "分享活動紀錄",
                            desc = "將你的運動與活動分享在動態牆",
                            checked = shareActivity,
                            onCheckedChange = { shareActivity = it }
                        )
                    }

                    GraySettingCard(title = "個人資料可見度") {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = profileVisibility,
                                onValueChange = {},
                                readOnly = true,
                                label = {
                                    Text(
                                        "誰可以看到你的個人資料",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = MutedText
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = BodyText,
                                    unfocusedTextColor = BodyText,
                                    focusedContainerColor = SolidGray,
                                    unfocusedContainerColor = SolidGray,
                                    disabledContainerColor = SolidGray,
                                    cursorColor = PrimaryBlue,
                                    focusedBorderColor = PrimaryBlue.copy(alpha = 0.85f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
                                    focusedLabelColor = PrimaryBlue,
                                    unfocusedLabelColor = MutedText,
                                    focusedTrailingIconColor = PrimaryBlue,
                                    unfocusedTrailingIconColor = MutedText
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                shape = RoundedCornerShape(18.dp),
                                containerColor = SolidGray, // ✅ 灰底
                                tonalElevation = 0.dp,
                                shadowElevation = 18.dp
                            ) {
                                visibilityOptions.forEach { option ->
                                    FancyDropdownItemDark(
                                        text = option,
                                        selected = option == profileVisibility,
                                        onClick = {
                                            profileVisibility = option
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (uid == null || isSaving) return@Button
                            isSaving = true

                            val visibilityKey = when (profileVisibility) {
                                "僅好友" -> "friends"
                                "僅自己" -> "private"
                                else -> "public"
                            }

                            val data = mapOf(
                                "showOnline" to showOnline,
                                "allowFriendRequests" to allowFriendRequests,
                                "shareActivity" to shareActivity,
                                "profileVisibility" to visibilityKey,
                                "updatedAt" to Timestamp.now()
                            )

                            db.collection("users")
                                .document(uid)
                                .set(data, SetOptions.merge())
                                .addOnSuccessListener {
                                    scope.launch { snackbar.showSnackbar("已儲存設定") }
                                    isSaving = false
                                }
                                .addOnFailureListener { e ->
                                    scope.launch { snackbar.showSnackbar("儲存失敗：${e.message ?: "未知錯誤"}") }
                                    isSaving = false
                                }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth(0.72f),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            contentColor = Color.White,
                            disabledContainerColor = PrimaryBlue.copy(alpha = 0.55f),
                            disabledContentColor = Color.White.copy(alpha = 0.8f)
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("儲存設定", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/* -------------------- 下拉選單：暗色版本 -------------------- */
@Composable
private fun FancyDropdownItemDark(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val itemText = if (selected) TitleText else BodyText
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = itemText
            )
        },
        onClick = onClick,
        leadingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = PrimaryBlue.copy(alpha = 0.90f)
                )
            }
        },
        colors = MenuDefaults.itemColors(
            textColor = itemText,
            leadingIconColor = PrimaryBlue
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    )
}

/* -------------------- 灰色卡片（取代玻璃卡） -------------------- */
@Composable
private fun GraySettingCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = SolidGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = TitleText
            )
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.10f),
                thickness = 1.dp
            )
            content()
        }
    }
}

/* -------------------- Switch Row（灰底） -------------------- */
@Composable
private fun SettingSwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RowGray, shape)
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = BodyText
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MutedText
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryBlue.copy(alpha = 0.80f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.18f)
            )
        )
    }
}


@Composable
private fun DarkGlowBackground(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "dark_glow_privacy")
    val tRaw by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "t"
    )
    val t = (1f - cos(tRaw * PI).toFloat()) / 2f
    val c1 = lerp(Color(0xFF2A2F45), Color(0xFF20243A), t).copy(alpha = 0.55f)
    val c2 = lerp(Color(0xFF2A3A5A), Color(0xFF1C2237), t).copy(alpha = 0.48f)
    val c3 = lerp(Color(0xFF222A40), Color(0xFF161A2A), t).copy(alpha = 0.45f)

    Box(
        modifier = modifier.drawBehind {
            val sx = size.width * (0.18f + 0.30f * t)
            val sy = size.height * (0.10f + 0.22f * (1f - t))
            val ex = size.width * (0.82f - 0.30f * t)
            val ey = size.height * (0.92f - 0.22f * (1f - t))
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