package com.example.amicitia.ui.menu.home.chat

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.navOptions
import coil3.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.util.Calendar

// 改成你實際 NavGraph 的 route
private const val TARGET_ROUTE = "run_solo"
private const val POPUP_ROUTE = "run_multi"

private val BgDark = Color(0xFF1E1E1E)
private val PrimaryBlue = Color(0xFF3F51B5)
private val CardBg = Color.White.copy(alpha = 0.14f)
private val CardTitle = Color.White.copy(alpha = 0.85f)
private val CardSub = Color.White.copy(alpha = 0.75f)
private val CardIcon = Color.White.copy(alpha = 0.80f)

@Composable
private fun AuthBackground(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(BgDark)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryBlue.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.88f),
                    radius = h * 0.75f
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunTempChatScreen(
    navController: NavHostController,
    sessionId: String
) {
    val repo = remember { RunTempChatRepository() }
    val meUid = Firebase.auth.currentUser?.uid.orEmpty()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var input by remember { mutableStateOf("") }

    val messages by repo.observeMessages(sessionId).collectAsState(initial = emptyList())
    val room by repo.observeRoom(sessionId).collectAsState(initial = RunRoomState())

    val otherUid = remember(room.members, meUid) {
        room.members.firstOrNull { it.isNotBlank() && it != meUid }.orEmpty()
    }

    var otherNickname by remember { mutableStateOf("聊天") }
    var otherAvatarUrl by remember { mutableStateOf("") }

    LaunchedEffect(otherUid) {
       if (otherUid.isBlank()) return@LaunchedEffect
        repo.getUserProfileOnce(otherUid) { nick, avatar ->
            otherNickname = nick
            otherAvatarUrl = avatar
        }
    }

    fun pushSnack(msg: String) {
        scope.launch { snackbar.showSnackbar(msg, withDismissAction = true) }
    }

    var showHelp by remember { mutableStateOf(false) }
    var showGoalSheet by remember { mutableStateOf(false) }

    val myReady = room.ready[meUid] == true
    val otherReadyNow = otherUid.isNotBlank() && room.ready[otherUid] == true

    // 寫法 A：finish 與 ready 分離
    val myFinished = room.finished[meUid] == true
    val otherFinishedNow = otherUid.isNotBlank() && room.finished[otherUid] == true

    // 兩人都 /finish 才跳（只跳一次）
    var hasNavigated by rememberSaveable(sessionId) { mutableStateOf(false) }
    LaunchedEffect(myFinished, otherFinishedNow) {
        if (hasNavigated) return@LaunchedEffect
        if (myFinished && otherFinishedNow) {
            hasNavigated = true
            navController.navigate(
                TARGET_ROUTE,
                navOptions {
                    launchSingleTop = true
                    popUpTo(POPUP_ROUTE) { inclusive = true }
                }
            )
        }
    }

    val now = remember {
        Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) to it.get(Calendar.MINUTE) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        AuthBackground(Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = otherAvatarUrl.ifBlank { null },
                                contentDescription = null,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = otherNickname,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("訊息（/help）", color = Color.White.copy(alpha = 0.6f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.6f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Spacer(Modifier.width(10.dp))

                    Button(
                        onClick = {
                            val raw = input.trim()
                            if (raw.isBlank()) return@Button

                            scope.launch {
                                val lower = raw.lowercase()

                                when {
                                    lower == "/help" -> {
                                        showHelp = true
                                        input = ""
                                    }

                                    lower == "/finish" -> {
                                        runCatching { repo.setFinished(sessionId, meUid, true) }
                                            .onFailure {
                                                Log.e("RunTempChat", "finish failed", it)
                                                pushSnack("完成失敗")
                                            }
                                        input = ""
                                    }

                                    lower == "/unfinish" -> {
                                        runCatching { repo.setFinished(sessionId, meUid, false) }
                                            .onFailure {
                                                Log.e("RunTempChat", "unfinish failed", it)
                                                pushSnack("取消完成失敗")
                                            }
                                        input = ""
                                    }

                                    lower.startsWith("/locate ") || lower.startsWith("/location ") -> {
                                        if (myFinished) {
                                            pushSnack("你已 /finish，無法再修改目標（輸入 /unfinish 解除）")
                                            return@launch
                                        }
                                        val place = raw.substringAfter(' ').trim()
                                        if (place.isBlank()) {
                                            pushSnack("請輸入地點，例如：/locate 台北101")
                                            return@launch
                                        }
                                        runCatching { repo.updateGoal(sessionId, place = place) }
                                            .onFailure {
                                                Log.e("RunTempChat", "update place failed", it)
                                                pushSnack("更新地點失敗")
                                            }
                                        input = ""
                                    }

                                    lower.startsWith("/time ") -> {
                                        if (myFinished) {
                                            pushSnack("你已 /finish，無法再修改目標（輸入 /unfinish 解除）")
                                            return@launch
                                        }
                                        val t = raw.substringAfter(' ').trim()
                                        val parts = t.split(":")
                                        if (parts.size != 2) {
                                            pushSnack("時間格式要 HH:MM，例如：/time 06:30")
                                            return@launch
                                        }
                                        val hh = parts[0].toIntOrNull()
                                        val mm = parts[1].toIntOrNull()
                                        if (hh == null || mm == null || hh !in 0..23 || mm !in 0..59) {
                                            pushSnack("時間格式要 HH:MM，例如：/time 06:30")
                                            return@launch
                                        }
                                        runCatching { repo.updateGoal(sessionId, hour = hh, minute = mm) }
                                            .onFailure {
                                                Log.e("RunTempChat", "update time failed", it)
                                                pushSnack("更新時間失敗")
                                            }
                                        input = ""
                                    }

                                    else -> {
                                        runCatching { repo.sendMessage(sessionId, meUid, raw) }
                                            .onFailure {
                                                Log.e("RunTempChat", "send failed", it)
                                                pushSnack("送出失敗")
                                            }
                                        input = ""
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) { Text("送出") }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item {
                    RunGoalCard(
                        place = room.goalPlace,
                        startHour = if (room.goalStartHour in 0..23) room.goalStartHour else now.first,
                        startMinute = if (room.goalStartMinute in 0..59) room.goalStartMinute else now.second,
                        myReady = myReady,
                        otherReady = otherReadyNow,
                        myFinished = myFinished,
                        otherFinished = otherFinishedNow,
                        onClick = {
                            if (myFinished) pushSnack("你已 /finish，目標已鎖定（輸入 /unfinish 可解除）")
                            else showGoalSheet = true
                        }
                    )
                }

                items(messages, key = { it.id }) { m ->
                    val mine = m.senderUid == meUid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (mine) PrimaryBlue else Color.White.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = m.text,
                                color = Color.White,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("知道了") } },
            title = { Text("指令") },
            text = { Text("/help\n/locate 地點\n/time HH:MM\n/finish\n/unfinish") }
        )
    }
}

@Composable
private fun RunGoalCard(
    place: String,
    startHour: Int,
    startMinute: Int,
    myReady: Boolean,
    otherReady: Boolean,
    myFinished: Boolean,
    otherFinished: Boolean,
    onClick: () -> Unit
) {
    val hh = startHour.toString().padStart(2, '0')
    val mm = startMinute.toString().padStart(2, '0')

    val statusText = when {
        myFinished && otherFinished -> "雙方已完成"
        myFinished && !otherFinished -> "你已完成，等待對方"
        !myFinished && otherFinished -> "對方已完成"
        myReady && otherReady -> "雙方就緒"
        myReady && !otherReady -> "你已就緒，等待對方"
        !myReady && otherReady -> "對方已就緒"
        else -> "未就緒"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = CardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "跑步目標",
                    color = CardTitle,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "編輯",
                    tint = if (myFinished) CardIcon.copy(alpha = 0.35f) else CardIcon
                )
            }

            Text(
                text = place.ifBlank { "未設定地點" },
                color = Color.White.copy(alpha = 0.90f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "$hh:$mm",
                color = CardSub,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = statusText,
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}