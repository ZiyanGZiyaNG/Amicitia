/* 登入 UI + 動態漸層背景（呼吸效果） */
package com.example.amicitia.ui.login

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.amicitia.R
import com.example.amicitia.nav.Routes
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.draw.drawBehind


val PrimaryBlue = Color(0xFF3F51B5)

@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    // 兩組較有差異的淺色，仍維持你的品牌調性
    aStart: Color = Color(0xFFF3F6FF),
    aMid:   Color = Color(0xFFEAF1FF),
    aEnd:   Color = Color(0xFFDDE7FF),
    bStart: Color = Color(0xFFE8F0FF),
    bMid:   Color = Color(0xFFD6E3FF),
    bEnd:   Color = Color(0xFFCBD9FF),
    durationMs: Int = 7000
) {
    val infinite = rememberInfiniteTransition()
    // 0..1 緩慢來回（線性即可，搭配 cos 變成平滑 in/out）
    val tRaw by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    // 轉成 0..1 的平滑曲線（cosine ease）
    val t = (1f - kotlin.math.cos(tRaw * Math.PI).toFloat()) / 2f

    // 顏色也在兩組之間來回
    val c1 = lerp(aStart, bStart, t)
    val c2 = lerp(aMid,   bMid,   t)
    val c3 = lerp(aEnd,   bEnd,   t)

    // 用 drawBehind 拿到實際 size，讓漸層端點位置也跟著位移（「角度在呼吸」）
    Box(
        modifier = modifier.drawBehind {
            val sx = size.width  * (0.15f + 0.35f * t)   // 0.15 → 0.50
            val sy = size.height * (0.10f + 0.25f * (1f - t)) // 0.35 → 0.10
            val ex = size.width  * (0.85f - 0.35f * t)   // 0.85 → 0.50
            val ey = size.height * (0.90f - 0.25f * (1f - t)) // 0.65 → 0.90

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
            modifier = Modifier.fillMaxSize().padding(12.dp)
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
    val scope = rememberCoroutineScope()

    val pwFocus = remember { FocusRequester() }
    val auth = Firebase.auth

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
            val msg = e.message ?: ""
            formMessage = when {
                "The supplied auth credential is incorrect" in msg -> "帳號或密碼錯誤"
                "There is no user record" in msg -> "此帳號不存在"
                "password is invalid" in msg -> "密碼錯誤，請再試一次"
                "too many requests" in msg -> "嘗試次數過多，請稍後再試"
                "network error" in msg.lowercase() -> "網路連線錯誤，請檢查網路"
                else -> "登入失敗，請稍後再試"
            }
        } finally {
            loading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
    ) {
        // 動態漸層背景
        AnimatedGradientBackground(modifier = Modifier.matchParentSize())

        // 主要內容
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
                        keyboardActions = KeyboardActions(onNext = { pwFocus.requestFocus() })
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
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (!loading) scope.launch { login() }
                                })
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
                            modifier = Modifier.fillMaxWidth().animateContentSize()
                        )
                    }

                    Button(
                        onClick = { scope.launch { login() } },
                        enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
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
                        modifier = Modifier.fillMaxWidth().height(52.dp),
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
                            scope.launch {
                                try {
                                    Firebase.auth.sendPasswordResetEmail(email.trim()).await()
                                    formMessage = "已寄出重設密碼信到 $email"
                                } catch (e: Exception) {
                                    val msg = e.message ?: ""
                                    formMessage = when {
                                        "no user record" in msg -> "查無此帳號"
                                        else -> "寄送失敗，請確認 Email 是否正確"
                                    }
                                }
                            }
                        },
                        enabled = email.isNotBlank()
                    ) {
                        Text("忘記密碼？", color = PrimaryBlue, textDecoration = TextDecoration.Underline)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}