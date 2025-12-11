package com.example.amicitia.ui.register

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.amicitia.ui.login.AnimatedGradientBackground
import com.example.amicitia.ui.theme.PrimaryBlue
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
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

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    var registerSuccess by remember { mutableStateOf(false) }
    val successProgress by animateFloatAsState(
        targetValue = if (registerSuccess) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1800,
            easing = FastOutSlowInEasing
        ),
        label = "register-success",
        finishedListener = { value ->
            if (value == 1f && registerSuccess) {
                navController.navigate("login") {
                    popUpTo("register") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    )

    val emailLooksValid by remember(email) {
        derivedStateOf { Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim()) }
    }
    val canShowNickname = emailLooksValid
    val canShowPassword by remember(canShowNickname, nickname) {
        derivedStateOf { canShowNickname && nickname.isNotBlank() }
    }
    val canShowConfirm by remember(canShowPassword, password) {
        derivedStateOf { canShowPassword && password.isNotBlank() }
    }
    val passwordsMatch by remember(canShowConfirm, password, passwordConfirmation) {
        derivedStateOf { canShowConfirm && passwordConfirmation == password && passwordConfirmation.isNotBlank() }
    }
    val canShowAgreeAndButtons = passwordsMatch

    val stepCount = remember(
        canShowNickname, canShowPassword, canShowConfirm, canShowAgreeAndButtons
    ) {
        (if (canShowNickname) 1 else 0) +
                (if (canShowPassword) 1 else 0) +
                (if (canShowConfirm) 1 else 0) +
                (if (canShowAgreeAndButtons) 1 else 0)
    }
    val ratio = when (stepCount) {
        0 -> 0.16f
        1 -> 0.14f
        2 -> 0.12f
        3 -> 0.10f
        else -> 0.08f
    }
    val topOffset = remember(screenHeight, ratio) {
        (screenHeight * ratio).coerceIn(16.dp, 80.dp)
    }

    fun submitUiOnly(): Boolean {
        fieldErrorEmail = null
        fieldErrorPassword = null
        fieldErrorConfirm = null
        fieldErrorNickname = null

        var ok = true
        if (!emailLooksValid) {
            fieldErrorEmail = "電子郵件格式錯誤"
            ok = false
        }
        if (nickname.isBlank()) {
            fieldErrorNickname = "請輸入暱稱"
            ok = false
        }
        if (password.isBlank()) {
            fieldErrorPassword = "請輸入密碼"
            ok = false
        }
        if (passwordConfirmation != password) {
            fieldErrorConfirm = "兩次密碼不一致"
            ok = false
        }
        if (!agreeTos) ok = false
        return ok
    }

    suspend fun registerToFirebase(email: String, password: String, nickname: String) {
        try {
            isLoading = true
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: error("User is null")

            val profile = UserProfileChangeRequest.Builder()
                .setDisplayName(nickname)
                .build()
            user.updateProfile(profile).await()

            val db = Firebase.firestore
            val doc = mapOf(
                "uid" to user.uid,
                "email" to (user.email ?: email),
                "nickname" to nickname,
                "avatarUrl" to "",
                "bio" to "",
                "showOnline" to true,
                "profileVisibility" to "public",
                "lastOnline" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(user.uid)
                .set(doc, SetOptions.merge())
                .await()

            user.sendEmailVerification().await()

            snackbarHostState.showSnackbar("註冊成功！已寫入暱稱，請查收驗證信。")

            registerSuccess = true
        } catch (e: Exception) {
            Log.e("Register", "register failed", e)
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
        // 上層兩顆球 + 爆白動畫
        AnimatedGradientBackground(
            modifier = Modifier.matchParentSize(),
            successProgress = successProgress
        )
        // 只留底部光暈，不畫波浪，避免中間那條線
        BottomDecorBackground(
            modifier = Modifier.matchParentSize(),
            tint = PrimaryBlue,
            successProgress = successProgress
        )

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "返回",
                                tint = Color(0xFF334155)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
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
                Text(
                    "歡迎註冊",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF0F172A)
                )
                Text(
                    "請依序填寫欄位",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569)
                )
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
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; fieldErrorEmail = null },
                            label = { Text("電子郵件") },
                            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                            singleLine = true,
                            isError = fieldErrorEmail != null,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        AnimatedVisibility(visible = fieldErrorEmail != null) {
                            Text(
                                fieldErrorEmail ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

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
                                    Text(
                                        fieldErrorNickname ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

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
                                        val icon =
                                            if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                        IconButton(onClick = { showPassword = !showPassword }) {
                                            Icon(imageVector = icon, contentDescription = null)
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
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
                                        val icon =
                                            if (showPassword2) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                        IconButton(onClick = { showPassword2 = !showPassword2 }) {
                                            Icon(imageVector = icon, contentDescription = null)
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                AnimatedVisibility(visible = fieldErrorConfirm != null) {
                                    Text(
                                        fieldErrorConfirm ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = canShowAgreeAndButtons,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
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
                                                registerToFirebase(
                                                    email.trim(),
                                                    password,
                                                    nickname.trim()
                                                )
                                            }
                                        }
                                    },
                                    enabled = !isLoading && agreeTos && !registerSuccess,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryBlue,
                                        contentColor = Color.White
                                    )
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                        Spacer(Modifier.size(8.dp))
                                        Text("處理中…")
                                    } else {
                                        Text("註冊")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
                Text(
                    "Power By ZiyanGZiyaNG",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun BottomDecorBackground(
    modifier: Modifier = Modifier,
    tint: Color = PrimaryBlue,
    successProgress: Float = 0f
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