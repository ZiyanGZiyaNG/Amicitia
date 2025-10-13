/* 登入 UI + 有動畫 + 防止 .com 換行 */
package com.example.amicitia.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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

val PrimaryBlue = Color(0xFF3F51B5)

@Composable
fun LoginLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.logo),
        contentDescription = "App Logo",
        modifier = modifier.size(150.dp)
    )
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
            formMessage = e.message ?: "登入失敗，請稍後再試"
        } finally {
            loading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF6FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoginLogo(modifier = Modifier.padding(bottom = 8.dp))

            Text("歡迎回來", style = MaterialTheme.typography.headlineSmall, color = Color.Black)
            Text("請登入以繼續", color = Color.Black, style = MaterialTheme.typography.bodyMedium)

            // Email 欄
            OutlinedTextField(
                value = email,
                onValueChange = {
                    val sanitized = it.replace("\n", "").replace(" ", "")
                    email = sanitized
                    fieldErrorEmail = null
                },
                label = { Text("電子郵件") },
                singleLine = true,
                maxLines = 1,
                isError = fieldErrorEmail != null,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
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

            // 密碼欄動畫出現
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
                        singleLine = true,
                        maxLines = 1,
                        isError = fieldErrorPassword != null,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val icon = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(icon, contentDescription = if (showPassword) "隱藏密碼" else "顯示密碼")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(pwFocus),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { if (!loading) scope.launch { login() } }
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

            Spacer(modifier = Modifier.weight(1f))

            // 登入按鈕
            Button(
                onClick = { scope.launch { login() } },
                enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
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

            // 註冊按鈕
            OutlinedButton(
                onClick = { navController.navigate(Routes.REGISTER) },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, PrimaryBlue),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
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
                            formMessage = e.message ?: "寄送失敗，請確認 Email 是否正確"
                        }
                    }
                },
                enabled = email.isNotBlank()
            ) {
                Text("忘記密碼？", color = PrimaryBlue, textDecoration = TextDecoration.Underline)
            }

            AnimatedVisibility(visible = formMessage != null) {
                Text(formMessage!!, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}