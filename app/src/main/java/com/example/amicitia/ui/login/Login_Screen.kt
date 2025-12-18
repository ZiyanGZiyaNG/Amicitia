package com.example.amicitia.ui.login

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.amicitia.R
import com.example.amicitia.nav.Routes
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

private val PrimaryBlue = Color(0xFF3F51B5)
private val BgDark = Color(0xFF1E1E1E)


@Composable
private fun AuthBackground(
    modifier: Modifier = Modifier,
    successProgress: Float = 0f
) {
    Box(
        modifier = modifier
            .background(BgDark)
    ) {
        BottomDecorBackground(
            modifier = Modifier.matchParentSize(),
            tint = PrimaryBlue,
            successProgress = successProgress
        )
    }
}

@Composable
private fun BottomDecorBackground(
    modifier: Modifier = Modifier,
    tint: Color = PrimaryBlue,
    successProgress: Float = 0f
) {
    val infinite = rememberInfiniteTransition(label = "login_bottom")
    val drift by infinite.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "login_drift"
    )

    val alphaFactor = 1f - successProgress

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    tint.copy(alpha = 0.14f * alphaFactor),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.88f + drift),
                radius = h * 0.75f
            )
        )
    }
}


@Composable
private fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerDp: Dp = 26.dp,
    shadowDp: Dp = 16.dp,
    frostAlpha: Float = 0.24f,
    borderAlpha: Float = 0.40f,
    innerBorderAlpha: Float = 0.16f,
    noiseAlpha: Float = 0.035f,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerDp)

    Box(
        modifier = modifier
            .shadow(shadowDp, shape, clip = false)
            .clip(shape)
            .drawBehind {
                val cornerPx = cornerDp.toPx()

                val glow = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.35f),
                    radius = size.minDimension * 0.95f
                )
                drawRoundRect(
                    brush = glow,
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    size = size
                )

                drawRoundRect(
                    color = Color.White.copy(alpha = frostAlpha),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    size = size
                )

                val highlight = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.26f),
                        Color.White.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    start = Offset(size.width * -0.22f, size.height * 0.02f),
                    end = Offset(size.width * 0.92f, size.height * 0.90f)
                )
                drawRoundRect(
                    brush = highlight,
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    size = size
                )

                val depth = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.10f)
                    ),
                    startY = size.height * 0.30f,
                    endY = size.height
                )
                drawRoundRect(
                    brush = depth,
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    size = size
                )

                if (noiseAlpha > 0f) {
                    val dotCount = (size.width * size.height / 9000f).toInt().coerceIn(40, 180)
                    repeat(dotCount) {
                        val x = Random.nextFloat() * size.width
                        val y = Random.nextFloat() * size.height
                        val r = Random.nextFloat().coerceIn(0.6f, 1.4f)
                        drawCircle(
                            color = Color.White.copy(alpha = noiseAlpha),
                            radius = r,
                            center = Offset(x, y)
                        )
                    }
                }
            }
            .border(1.dp, Color.White.copy(alpha = borderAlpha), shape)
            .padding(1.dp)
            .border(1.dp, Color.White.copy(alpha = innerBorderAlpha), shape)
            .padding(contentPadding)
    ) {
        content()
    }
}

@Composable
fun LoginLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(112.dp)
            .shadow(elevation = 10.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.94f))
            .border(1.dp, Color.White.copy(alpha = 0.60f), CircleShape)
            .padding(2.dp)
            .border(1.dp, PrimaryBlue.copy(alpha = 0.16f), CircleShape),
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

    val glassFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        cursorColor = Color.White.copy(alpha = 0.95f),

        focusedLabelColor = Color.White.copy(alpha = 0.90f),
        unfocusedLabelColor = Color.White.copy(alpha = 0.78f),

        focusedBorderColor = Color.White.copy(alpha = 0.65f),
        unfocusedBorderColor = Color.White.copy(alpha = 0.38f),

        focusedLeadingIconColor = Color.White.copy(alpha = 0.90f),
        unfocusedLeadingIconColor = Color.White.copy(alpha = 0.80f),
        focusedTrailingIconColor = Color.White.copy(alpha = 0.90f),
        unfocusedTrailingIconColor = Color.White.copy(alpha = 0.80f),

        focusedContainerColor = Color.White.copy(alpha = 0.16f),
        unfocusedContainerColor = Color.White.copy(alpha = 0.12f),
        errorContainerColor = Color.White.copy(alpha = 0.12f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .systemBarsPadding()
            .imePadding()
    ) {
        AuthBackground(
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
            Text("歡迎回來", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text("請登入以繼續", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.70f))

            Spacer(Modifier.height(20.dp))

            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerDp = 26.dp,
                shadowDp = 16.dp,
                frostAlpha = 0.24f,
                borderAlpha = 0.40f,
                innerBorderAlpha = 0.16f,
                noiseAlpha = 0.035f,
                contentPadding = 16.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

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
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { pwFocus.requestFocus() }),
                        colors = glassFieldColors
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
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(pwFocus),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = { if (!loading && !loginSuccess) scope.launch { login() } }
                                ),
                                colors = glassFieldColors
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
                            color = if (isErrorMsg) MaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.92f),
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
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue.copy(alpha = 0.88f),
                            contentColor = Color.White
                        )
                    ) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
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
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.04f),
                            contentColor = Color.White
                        )
                    ) { Text("註冊") }

                    TextButton(
                        onClick = {
                            resetEmail = email
                            showResetDialog = true
                        },
                        enabled = !loading && !loginSuccess
                    ) {
                        Text(
                            "忘記密碼？",
                            color = Color.White.copy(alpha = 0.92f),
                            textDecoration = TextDecoration.Underline
                        )
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
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
            ) {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerDp = 24.dp,
                    shadowDp = 16.dp,
                    frostAlpha = 0.28f,
                    borderAlpha = 0.42f,
                    innerBorderAlpha = 0.16f,
                    noiseAlpha = 0.030f,
                    contentPadding = 18.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("重設密碼", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(
                            "請輸入你的註冊 Email，我們會寄送重設密碼連結給你。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.78f)
                        )

                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it.replace(" ", "").replace("\n", "") },
                            singleLine = true,
                            enabled = !resetLoading,
                            label = { Text("電子郵件") },
                            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                            shape = RoundedCornerShape(18.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            colors = glassFieldColors
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { if (!resetLoading) showResetDialog = false },
                                enabled = !resetLoading
                            ) { Text("取消", color = Color.White.copy(alpha = 0.92f)) }

                            TextButton(
                                onClick = {
                                    if (resetEmail.isBlank() || !resetEmail.contains("@")) {
                                        formMessage = "請輸入正確的 Email"
                                        return@TextButton
                                    }
                                    scope.launch { sendPasswordReset(resetEmail) }
                                },
                                enabled = !resetLoading
                            ) {
                                if (resetLoading) {
                                    CircularProgressIndicator(
                                        Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text("寄送", color = Color.White.copy(alpha = 0.96f))
                            }
                        }
                    }
                }
            }
        }
    }
}