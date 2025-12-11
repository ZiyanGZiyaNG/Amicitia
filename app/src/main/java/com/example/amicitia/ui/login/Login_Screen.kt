package com.example.amicitia.ui.login

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

val PrimaryBlue = Color(0xFF3F51B5)


@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    successProgress: Float = 0f
) {
    val infinite = rememberInfiniteTransition(label = "login_bg")

    val pulse by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "login_pulse"
    )

    val drift by infinite.animateFloat(
        initialValue = -0.04f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "login_drift"
    )

    Box(
        modifier = modifier.drawBehind {
            val minDim = size.minDimension

            val scaleBoost = 1f + 5f * successProgress
            val pulseScaled = pulse * scaleBoost

            val circle1Color = lerp(Color(0x667C3AED), Color(0xFFFFFFFF), successProgress)
            val circle2Color = lerp(Color(0x664F46E5), Color(0xFFFFFFFF), successProgress)

            drawCircle(
                color = circle1Color,
                radius = minDim * 0.45f * pulseScaled,
                center = Offset(
                    x = size.width * (0.0f + drift),
                    y = size.height * (0.12f + drift * 0.5f)
                )
            )

            drawCircle(
                color = circle2Color,
                radius = minDim * 0.55f * pulseScaled,
                center = Offset(
                    x = size.width * (1.15f - drift * 0.5f),
                    y = size.height * (0.95f - drift)
                )
            )

            if (successProgress > 0f) {
                drawRect(
                    color = Color.White.copy(alpha = successProgress * 0.9f),
                    size = size
                )
            }
        }
    )
}

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

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var fieldErrorEmail by remember { mutableStateOf<String?>(null) }
    var fieldErrorPassword by remember { mutableStateOf<String?>(null) }
    var formMessage by remember { mutableStateOf<String?>(null) }


    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetLoading by remember { mutableStateOf(false) }
    var loginSuccess by remember { mutableStateOf(false) }
    val successProgress by animateFloatAsState(
        targetValue = if (loginSuccess) 1f else 0f,
        animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
        label = "login-success",
        finishedListener = { value ->
            if (value == 1f && loginSuccess) {
                navController.navigate(Routes.MENU) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    )

    val scope = rememberCoroutineScope()
    val pwFocus = remember { FocusRequester() }
    val auth = Firebase.auth

    LaunchedEffect(Unit) {
        val o = com.google.firebase.FirebaseApp.getInstance().options
        Log.i("FirebaseCheck", "projectId=${o.projectId}, appId=${o.applicationId}")
    }

    suspend fun login() {
        try {
            loading = true
            formMessage = null
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: error("找不到使用者")
            formMessage = "登入成功，歡迎 ${user.displayName ?: user.email}"
            loginSuccess = true
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

    suspend fun sendPasswordReset(targetEmail: String) {
        resetLoading = true
        formMessage = null
        try {
            val mail = targetEmail.trim()
            auth.setLanguageCode("zh-TW")

            val methods = auth.fetchSignInMethodsForEmail(mail).await().signInMethods ?: emptyList()
            Log.i("ResetPW", "email=$mail, signInMethods=$methods")

            auth.sendPasswordResetEmail(mail).await()

            formMessage = "如果這個 Email 已註冊，我們已寄出重設連結；請查看收件匣與垃圾郵件。"
            showResetDialog = false
            Log.i("ResetPW", "reset email SENT to $mail")
        } catch (e: Exception) {
            val code = (e as? FirebaseAuthException)?.errorCode ?: "UNKNOWN"
            Log.e("ResetPW", "FAILED code=$code, msg=${e.message}", e)

            formMessage = when (code) {
                "ERROR_INVALID_EMAIL" -> "Email 格式不正確"
                "ERROR_TOO_MANY_REQUESTS" -> "請求過於頻繁，稍後再試"
                "ERROR_NETWORK_REQUEST_FAILED" -> "網路連線錯誤，請檢查網路"
                else -> "寄送失敗（$code）"
            }
        } finally {
            resetLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
    ) {
        AnimatedGradientBackground(
            modifier = Modifier.matchParentSize(),
            successProgress = successProgress
        )

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
                                        if (!loading && !loginSuccess) scope.launch { login() }
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
                        enabled = !loading && !loginSuccess && email.isNotBlank() && password.isNotBlank(),
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
                        enabled = !loading && !loginSuccess,
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
                        enabled = !loading && !loginSuccess
                    ) {
                        Text("忘記密碼？", color = PrimaryBlue, textDecoration = TextDecoration.Underline)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showResetDialog) {
        Dialog(onDismissRequest = { if (!resetLoading) showResetDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(22.dp))
            ) {
                AnimatedGradientBackground(
                    modifier = Modifier.matchParentSize(),
                    successProgress = 0f   // 重設密碼對話框不要進入成功動畫
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.White.copy(alpha = 0.35f))
                        .border(1.dp, PrimaryBlue.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                )

                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("重設密碼", style = MaterialTheme.typography.titleMedium, color = Color(0xFF0F172A))
                    Text("請輸入你的註冊 Email，我們會寄送重設密碼連結給你。", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF475569))

                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it.replace(" ", "").replace("\n", "") },
                        singleLine = true,
                        enabled = !resetLoading,
                        label = { Text("電子郵件", color = Color(0xFF475569)) },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = Color(0xFF475569)) },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
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
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryBlue)
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