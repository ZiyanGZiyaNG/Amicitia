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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.PI
import kotlin.math.cos

private val BgDark = Color(0xFF1E1E1E)
private val PrimaryBlue = Color(0xFF3F51B5)

// ✅ 你要的：灰色按鈕 / 灰色卡片（不改 icon 顏色，icon 照原本 PrimaryBlue / danger 走）
private val SolidGray = Color(0xFF2A2A2A)

private val TitleText = Color.White.copy(alpha = 0.92f)
private val BodyText = Color.White.copy(alpha = 0.82f)
private val MutedText = Color.White.copy(alpha = 0.62f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val danger = MaterialTheme.colorScheme.error
    val auth = remember { FirebaseAuth.getInstance() }

    var isLoading by remember { mutableStateOf(true) }
    var isWorking by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var email by rememberSaveable { mutableStateOf(auth.currentUser?.email.orEmpty()) }
    var emailVerified by remember { mutableStateOf(auth.currentUser?.isEmailVerified == true) }

    LaunchedEffect(Unit) { isLoading = false }

    suspend fun resendVerify(): String = try {
        auth.currentUser?.sendEmailVerification()?.await()
        "已寄出驗證信，請至信箱查收"
    } catch (e: Exception) {
        "寄送失敗：${e.message ?: "未知錯誤"}"
    }

    suspend fun changeEmailTo(newEmail: String): String = try {
        auth.currentUser?.updateEmail(newEmail)?.await()
        email = newEmail
        emailVerified = false
        auth.currentUser?.sendEmailVerification()?.await()
        "Email 已更新並寄出驗證信"
    } catch (e: Exception) {
        "更新失敗：${e.message ?: "未知錯誤"}"
    }

    suspend fun changePassword(old: String, new: String): String {
        val u = auth.currentUser ?: return "尚未登入"
        val mail = u.email
        return try {
            if (!mail.isNullOrBlank()) {
                val cred = EmailAuthProvider.getCredential(mail, old)
                u.reauthenticate(cred).await()
            }
            u.updatePassword(new).await()
            "密碼已更新"
        } catch (e: Exception) {
            "變更失敗：${e.message ?: "未知錯誤"}"
        }
    }

    suspend fun deleteAccount(password: String): String {
        val u = auth.currentUser ?: return "尚未登入"
        val mail = u.email
        return try {
            if (!mail.isNullOrBlank()) {
                val cred = EmailAuthProvider.getCredential(mail, password)
                u.reauthenticate(cred).await()
            }
            u.delete().await()
            "帳號已刪除"
        } catch (e: Exception) {
            "刪除失敗：${e.message ?: "未知錯誤"}"
        }
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
                            text = "帳號與安全",
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
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                    Modifier
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
                    // A) 帳號資訊
                    GlassSettingCard(title = "帳號資訊") {
                        Text(
                            text = "Email：$email",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = BodyText
                        )

                        val statusColor = if (emailVerified) PrimaryBlue else danger
                        Text(
                            text = if (emailVerified) "狀態：已驗證" else "狀態：未驗證",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = statusColor
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            var showEmailDialog by remember { mutableStateOf(false) }

                            GlassOutlinedButton(
                                onClick = { showEmailDialog = true },
                                enabled = !isWorking,
                                leading = { Icon(Icons.Rounded.Edit, null) },
                                text = "變更 Email"
                            )

                            GlassOutlinedButton(
                                onClick = {
                                    if (isWorking) return@GlassOutlinedButton
                                    isWorking = true
                                    scope.launch {
                                        snackbar.showSnackbar(resendVerify())
                                        isWorking = false
                                    }
                                },
                                enabled = !emailVerified && !isWorking,
                                leading = { Icon(Icons.Rounded.MarkEmailRead, null) },
                                text = "重寄驗證信"
                            )

                            if (showEmailDialog) {
                                ChangeEmailDialog(
                                    current = email,
                                    onDismiss = { showEmailDialog = false },
                                    onConfirm = { newEmail ->
                                        isWorking = true
                                        scope.launch {
                                            snackbar.showSnackbar(changeEmailTo(newEmail))
                                            showEmailDialog = false
                                            isWorking = false
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // B) 登入與安全
                    GlassSettingCard(title = "登入與安全") {
                        var showPwd by remember { mutableStateOf(false) }

                        GlassOutlinedButton(
                            onClick = { showPwd = true },
                            enabled = !isWorking,
                            leading = { Icon(Icons.Rounded.Key, null) },
                            text = "變更密碼"
                        )

                        if (showPwd) {
                            ChangePasswordDialog(
                                onDismiss = { showPwd = false },
                                onConfirm = { old, new ->
                                    isWorking = true
                                    scope.launch {
                                        snackbar.showSnackbar(changePassword(old, new))
                                        showPwd = false
                                        isWorking = false
                                    }
                                }
                            )
                        }

                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = {
                                Text(
                                    "MFA（稍後接）",
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                    color = PrimaryBlue
                                )
                            },
                            leadingIcon = { Icon(Icons.Rounded.Security, null, tint = PrimaryBlue) },
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

                    // C) 裝置與工作階段（占位）
                    GlassSettingCard(title = "裝置與工作階段") {
                        Text(
                            text = "將在此顯示已登入裝置，並提供登出其他裝置。",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = MutedText
                        )
                        GlassOutlinedButton(
                            onClick = {},
                            enabled = false,
                            leading = { Icon(Icons.Rounded.Logout, null) },
                            text = "登出其他裝置"
                        )
                    }

                    // D) 資料與帳號
                    GlassSettingCard(title = "資料與帳號") {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassOutlinedButton(
                                onClick = {},
                                enabled = false,
                                leading = { Icon(Icons.Rounded.FileDownload, null) },
                                text = "匯出我的資料"
                            )

                            var showDelete by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = { showDelete = true },
                                enabled = !isWorking,
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, danger.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = danger,
                                    disabledContentColor = danger.copy(alpha = 0.35f),
                                    containerColor = SolidGray,            // ✅ 灰底
                                    disabledContainerColor = SolidGray.copy(alpha = 0.65f)
                                )
                            ) {
                                Icon(Icons.Rounded.DeleteForever, null) // ✅ icon 顏色走 danger，不會被改
                                Spacer(Modifier.width(6.dp))
                                Text("刪除帳號", style = MaterialTheme.typography.labelLarge)
                            }

                            if (showDelete) {
                                DeleteAccountDialog(
                                    onDismiss = { showDelete = false },
                                    onConfirm = { pwd ->
                                        isWorking = true
                                        scope.launch {
                                            snackbar.showSnackbar(deleteAccount(pwd))
                                            showDelete = false
                                            isWorking = false
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { scope.launch { snackbar.showSnackbar("設定已更新") } },
                        enabled = !isWorking,
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
                        Text("完成", style = MaterialTheme.typography.labelLarge)
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/* -------------------- UI：暗色光暈背景 -------------------- */

@Composable
private fun DarkGlowBackground(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "dark_glow")
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

/* -------------------- UI：卡片改成灰色（不改 icon 顏色） -------------------- */

@Composable
private fun GlassSettingCard(
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

/* -------------------- UI：按鈕改成灰色（不改 icon 顏色） -------------------- */

@Composable
private fun GlassOutlinedButton(
    onClick: () -> Unit,
    enabled: Boolean,
    leading: @Composable (() -> Unit)? = null,
    text: String
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (enabled) 0.16f else 0.08f)),
        colors = ButtonDefaults.outlinedButtonColors(
            // ✅ 內容顏色維持 PrimaryBlue → 你的 Icon() 沒寫 tint 也會跟著走，不會被我硬改
            contentColor = PrimaryBlue,
            disabledContentColor = PrimaryBlue.copy(alpha = 0.35f),

            // ✅ 灰底
            containerColor = SolidGray,
            disabledContainerColor = SolidGray.copy(alpha = 0.65f)
        )
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/* -------------------- Dialogs（暗色） -------------------- */

@Composable
private fun ChangeEmailDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(current) }

    DarkAlertDialog(
        title = "變更 Email",
        onDismiss = onDismiss,
        confirmText = "確認",
        confirmEnabled = value.isNotBlank(),
        onConfirm = { onConfirm(value) }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DarkOutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = "新 Email",
                isPassword = false
            )
            Text(
                "系統可能會要求你重新驗證身份。",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MutedText
            )
        }
    }
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (old: String, new: String) -> Unit
) {
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }

    DarkAlertDialog(
        title = "變更密碼",
        onDismiss = onDismiss,
        confirmText = "更新",
        confirmEnabled = new.length >= 8,
        onConfirm = { onConfirm(old, new) }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DarkOutlinedTextField(
                value = old,
                onValueChange = { old = it },
                label = "目前密碼",
                isPassword = true
            )
            DarkOutlinedTextField(
                value = new,
                onValueChange = { new = it },
                label = "新密碼",
                isPassword = true
            )
            Text(
                "至少 8 碼，混合大小寫與數字符號。",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MutedText
            )
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pwd by remember { mutableStateOf("") }

    DarkAlertDialog(
        title = "刪除帳號",
        onDismiss = onDismiss,
        confirmText = "確認刪除",
        confirmEnabled = pwd.isNotBlank(),
        confirmIsDanger = true,
        onConfirm = { onConfirm(pwd) }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "此動作無法復原。請輸入密碼以確認身份。",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MutedText
            )
            DarkOutlinedTextField(
                value = pwd,
                onValueChange = { pwd = it },
                label = "密碼",
                isPassword = true
            )
        }
    }
}

@Composable
private fun DarkAlertDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String,
    confirmEnabled: Boolean,
    confirmIsDanger: Boolean = false,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    val danger = MaterialTheme.colorScheme.error

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121318),
        titleContentColor = TitleText,
        textContentColor = BodyText,
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = TitleText
            )
        },
        text = { content() },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (confirmIsDanger) danger else PrimaryBlue,
                    contentColor = Color.White,
                    disabledContainerColor = (if (confirmIsDanger) danger else PrimaryBlue).copy(alpha = 0.45f),
                    disabledContentColor = Color.White.copy(alpha = 0.75f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White.copy(alpha = 0.80f)
                )
            ) { Text("取消") }
        }
    )
}

@Composable
private fun DarkOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = { Text(label, color = MutedText) },
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = BodyText),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = BodyText,
            unfocusedTextColor = BodyText,
            focusedBorderColor = PrimaryBlue.copy(alpha = 0.85f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
            focusedLabelColor = PrimaryBlue,
            unfocusedLabelColor = MutedText,
            cursorColor = PrimaryBlue,
            focusedContainerColor = SolidGray,
            unfocusedContainerColor = SolidGray
        ),
        modifier = Modifier.fillMaxWidth()
    )
}