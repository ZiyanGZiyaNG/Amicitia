/*登入 UI*/
package com.example.amicitia.ui.login
//
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.amicitia.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.NavController

// 主色系
val PrimaryBlue = Color(0xFF3F51B5)

// LOGO
@Composable
fun LoginLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.logo),
        contentDescription = "App Logo",
        modifier = modifier.size(150.dp)
    )
}

// 主程式
@Composable
fun LoginScreen(navController: NavController)
{
    // 變數
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var fieldErrorEmail by remember { mutableStateOf<String?>(null) }
    var fieldErrorPassword by remember { mutableStateOf<String?>(null) }
    var formMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun submitUiOnly(): Boolean
    {
        formMessage = null
        fieldErrorEmail = null
        fieldErrorPassword = null
        var ok = true
        if (email.isBlank())
        {
            fieldErrorEmail = "請輸入電子郵件"; ok = false
        }
        else if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email))
        {
            fieldErrorEmail = "電子郵件格式錯誤"; ok = false
        }
        if (password.isBlank())
        {
            fieldErrorPassword = "請輸入密碼"; ok = false
        }
        if (!ok) return false
        loading = true
        scope.launch{
            delay(300)
            loading = false
            formMessage = null
        }
        return true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF6FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoginLogo(modifier = Modifier.padding(bottom = 8.dp))

            Text(
                "歡迎回來",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Black
            )
            Text(
                "請登入以繼續",
                color = Color.Black,
                style = MaterialTheme.typography.bodyMedium
            )

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
                Text(
                    fieldErrorEmail ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; fieldErrorPassword = null },
                label = { Text("密碼") },
                singleLine = true,
                isError = fieldErrorPassword != null,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon =
                        if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    val desc = if (showPassword) "隱藏密碼" else "顯示密碼"
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(icon, contentDescription = desc)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            AnimatedVisibility(visible = fieldErrorPassword != null) {
                Text(
                    fieldErrorPassword ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PrimaryBlue,
                            checkmarkColor = Color.White,
                            uncheckedColor = Color.Gray
                        )
                    )

                    Spacer(Modifier.width(8.dp))
                    Text("記住我", color = Color.Black)
                }
                TextButton(onClick = { /* TODO: 忘記密碼流程 */ }) {
                    Text(
                        "忘記密碼？",
                        color = PrimaryBlue,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }

            val loginInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = {
                    if (submitUiOnly()) { /* TODO: 驗證成功後導頁，如 navController.navigate("Home") */ }
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .indication(loginInteraction, null),
                shape = RoundedCornerShape(24.dp),
                interactionSource = loginInteraction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    disabledContainerColor = PrimaryBlue,
                    disabledContentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("處理中…")
                } else {
                    Text("登入")
                }
            }

            val registerInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = { navController.navigate("Register") },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .indication(registerInteraction, null),
                shape = RoundedCornerShape(24.dp),
                interactionSource = registerInteraction,
                border = BorderStroke(1.dp, PrimaryBlue),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = PrimaryBlue,
                    disabledContentColor = PrimaryBlue
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp)
            ) {
                Text("註冊")
            }

            Text(
                text = "Power By ZiyanGZiyaNG",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            AnimatedVisibility(visible = formMessage != null) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        formMessage!!,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}