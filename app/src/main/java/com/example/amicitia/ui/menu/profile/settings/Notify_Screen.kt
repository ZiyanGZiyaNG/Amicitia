package com.example.amicitia.ui.menu.profile.settings

import android.app.TimePickerDialog
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
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
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
private val SolidGray = Color(0xFF2A2A2A)
private val RowGray = Color(0xFF242424)

private val TitleText = Color.White.copy(alpha = 0.92f)
private val BodyText = Color.White.copy(alpha = 0.82f)
private val MutedText = Color.White.copy(alpha = 0.62f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotifyScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid

    // 偏好狀態（與雲端同步）
    var enableAll by rememberSaveable { mutableStateOf(true) }
    var pushEnabled by rememberSaveable { mutableStateOf(true) }
    var emailEnabled by rememberSaveable { mutableStateOf(false) }
    var smsEnabled by rememberSaveable { mutableStateOf(false) }

    var activityReminders by rememberSaveable { mutableStateOf(true) }
    var socialMentions by rememberSaveable { mutableStateOf(true) }
    var appUpdates by rememberSaveable { mutableStateOf(true) }
    var marketing by rememberSaveable { mutableStateOf(false) }

    var dndEnabled by rememberSaveable { mutableStateOf(false) }
    var dndStart by rememberSaveable { mutableStateOf("22:00") }
    var dndEnd by rememberSaveable { mutableStateOf("07:00") }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 初次載入
    LaunchedEffect(uid) {
        if (uid == null) {
            scope.launch { snackbar.showSnackbar("尚未登入") }
            isLoading = false
            return@LaunchedEffect
        }
        try {
            val doc = db.collection("users").document(uid).get().await()
            val prefs = (doc.get("notificationPrefs") as? Map<*, *>) ?: emptyMap<String, Any>()
            enableAll = prefs["enableAll"] as? Boolean ?: true
            pushEnabled = prefs["pushEnabled"] as? Boolean ?: true
            emailEnabled = prefs["emailEnabled"] as? Boolean ?: false
            smsEnabled = prefs["smsEnabled"] as? Boolean ?: false

            activityReminders = prefs["activityReminders"] as? Boolean ?: true
            socialMentions = prefs["socialMentions"] as? Boolean ?: true
            appUpdates = prefs["appUpdates"] as? Boolean ?: true
            marketing = prefs["marketing"] as? Boolean ?: false

            dndEnabled = prefs["dndEnabled"] as? Boolean ?: false
            dndStart = prefs["dndStart"] as? String ?: "22:00"
            dndEnd = prefs["dndEnd"] as? String ?: "07:00"
        } catch (e: Exception) {
            scope.launch { snackbar.showSnackbar("載入失敗：${e.message ?: "未知錯誤"}") }
        }
        isLoading = false
    }

    fun persist() {
        val id = uid ?: return
        if (isSaving) return
        isSaving = true
        val data = mapOf(
            "notificationPrefs" to mapOf(
                "enableAll" to enableAll,
                "pushEnabled" to pushEnabled,
                "emailEnabled" to emailEnabled,
                "smsEnabled" to smsEnabled,
                "activityReminders" to activityReminders,
                "socialMentions" to socialMentions,
                "appUpdates" to appUpdates,
                "marketing" to marketing,
                "dndEnabled" to dndEnabled,
                "dndStart" to dndStart,
                "dndEnd" to dndEnd,
                "updatedAt" to Timestamp.now()
            )
        )
        db.collection("users").document(id)
            .set(data, SetOptions.merge())
            .addOnSuccessListener { scope.launch { snackbar.showSnackbar("已儲存設定") } }
            .addOnFailureListener { e ->
                scope.launch { snackbar.showSnackbar("儲存失敗：${e.message ?: "未知錯誤"}") }
            }
            .addOnCompleteListener { isSaving = false }
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
                            text = "通知偏好",
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
                    // 1) 總開關
                    GraySettingCard(title = "總體") {
                        SettingSwitchRow(
                            title = "啟用所有通知",
                            desc = "關閉後將不再接收任何提醒",
                            checked = enableAll,
                            onCheckedChange = { enableAll = it; persist() }
                        )
                    }

                    // 2) 傳遞方式
                    GraySettingCard(title = "傳遞方式") {
                        SettingSwitchRow(
                            title = "推播通知",
                            desc = "顯示在裝置通知中心",
                            checked = pushEnabled,
                            onCheckedChange = { pushEnabled = it; persist() },
                            leading = { Icon(Icons.Rounded.Notifications, null, tint = PrimaryBlue) }
                        )
                        SettingSwitchRow(
                            title = "電子郵件",
                            desc = "將摘要寄送至你的信箱",
                            checked = emailEnabled,
                            onCheckedChange = { emailEnabled = it; persist() },
                            leading = { Icon(Icons.Rounded.Email, null, tint = PrimaryBlue) }
                        )
                        SettingSwitchRow(
                            title = "簡訊",
                            desc = "可能產生電信商收費",
                            checked = smsEnabled,
                            onCheckedChange = { smsEnabled = it; persist() },
                            leading = { Icon(Icons.Rounded.Sms, null, tint = PrimaryBlue) }
                        )
                    }

                    // 3) 要通知的內容
                    GraySettingCard(title = "通知內容") {
                        SettingSwitchRow(
                            title = "運動/活動提醒",
                            desc = "每日目標、久坐提醒、活動紀錄",
                            checked = activityReminders,
                            onCheckedChange = { activityReminders = it; persist() }
                        )
                        SettingSwitchRow(
                            title = "被標註與訊息",
                            desc = "@提及、留言、邀請",
                            checked = socialMentions,
                            onCheckedChange = { socialMentions = it; persist() }
                        )
                        SettingSwitchRow(
                            title = "系統與版本更新",
                            desc = "維護公告、重大更新通知",
                            checked = appUpdates,
                            onCheckedChange = { appUpdates = it; persist() }
                        )
                        SettingSwitchRow(
                            title = "活動與行銷訊息",
                            desc = "官方活動、優惠與推薦",
                            checked = marketing,
                            onCheckedChange = { marketing = it; persist() }
                        )
                    }

                    // 4) 勿擾模式
                    GraySettingCard(title = "勿擾模式") {
                        SettingSwitchRow(
                            title = "排程勿擾",
                            desc = "在指定時段靜音通知",
                            checked = dndEnabled,
                            onCheckedChange = { dndEnabled = it; persist() }
                        )

                        val context = LocalContext.current

                        val (sh, sm) = runCatching { dndStart.split(":").map { it.toInt() } }
                            .getOrElse { listOf(22, 0) }
                        val (eh, em) = runCatching { dndEnd.split(":").map { it.toInt() } }
                            .getOrElse { listOf(7, 0) }

                        val startPicker = remember(context, sh, sm) {
                            TimePickerDialog(context, { _, h, m ->
                                dndStart = "%02d:%02d".format(h, m); persist()
                            }, sh, sm, true)
                        }
                        val endPicker = remember(context, eh, em) {
                            TimePickerDialog(context, { _, h, m ->
                                dndEnd = "%02d:%02d".format(h, m); persist()
                            }, eh, em, true)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GrayOutlinedButton(
                                enabled = dndEnabled && !isSaving,
                                onClick = { startPicker.show() },
                                icon = Icons.Rounded.Schedule,
                                text = "開始 $dndStart"
                            )
                            GrayOutlinedButton(
                                enabled = dndEnabled && !isSaving,
                                onClick = { endPicker.show() },
                                icon = Icons.Rounded.Schedule,
                                text = "結束 $dndEnd"
                            )
                        }

                        AssistChip(
                            onClick = {
                                dndEnabled = true
                                dndStart = "23:00"
                                dndEnd = "07:00"
                                persist()
                            },
                            label = {
                                Text(
                                    "一鍵 23:00–07:00",
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                    color = PrimaryBlue
                                )
                            },
                            leadingIcon = { Icon(Icons.Rounded.Alarm, null, tint = PrimaryBlue) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = SolidGray,
                                labelColor = PrimaryBlue,
                                leadingIconContentColor = PrimaryBlue,
                                disabledContainerColor = SolidGray,
                                disabledLabelColor = PrimaryBlue.copy(alpha = 0.55f),
                                disabledLeadingIconContentColor = PrimaryBlue.copy(alpha = 0.55f)
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { if (!isSaving && uid != null) persist() },
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


@Composable
private fun DarkGlowBackground(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "dark_glow_notify")
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


@Composable
private fun GraySettingCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = SolidGray), // ✅ 灰底
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
                    fontWeight = FontWeight.Bold,
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


@Composable
private fun SettingSwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leading: @Composable (() -> Unit)? = null
) {
    val rowShape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RowGray, rowShape) // ✅ 灰底
            .border(1.dp, Color.White.copy(alpha = 0.08f), rowShape)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(10.dp))
        }

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
private fun GrayOutlinedButton(
    enabled: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    OutlinedButton(
        enabled = enabled,
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (enabled) 0.16f else 0.08f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = PrimaryBlue,                       // ✅ icon/text 走 PrimaryBlue（你原本就這樣）
            disabledContentColor = PrimaryBlue.copy(alpha = 0.35f),
            containerColor = SolidGray,                       // ✅ 灰底
            disabledContainerColor = SolidGray.copy(alpha = 0.65f)
        )
    ) {
        Icon(icon, null) // ✅ 不指定 tint，就會吃 contentColor（PrimaryBlue）
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}