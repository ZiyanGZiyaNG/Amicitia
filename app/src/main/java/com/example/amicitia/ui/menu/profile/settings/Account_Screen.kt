package com.example.amicitia.ui.menu.profile.settings

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.PI
import kotlin.math.cos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val Primary = Color(0xFF3F51B5)
    val PrimaryContainer = Color(0xFFE8EBFF)
    val Danger = MaterialTheme.colorScheme.error

    val OutlinedColors = ButtonDefaults.outlinedButtonColors(
        contentColor = Primary,
        disabledContentColor = Primary.copy(alpha = 0.35f)
    )

    val auth = remember { FirebaseAuth.getInstance() }

    var isLoading by remember { mutableStateOf(true) }
    var isWorking by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var email by rememberSaveable { mutableStateOf(auth.currentUser?.email.orEmpty()) }
    var emailVerified by remember { mutableStateOf(auth.currentUser?.isEmailVerified == true) }

    LaunchedEffect(Unit) { isLoading = false }

    // ---- 動作封裝 ----
    suspend fun resendVerify(): String = try {
        auth.currentUser?.sendEmailVerification()?.await()
        "已寄出驗證信，請至信箱查收"
    } catch (e: Exception) { "寄送失敗：${e.message ?: "未知錯誤"}" }

    suspend fun changeEmailTo(newEmail: String): String = try {
        auth.currentUser?.updateEmail(newEmail)?.await()
        email = newEmail
        emailVerified = false
        auth.currentUser?.sendEmailVerification()?.await()
        "Email 已更新並寄出驗證信"
    } catch (e: Exception) { "更新失敗：${e.message ?: "未知錯誤"}" }

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
        } catch (e: Exception) { "變更失敗：${e.message ?: "未知錯誤"}" }
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
        } catch (e: Exception) { "刪除失敗：${e.message ?: "未知錯誤"}" }
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
                            text = "帳號與安全",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
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
                    // A) 帳號資訊
                    AccountSettingCard(title = "帳號資訊") {
                        Text(
                            text = "Email：$email",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val statusColor =
                            if (emailVerified) Primary else MaterialTheme.colorScheme.error
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

                            OutlinedButton(
                                onClick = { showEmailDialog = true },
                                colors = OutlinedColors,
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Rounded.Edit, null)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "變更 Email",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    if (isWorking) return@OutlinedButton
                                    isWorking = true
                                    scope.launch {
                                        snackbar.showSnackbar(resendVerify())
                                        isWorking = false
                                    }
                                },
                                enabled = !emailVerified,
                                colors = OutlinedColors,
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Rounded.MarkEmailRead, null)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "重寄驗證信",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            if (showEmailDialog) ChangeEmailDialog(
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

                    // B) 登入與安全
                    AccountSettingCard(title = "登入與安全") {
                        var showPwd by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { showPwd = true },
                            colors = OutlinedColors,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Rounded.Key, null)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "變更密碼",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        if (showPwd) ChangePasswordDialog(
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

                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = {
                                Text(
                                    "MFA（稍後接）",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp
                                    )
                                )
                            },
                            leadingIcon = { Icon(Icons.Rounded.Security, null) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = PrimaryContainer,
                                labelColor = Primary,
                                leadingIconContentColor = Primary
                            )
                        )
                    }

                    // C) 裝置與工作階段（占位）
                    AccountSettingCard(title = "裝置與工作階段") {
                        Text(
                            text = "將在此顯示已登入裝置，並提供登出其他裝置。",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            colors = OutlinedColors,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Rounded.Logout, null)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "登出其他裝置",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    // D) 資料與帳號
                    AccountSettingCard(title = "資料與帳號") {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                colors = OutlinedColors,
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Rounded.FileDownload, null)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "匯出我的資料",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            var showDelete by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = { showDelete = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Danger,
                                    disabledContentColor = Danger.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Rounded.DeleteForever, null)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "刪除帳號",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            if (showDelete) DeleteAccountDialog(
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

                    Spacer(Modifier.height(32.dp))

                    Button(
                        onClick = { scope.launch { snackbar.showSnackbar("設定已更新") } },
                        enabled = !isWorking,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth(0.7f),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = Color.White,
                            disabledContainerColor = Primary.copy(alpha = 0.5f),
                            disabledContentColor = Color.White.copy(alpha = 0.8f)
                        )
                    ) {
                        Text(
                            text = "完成",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ChangeEmailDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "變更 Email",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("新 Email") }
                )
                Text(
                    "系統可能會要求你重新驗證身份。",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(value) }) {
                Text(
                    "確認",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "取消",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (old: String, new: String) -> Unit
) {
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "變更密碼",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = old,
                    onValueChange = { old = it },
                    label = { Text("目前密碼") },
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = new,
                    onValueChange = { new = it },
                    label = { Text("新密碼") },
                    visualTransformation = PasswordVisualTransformation()
                )
                Text(
                    "至少 8 碼，混合大小寫與數字符號。",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(old, new) }, enabled = new.length >= 8) {
                Text(
                    "更新",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "取消",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}

@Composable
private fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pwd by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "刪除帳號",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "此動作無法復原。請輸入密碼以確認身份。",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                )
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it },
                    label = { Text("密碼") },
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(pwd) }) {
                Text(
                    "確認刪除",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "取消",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}

/* 背景動畫（與 Notify/Privacy 風格一致） */
@Composable
private fun AnimatedGradientBackground(
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition()
    val tRaw by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
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

@Composable
private fun AccountSettingCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF7F4FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = Color(0xFF2C2C54)
                )
            }
            HorizontalDivider(
                color = Color(0xFFE0E0FF),
                thickness = 1.dp
            )
            content()
        }
    }
}