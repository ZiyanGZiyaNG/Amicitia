package com.example.amicitia.ui.login

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.amicitia.R
import com.example.amicitia.nav.Routes
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// 主色
val PrimaryBlue = Color(0xFF3F51B5)

/** 動態漸層背景 **/
@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    aStart: Color = Color(0xFFF3F6FF),
    aMid:   Color = Color(0xFFEAF1FF),
    aEnd:   Color = Color(0xFFDDE7FF),
    bStart: Color = Color(0xFFE8F0FF),
    bMid:   Color = Color(0xFFD6E3FF),
    bEnd:   Color = Color(0xFFCBD9FF),
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

/** App Logo **/
@Composable
fun LoginLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(112.dp)
            .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, PrimaryBlue.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        )
    }
}

/** Login 主畫面（含忘記密碼流程） **/
@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var fieldErrorEmail by remember { mutableStateOf<String?>(null) }
    var fieldErrorPassword by remember { mutableStateOf<String?>(null) }
    var formMessage by remember { mutableStateOf<String?>(null) }

    // 忘記密碼 Dialog 狀態
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val pwFocus = remember { FocusRequester() }
    val auth = Firebase.auth

    /** 登入流程 **/
    suspend fun login() {
        try {
            loading = true
            formMessage = null
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: error("找不到使用者")
            formMessage = "登入成功，歡迎 ${user.displayName ?: user.email}"
            delay(800)
            navController.navigate(Routes.MENU) {
                popUpTo(Routes.LOGIN) { inclusive = true }
                launchSingleTop = true
            }
        } catch (e: Exception) {
            val msg = e.message?.lowercase() ?: ""
            formMessage = when {
                "no user record" in msg -> "此帳號不存在"
                "invalid password" in msg || "password is invalid" in msg -> "密碼錯誤，請再試一次"
                "too many requests" in msg -> "嘗試次數過多，請稍後再試"
                "network" in msg -> "網路連線錯誤，請檢查網路"
                else -> "登入失敗，請稍後再試"
            }
        } finally {
            loading = false
        }
    }

    /** 寄送重設密碼信 **/
    suspend fun sendPasswordReset(targetEmail: String) {
        try {
            resetLoading = true
            formMessage = null
            auth.setLanguageCode("zh-TW")

            val acs = ActionCodeSettings.newBuilder()
                .setUrl("https://amicitia.page.link/reset")
                .setHandleCodeInApp(true)
                .setAndroidPackageName("com.example.amicitia", true, null)
                .build()

            try {
                auth.sendPasswordResetEmail(targetEmail.trim(), acs).await()
            } catch (_: Exception) {
                auth.sendPasswordResetEmail(targetEmail.trim()).await()
            }

            formMessage = "已寄出重設密碼信到 $targetEmail"
            showResetDialog = false
        } catch (e: Exception) {
            val msg = e.message?.lowercase() ?: ""
            formMessage = when {
                "no user record" in msg -> "查無此帳號"
                "invalid email" in msg -> "Email 格式不正確"
                "blocked" in msg || "too many requests" in msg -> "請求過於頻繁，稍後再試"
                else -> "寄送失敗，請確認 Email 是否正確"
            }
        } finally {
            resetLoading = false
        }
    }

    // ============ UI ============

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
    ) {
        AnimatedGradientBackground(modifier = Modifier.matchParentSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(128.dp))
            LoginLogo()

            Spacer(Modifier.height(16.dp))
            Text("歡迎回來", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF0F172A))
            Text("請登入以繼續", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF475569))

            Spacer(Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 2.dp,
                shadowElevation = 8.dp,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            val sanitized = it.replace("\n", "").replace(" ", "")
                            email = sanitized
                            fieldErrorEmail = null
                        },
                        label = { Text("電子郵件") },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                        singleLine = true,
                        isError = fieldErrorEmail != null,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { pwFocus.requestFocus() }
                        )
                    )

                    AnimatedVisibility(visible = fieldErrorEmail != null) {
                        Text(
                            fieldErrorEmail ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    AnimatedVisibility(
                        visible = email.contains("@") && email.contains("."),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it; fieldErrorPassword = null },
                                label = { Text("密碼") },
                                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                                singleLine = true,
                                isError = fieldErrorPassword != null,
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    val icon = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(icon, contentDescription = null)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(pwFocus),
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (!loading) scope.launch { login() }
                                    }
                                )
                            )

                            AnimatedVisibility(visible = fieldErrorPassword != null) {
                                Text(
                                    fieldErrorPassword ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = formMessage != null, enter = fadeIn(), exit = fadeOut()) {
                        val isErrorMsg = remember(formMessage) {
                            formMessage?.contains("錯誤") == true ||
                                    formMessage?.contains("失敗") == true ||
                                    formMessage?.contains("不存在") == true
                        }
                        Text(
                            text = formMessage ?: "",
                            color = if (isErrorMsg) MaterialTheme.colorScheme.error else PrimaryBlue,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                        )
                    }

                    Button(
                        onClick = { scope.launch { login() } },
                        enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            contentColor = Color.White
                        )
                    ) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("處理中…")
                        } else {
                            Text("登入")
                        }
                    }

                    OutlinedButton(
                        onClick = { navController.navigate(Routes.REGISTER) },
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = PrimaryBlue
                        )
                    ) {
                        Text("註冊")
                    }

                    TextButton(
                        onClick = {
                            resetEmail = email
                            showResetDialog = true
                        },
                        enabled = !loading
                    ) {
                        Text("忘記密碼？", color = PrimaryBlue, textDecoration = TextDecoration.Underline)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // 忘記密碼對話框（同色系動態漸層 + 輕霧面層）
    // 忘記密碼對話框（動態漸層 + 同字色 + 同框線色）
    if (showResetDialog) {
        Dialog(onDismissRequest = { if (!resetLoading) showResetDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(22.dp))
            ) {
                // 背景漸層：與主畫面完全相同
                AnimatedGradientBackground(
                    modifier = Modifier.matchParentSize(),
                    aStart = Color(0xFFF3F6FF),
                    aMid = Color(0xFFEAF1FF),
                    aEnd = Color(0xFFDDE7FF),
                    bStart = Color(0xFFE8F0FF),
                    bMid = Color(0xFFD6E3FF),
                    bEnd = Color(0xFFCBD9FF)
                )

                // 淺霧面層
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.White.copy(alpha = 0.35f))
                        .border(
                            width = 1.dp,
                            color = PrimaryBlue.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(22.dp)
                        )
                )

                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "重設密碼",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        "請輸入你的註冊 Email，我們會寄送重設密碼連結給你。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF475569)
                    )

                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = {
                            resetEmail = it.replace(" ", "").replace("\n", "")
                        },
                        singleLine = true,
                        enabled = !resetLoading,
                        label = {
                            Text("電子郵件", color = Color(0xFF475569))
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Email,
                                contentDescription = null,
                                tint = Color(0xFF475569)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = PrimaryBlue.copy(alpha = 0.4f),
                            cursorColor = PrimaryBlue,
                            focusedLabelColor = PrimaryBlue,
                            unfocusedLabelColor = Color(0xFF475569),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { if (!resetLoading) showResetDialog = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = PrimaryBlue)
                        ) { Text("取消") }

                        TextButton(
                            onClick = {
                                if (resetEmail.isBlank() || !resetEmail.contains("@")) {
                                    formMessage = "請輸入正確的 Email"
                                    return@TextButton
                                }
                                scope.launch { sendPasswordReset(resetEmail) }
                            },
                            enabled = !resetLoading,
                            colors = ButtonDefaults.textButtonColors(contentColor = PrimaryBlue)
                        ) {
                            if (resetLoading) {
                                CircularProgressIndicator(
                                    Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = PrimaryBlue
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text("寄送")
                        }
                    }
                }
            }
        }
    }
}