/* 註冊 UI + DB */
package com.example.amicitia.ui.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.amicitia.ui.login.PrimaryBlue
import kotlinx.coroutines.launch
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

private val PrimeColor = Color(0xFF3F51B5)

@Composable
fun RegisterScreen(navController: NavController) {
    // 狀態
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

    var isLoading by remember { mutableStateOf(false) } // <--- 補上這個
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val auth = Firebase.auth

    // 驗證函式
    fun submitUiOnly(): Boolean {
        fieldErrorEmail = null
        fieldErrorPassword = null
        fieldErrorConfirm = null
        fieldErrorNickname = null

        var ok = true
        if (email.isBlank()) {
            fieldErrorEmail = "請輸入電子郵件"; ok = false
        } else if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email)) {
            fieldErrorEmail = "電子郵件格式錯誤"; ok = false
        }

        if (nickname.isBlank()) {
            fieldErrorNickname = "請輸入暱稱"; ok = false
        }

        if (password.isBlank()) {
            fieldErrorPassword = "請輸入密碼"; ok = false
        }

        if (passwordConfirmation != password) {
            fieldErrorConfirm = "兩次輸入的密碼不一致"; ok = false
        }

        if (!agreeTos) {
            ok = false
        }

        return ok
    }

    // Firebase 註冊函式
    suspend fun registerToFirebase(email: String, password: String, nickname: String) {
        try {
            isLoading = true
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: error("User is null after registration")

            val profile = UserProfileChangeRequest.Builder()
                .setDisplayName(nickname)
                .build()
            user.updateProfile(profile).await()

            snackbarHostState.showSnackbar("註冊成功，歡迎 $nickname！")
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

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF6FF))
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 60.dp)
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 標題
                Text("歡迎註冊", style = MaterialTheme.typography.headlineSmall, color = Color.Black)
                Text("請依下面指示註冊", style = MaterialTheme.typography.bodyMedium, color = Color.Black)

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; fieldErrorEmail = null },
                    label = { Text("電子郵件") },
                    singleLine = true,
                    isError = fieldErrorEmail != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedVisibility(visible = fieldErrorEmail != null) {
                    Text(fieldErrorEmail ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                // 暱稱
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it; fieldErrorNickname = null },
                    label = { Text("暱稱") },
                    singleLine = true,
                    isError = fieldErrorNickname != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedVisibility(visible = fieldErrorNickname != null) {
                    Text(fieldErrorNickname ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                // 密碼
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; fieldErrorPassword = null },
                    label = { Text("密碼") },
                    singleLine = true,
                    isError = fieldErrorPassword != null,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(imageVector = icon, contentDescription = null)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedVisibility(visible = fieldErrorPassword != null) {
                    Text(fieldErrorPassword ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                // 確認密碼
                OutlinedTextField(
                    value = passwordConfirmation,
                    onValueChange = { passwordConfirmation = it; fieldErrorConfirm = null },
                    label = { Text("再次輸入密碼") },
                    singleLine = true,
                    isError = fieldErrorConfirm != null,
                    visualTransformation = if (showPassword2) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (showPassword2) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { showPassword2 = !showPassword2 }) {
                            Icon(imageVector = icon, contentDescription = null)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedVisibility(visible = fieldErrorConfirm != null) {
                    Text(fieldErrorConfirm ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                // 條款
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Checkbox(
                        checked = agreeTos,
                        onCheckedChange = { agreeTos = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PrimeColor,
                            checkmarkColor = Color.White,
                            uncheckedColor = Color.Gray
                        )
                    )
                    Text("我同意服務條款")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 註冊按鈕
                Button(
                    onClick = {
                        if (submitUiOnly()) {
                            scope.launch {
                                registerToFirebase(email.trim(), password, nickname.trim())
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimeColor, contentColor = Color.White)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("註冊")
                    }
                }

                // 返回按鈕
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(50.dp),
                    border = BorderStroke(1.dp, PrimeColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = PrimaryBlue,
                        disabledContentColor = PrimaryBlue
                    )
                ) { Text("返回") }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer
                Text(
                    text = "Power By ZiyanGZiyaNG",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            }
        }
    }
}