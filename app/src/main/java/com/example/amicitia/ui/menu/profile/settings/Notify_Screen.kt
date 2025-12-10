package com.example.amicitia.ui.menu.profile.settings

import android.app.TimePickerDialog
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotifyScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid
    val mainButtonColor = Color(0xFF3F51B5)
    val lightBorderColor = Color(0xFF3F51B5)

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

    // 儲存
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
            .systemBarsPadding()
    ) {
        AnimatedGradientBackground(modifier = Modifier.matchParentSize())

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "通知偏好",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            )
                        )
                    },
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
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) }
        ) { innerPadding ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = mainButtonColor)
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1) 總開關
                    SettingCard(title = "總體") {
                        SettingSwitch(
                            title = "啟用所有通知",
                            desc = "關閉後將不再接收任何提醒",
                            checked = enableAll,
                            onCheckedChange = { enableAll = it; persist() },
                            color = mainButtonColor
                        )
                    }

                    // 2) 傳遞方式
                    SettingCard(title = "傳遞方式") {
                        SettingSwitch(
                            title = "推播通知",
                            desc = "顯示在裝置通知中心",
                            checked = pushEnabled,
                            onCheckedChange = { pushEnabled = it; persist() },
                            color = mainButtonColor
                        )
                        SettingSwitch(
                            title = "電子郵件",
                            desc = "將摘要寄送至你的信箱",
                            checked = emailEnabled,
                            onCheckedChange = { emailEnabled = it; persist() },
                            color = mainButtonColor
                        )
                        SettingSwitch(
                            title = "簡訊",
                            desc = "可能產生電信商收費",
                            checked = smsEnabled,
                            onCheckedChange = { smsEnabled = it; persist() },
                            color = mainButtonColor
                        )
                    }

                    // 3) 要通知的內容
                    SettingCard(title = "通知內容") {
                        SettingSwitch(
                            title = "運動/活動提醒",
                            desc = "每日目標、久坐提醒、活動紀錄",
                            checked = activityReminders,
                            onCheckedChange = { activityReminders = it; persist() },
                            color = mainButtonColor
                        )
                        SettingSwitch(
                            title = "被標註與訊息",
                            desc = "@提及、留言、邀請",
                            checked = socialMentions,
                            onCheckedChange = { socialMentions = it; persist() },
                            color = mainButtonColor
                        )
                        SettingSwitch(
                            title = "系統與版本更新",
                            desc = "維護公告、重大更新通知",
                            checked = appUpdates,
                            onCheckedChange = { appUpdates = it; persist() },
                            color = mainButtonColor
                        )
                        SettingSwitch(
                            title = "活動與行銷訊息",
                            desc = "官方活動、優惠與推薦",
                            checked = marketing,
                            onCheckedChange = { marketing = it; persist() },
                            color = mainButtonColor
                        )
                    }

                    // 4) 勿擾模式
                    SettingCard(title = "勿擾模式") {
                        SettingSwitch(
                            title = "排程勿擾",
                            desc = "在指定時段靜音通知",
                            checked = dndEnabled,
                            onCheckedChange = { dndEnabled = it; persist() },
                            color = mainButtonColor
                        )

                        // 時段選擇
                        val (sh, sm) = dndStart.split(":").map { it.toInt() }
                        val (eh, em) = dndEnd.split(":").map { it.toInt() }
                        val context = LocalContext.current
                        val startPicker = remember {
                            TimePickerDialog(context, { _, h, m ->
                                dndStart = "%02d:%02d".format(h, m); persist()
                            }, sh, sm, true)
                        }
                        val endPicker = remember {
                            TimePickerDialog(context, { _, h, m ->
                                dndEnd = "%02d:%02d".format(h, m); persist()
                            }, eh, em, true)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                enabled = dndEnabled,
                                onClick = { startPicker.show() },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = mainButtonColor
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.linearGradient(
                                        listOf(mainButtonColor, lightBorderColor)
                                    )
                                )
                            ) {
                                Icon(Icons.Rounded.Schedule, null)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "開始 $dndStart",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            OutlinedButton(
                                enabled = dndEnabled,
                                onClick = { endPicker.show() },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = mainButtonColor
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.linearGradient(
                                        listOf(mainButtonColor, lightBorderColor)
                                    )
                                )
                            ) {
                                Icon(Icons.Rounded.Schedule, null)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "結束 $dndEnd",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
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
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp
                                    )
                                )
                            },
                            leadingIcon = { Icon(Icons.Rounded.Alarm, null) }
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    // 底部「儲存」
                    Button(
                        onClick = { if (!isSaving && uid != null) persist() },
                        enabled = !isSaving,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth(0.7f),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = mainButtonColor,
                            contentColor = Color.White,
                            disabledContainerColor = mainButtonColor.copy(alpha = 0.5f),
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
                            Text(
                                text = "儲存設定",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/* 背景動畫 */
@Composable
private fun AnimatedGradientBackground(
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition()
    val tRaw by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val t = (1f - cos(tRaw * PI).toFloat()) / 2f
    val c1 = lerp(Color(0xFFF3F6FF), Color(0xFFE8F0FF), t)
    val c2 = lerp(Color(0xFFEAF1FF), Color(0xFFD6E3FF), t)
    val c3 = lerp(Color(0xFFDDE7FF), Color(0xFFCBD9FF), t)

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