/* 註冊 UI ─ 與登入頁一致的風格（呼吸漸層 + 底部裝飾）
 * 逐步顯示：Email → 暱稱 → 密碼 → 確認密碼 → 勾選/按鈕
 * 使用者手動點選下一欄（不自動跳焦點）
 */
package com.example.amicitia.ui.register

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import com.example.amicitia.ui.login.AnimatedGradientBackground
import com.example.amicitia.ui.login.PrimaryBlue

@Composable
fun RegisterScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirmation by remember { mutableStateOf("") }
    var agreeTos by remember { mutableStateOf(false) }

    var showPassword by remember { mutableStateOf(false) }
    var showPassword2 by remember { mutableStateOf(false) }

    var fieldErrorEmail by remember { mutableStateOf<String?>(null) }
    var fieldErrorPassword by remember { mutableStateOf<String?>(null) }
    var fieldErrorConfirm by remember { mutableStateOf<String?>(null) }
    var fieldErrorNickname by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val auth = Firebase.auth

    // 螢幕高度控制位置（中間偏上）
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val topOffset = (screenHeight * 0.22f).coerceAtLeast(40.dp)  // 從原本 0.10f 改為 0.22f，看起來更居中

    // 逐步顯示邏輯
    val emailLooksValid by derivedStateOf {
        Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim())
    }
    val canShowNickname by derivedStateOf { emailLooksValid }
    val canShowPassword by derivedStateOf { canShowNickname && nickname.isNotBlank() }
    val canShowConfirm  by derivedStateOf { canShowPassword && password.isNotBlank() }
    val passwordsMatch  by derivedStateOf { canShowConfirm && passwordConfirmation == password && passwordConfirmation.isNotBlank() }
    val canShowAgreeAndButtons by derivedStateOf { passwordsMatch }

    fun submitUiOnly(): Boolean {
        fieldErrorEmail = null
        fieldErrorPassword = null
        fieldErrorConfirm = null
        fieldErrorNickname = null

        var ok = true
        if (!emailLooksValid) { fieldErrorEmail = "電子郵件格式錯誤"; ok = false }
        if (nickname.isBlank()) { fieldErrorNickname = "請輸入暱稱"; ok = false }
        if (password.isBlank()) { fieldErrorPassword = "請輸入密碼"; ok = false }
        if (passwordConfirmation != password) { fieldErrorConfirm = "兩次輸入的密碼不一致"; ok = false }
        if (!agreeTos) ok = false
        return ok
    }

    suspend fun registerToFirebase(email: String, password: String, nickname: String) {
        try {
            isLoading = true
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: error("User is null after registration")

            val profile = UserProfileChangeRequest.Builder().setDisplayName(nickname).build()
            user.updateProfile(profile).await()
            user.sendEmailVerification().await()

            snackbarHostState.showSnackbar("註冊成功！驗證信已寄到 $email，請點信中的連結完成驗證（可能在垃圾郵件，寄件者為 noreply）。")
            navController.navigate("login") {
                popUpTo("register") { inclusive = true }
                launchSingleTop = true
            }
        } catch (e: Exception) {
            snackbarHostState.showSnackbar(e.message ?: "註冊失敗，請稍候再試")
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
    ) {
        // 背景（呼吸漸層 + 底部柔光）
        AnimatedGradientBackground(modifier = Modifier.matchParentSize())
        BottomDecorBackground(modifier = Modifier.matchParentSize())

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0)
        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(topOffset))

                Text("歡迎註冊", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF0F172A))
                Text("請依序填寫欄位", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF475569))
                Spacer(Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 2.dp,
                    shadowElevation = 8.dp,
                    color = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Email
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; fieldErrorEmail = null },
                            label = { Text("電子郵件") },
                            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                            singleLine = true,
                            isError = fieldErrorEmail != null,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        AnimatedVisibility(visible = fieldErrorEmail != null) {
                            Text(fieldErrorEmail ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        // 暱稱
                        AnimatedVisibility(
                            visible = canShowNickname,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                OutlinedTextField(
                                    value = nickname,
                                    onValueChange = { nickname = it; fieldErrorNickname = null },
                                    label = { Text("暱稱") },
                                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                                    singleLine = true,
                                    isError = fieldErrorNickname != null,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                AnimatedVisibility(visible = fieldErrorNickname != null) {
                                    Text(fieldErrorNickname ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        // 密碼
                        AnimatedVisibility(
                            visible = canShowPassword,
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
                                            Icon(imageVector = icon, contentDescription = null)
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                AnimatedVisibility(visible = fieldErrorPassword != null) {
                                    Text(fieldErrorPassword ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        // 確認密碼
                        AnimatedVisibility(
                            visible = canShowConfirm,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                OutlinedTextField(
                                    value = passwordConfirmation,
                                    onValueChange = { passwordConfirmation = it; fieldErrorConfirm = null },
                                    label = { Text("再次輸入密碼") },
                                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                                    singleLine = true,
                                    isError = fieldErrorConfirm != null,
                                    visualTransformation = if (showPassword2) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        val icon = if (showPassword2) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                        IconButton(onClick = { showPassword2 = !showPassword2 }) {
                                            Icon(imageVector = icon, contentDescription = null)
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                AnimatedVisibility(visible = fieldErrorConfirm != null) {
                                    Text(fieldErrorConfirm ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        // 勾選 + 按鈕
                        AnimatedVisibility(
                            visible = canShowAgreeAndButtons,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Checkbox(
                                        checked = agreeTos,
                                        onCheckedChange = { agreeTos = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = PrimaryBlue,
                                            checkmarkColor = Color.White,
                                            uncheckedColor = Color.Gray
                                        )
                                    )
                                    Text("我同意服務條款", color = Color(0xFF334155))
                                }

                                Button(
                                    onClick = {
                                        if (submitUiOnly()) {
                                            scope.launch {
                                                registerToFirebase(email.trim(), password, nickname.trim())
                                            }
                                        }
                                    },
                                    enabled = !isLoading && agreeTos,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.White)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                        Spacer(Modifier.width(8.dp))
                                        Text("處理中…")
                                    } else {
                                        Text("註冊")
                                    }
                                }

                                OutlinedButton(
                                    onClick = { navController.popBackStack() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.6f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = PrimaryBlue
                                    )
                                ) { Text("返回") }
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
                Text(
                    text = "Power By ZiyanGZiyaNG",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/* ---------- 背景柔光 + 波浪 ---------- */
@Composable
private fun BottomDecorBackground(
    modifier: Modifier = Modifier,
    tint: Color = PrimaryBlue
) {
    val infinite = rememberInfiniteTransition()
    val drift by infinite.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 柔光
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(tint.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.88f + drift),
                radius = h * 0.75f
            )
        )

        // 波浪
        val y = h * 0.60f + drift
        val wave = Path().apply {
            moveTo(0f, y)
            quadraticTo(w * 0.20f, y - 28f, w * 0.40f, y - 6f)
            quadraticTo(w * 0.70f, y + 24f, w, y - 4f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(
            path = wave,
            brush = Brush.verticalGradient(
                colors = listOf(
                    tint.copy(alpha = 0.10f),
                    tint.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                startY = y - 40f, endY = h
            )
        )
    }
}