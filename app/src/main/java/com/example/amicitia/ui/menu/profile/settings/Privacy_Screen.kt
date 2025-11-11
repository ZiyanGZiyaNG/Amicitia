package com.example.amicitia.ui.menu.profile.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.PI
import kotlin.math.cos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid

    var showOnline by rememberSaveable { mutableStateOf(true) }
    var allowFriendRequests by rememberSaveable { mutableStateOf(true) }
    var shareActivity by rememberSaveable { mutableStateOf(false) }
    var profileVisibility by rememberSaveable { mutableStateOf("所有人") }

    val visibilityOptions = listOf("所有人", "僅好友", "僅自己")
    var expanded by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val mainButtonColor = Color(0xFF3F51B5)
    val lightBorderColor = Color(0xFF3F51B5)

    LaunchedEffect(uid) {
        if (uid != null) {
            try {
                val doc = db.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    showOnline = doc.getBoolean("showOnline") ?: true
                    allowFriendRequests = doc.getBoolean("allowFriendRequests") ?: true
                    shareActivity = doc.getBoolean("shareActivity") ?: false
                    profileVisibility = when (doc.getString("profileVisibility")) {
                        "friends" -> "僅好友"
                        "private" -> "僅自己"
                        else -> "所有人"
                    }
                }
            } catch (e: Exception) {
                scope.launch { snackbar.showSnackbar("載入失敗：${e.message ?: "未知錯誤"}") }
            }
        } else {
            scope.launch { snackbar.showSnackbar("尚未登入") }
        }
        isLoading = false
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
                    title = { Text("隱私與可見度") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = mainButtonColor)
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
                    SettingCard(title = "可見度設定") {
                        SettingSwitch(
                            title = "顯示上線狀態",
                            desc = "讓好友知道你目前是否在線上",
                            checked = showOnline,
                            onCheckedChange = { showOnline = it },
                            color = mainButtonColor
                        )
                        SettingSwitch(
                            title = "允許好友邀請",
                            desc = "允許其他使用者傳送好友邀請給你",
                            checked = allowFriendRequests,
                            onCheckedChange = { allowFriendRequests = it },
                            color = mainButtonColor
                        )
                        SettingSwitch(
                            title = "分享活動紀錄",
                            desc = "將你的運動與活動分享在動態牆",
                            checked = shareActivity,
                            onCheckedChange = { shareActivity = it },
                            color = mainButtonColor
                        )
                    }

                    SettingCard(title = "個人資料可見度") {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = profileVisibility,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("誰可以看到你的個人資料") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = mainButtonColor,
                                    unfocusedBorderColor = lightBorderColor,
                                    focusedLabelColor = mainButtonColor,
                                    cursorColor = mainButtonColor
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                shape = RoundedCornerShape(18.dp),
                                containerColor = Color(0xFFF6F7FF),
                                tonalElevation = 2.dp,
                                shadowElevation = 12.dp
                            ) {
                                visibilityOptions.forEach { option ->
                                    FancyDropdownItem(
                                        text = option,
                                        selected = option == profileVisibility,
                                        onClick = {
                                            profileVisibility = option
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (uid == null || isSaving) return@Button
                            isSaving = true
                            val visibilityKey = when (profileVisibility) {
                                "僅好友" -> "friends"
                                "僅自己" -> "private"
                                else -> "public"
                            }

                            val data = mapOf(
                                "showOnline" to showOnline,
                                "allowFriendRequests" to allowFriendRequests,
                                "shareActivity" to shareActivity,
                                "profileVisibility" to visibilityKey,
                                "updatedAt" to Timestamp.now()
                            )

                            db.collection("users")
                                .document(uid)
                                .set(data, SetOptions.merge())
                                .addOnSuccessListener {
                                    scope.launch { snackbar.showSnackbar("已儲存設定") }
                                    isSaving = false
                                }
                                .addOnFailureListener { e ->
                                    scope.launch {
                                        snackbar.showSnackbar("儲存失敗：${e.message ?: "未知錯誤"}")
                                    }
                                    isSaving = false
                                }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth(0.7f),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = mainButtonColor,
                            contentColor = Color.White,
                            disabledContainerColor = mainButtonColor.copy(alpha = 0.5f),
                            disabledContentColor = Color.White.copy(alpha = 0.8f)
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("儲存設定")
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun FancyDropdownItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val highlight = Color(0xFFE7ECFF)
    val content = MaterialTheme.colorScheme.onSurface
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) content.copy(alpha = 0.95f) else content.copy(alpha = 0.88f)
            )
        },
        onClick = onClick,
        leadingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color(0xFF6B57B2).copy(alpha = 0.8f)
                )
            }
        },
        colors = MenuDefaults.itemColors(
            textColor = content,
            leadingIconColor = content
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    )

    if (selected) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .drawBehind { drawRect(highlight.copy(alpha = 0.5f)) }
        )
    }
}

@Composable
fun SettingCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            content()
        }
    }
}

@Composable
fun SettingSwitch(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = color,
                    checkedThumbColor = Color.White
                )
            )
        }
        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AnimatedGradientBackground(
    modifier: Modifier = Modifier
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