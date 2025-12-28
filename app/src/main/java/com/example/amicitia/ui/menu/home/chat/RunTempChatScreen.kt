package com.example.amicitia.ui.menu.home.chat

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.navOptions
import coil3.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs

private val BgDark = Color(0xFF1E1E1E)
private val PrimaryBlue = Color(0xFF3F51B5)

// 卡片可讀性
private val CardBg = Color.White.copy(alpha = 0.14f)
private val CardTitle = Color.White.copy(alpha = 0.85f)
private val CardSub = Color.White.copy(alpha = 0.75f)
private val CardHint = Color.White.copy(alpha = 0.70f)
private val CardIcon = Color.White.copy(alpha = 0.80f)

@Composable
private fun AuthBackground(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(BgDark)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PrimaryBlue.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
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

    // 對方 uid：從 members 取（最準）
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

    // 目標設定 BottomSheet
    var showGoalSheet by remember { mutableStateOf(false) }

    // /help 只在送出 /help 時顯示
    var showHelp by remember { mutableStateOf(false) }

    // Ready 狀態
    val myReady = room.ready[meUid] == true
    val otherReadyNow = otherUid.isNotBlank() && room.ready[otherUid] == true

    // ✅ 新增：避免重複 navigate（兩人都 /finish 時只跳一次）
    var hasNavigated by rememberSaveable(sessionId) { mutableStateOf(false) }

    // ✅ 兩邊都就緒 -> 跳到 run_solo（並清掉 run_multi 返回堆疊）
    LaunchedEffect(myReady, otherReadyNow) {
        if (hasNavigated) return@LaunchedEffect
        if (myReady && otherReadyNow) {
            hasNavigated = true
            navController.navigate(
                "run_solo",
                navOptions {
                    launchSingleTop = true
                    popUpTo("run_multi") { inclusive = true }
                }
            )
        }
    }

    // 預設時間（用於第一次還沒設定時）
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
                        placeholder = {
                            Text(
                                text = "訊息（/help）",
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        },
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

                                if (lower == "/help") {
                                    showHelp = true
                                    input = ""
                                    return@launch
                                }

                                if (lower == "/finish") {
                                    runCatching {
                                        repo.setReady(sessionId, meUid, true)
                                    }.onFailure {
                                        Log.e("RunTempChat", "finish failed", it)
                                        pushSnack("就緒失敗")
                                    }
                                    input = ""
                                    return@launch
                                }

                                if (lower == "/unfinish") {
                                    runCatching {
                                        repo.setReady(sessionId, meUid, false)
                                    }.onFailure {
                                        Log.e("RunTempChat", "unfinish failed", it)
                                        pushSnack("取消就緒失敗")
                                    }
                                    input = ""
                                    return@launch
                                }

                                if (lower.startsWith("/locate ") || lower.startsWith("/location ")) {
                                    val place = raw.substringAfter(' ').trim()
                                    if (place.isBlank()) {
                                        pushSnack("請輸入地點，例如：/locate 台北101")
                                        return@launch
                                    }
                                    runCatching {
                                        repo.updateGoal(sessionId, place = place)
                                    }.onFailure {
                                        Log.e("RunTempChat", "update place failed", it)
                                        pushSnack("更新地點失敗")
                                    }
                                    input = ""
                                    return@launch
                                }

                                if (lower.startsWith("/time ")) {
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
                                    runCatching {
                                        repo.updateGoal(sessionId, hour = hh, minute = mm)
                                    }.onFailure {
                                        Log.e("RunTempChat", "update time failed", it)
                                        pushSnack("更新時間失敗")
                                    }
                                    input = ""
                                    return@launch
                                }

                                runCatching {
                                    repo.sendMessage(sessionId, meUid, raw)
                                }.onFailure {
                                    Log.e("RunTempChat", "send failed", it)
                                    pushSnack("送出失敗")
                                }
                                input = ""
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
                        onClick = { showGoalSheet = true }
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

        if (showGoalSheet) {
            var draftPlace by remember(room.goalPlace) { mutableStateOf(room.goalPlace) }
            var draftHour by remember(room.goalStartHour) { mutableStateOf(room.goalStartHour.coerceIn(0, 23)) }
            var draftMinute by remember(room.goalStartMinute) { mutableStateOf(room.goalStartMinute.coerceIn(0, 59)) }

            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { showGoalSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF222222),
                contentColor = Color.White,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 4.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp)
                        .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "跑步目標",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )

                    Text(text = "地點", color = CardSub)
                    OutlinedTextField(
                        value = draftPlace,
                        onValueChange = { draftPlace = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "例如：河濱公園 / 校園操場",
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedBorderColor = PrimaryBlue.copy(alpha = 0.8f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Text(text = "開始時間", color = CardSub)

                    WheelTimeInline(
                        hour = draftHour,
                        minute = draftMinute,
                        onHourChange = { draftHour = it },
                        onMinuteChange = { draftMinute = it }
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                runCatching {
                                    repo.updateGoal(
                                        sessionId = sessionId,
                                        place = draftPlace.trim(),
                                        hour = draftHour,
                                        minute = draftMinute
                                    )
                                }.onFailure {
                                    Log.e("RunTempChat", "updateGoal failed", it)
                                    pushSnack("設定失敗")
                                    return@launch
                                }
                                showGoalSheet = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("設定完成")
                    }
                }
            }
        }

        if (showHelp) {
            AlertDialog(
                onDismissRequest = { showHelp = false },
                confirmButton = {
                    TextButton(onClick = { showHelp = false }) { Text("知道了") }
                },
                title = { Text("指令") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("/finish  就緒")
                        Text("/unfinish  取消就緒")
                        Text("/locate 台北101  設定地點")
                        Text("/time 06:30  設定時間")
                    }
                },
                containerColor = Color(0xFF222222),
                titleContentColor = Color.White,
                textContentColor = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun RunGoalCard(
    place: String,
    startHour: Int,
    startMinute: Int,
    myReady: Boolean,
    otherReady: Boolean,
    onClick: () -> Unit
) {
    val hh = startHour.toString().padStart(2, '0')
    val mm = startMinute.toString().padStart(2, '0')
    val hasPlace = place.trim().isNotEmpty()

    val statusText = when {
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
                    tint = CardIcon
                )
            }

            Text(
                text = if (hasPlace) place.trim() else "未設定地點",
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

@Composable
private fun WheelTimeInline(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    val itemHeight = 44.dp
    val visibleCount = 5
    val containerHeight = itemHeight * visibleCount

    val colWidth = 128.dp
    val gap = 24.dp
    val colonWidth = 20.dp

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.10f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(containerHeight)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(colWidth)
                        .height(itemHeight)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryBlue)
                )

                Spacer(Modifier.width(gap))

                Box(
                    modifier = Modifier.width(colonWidth),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ":",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(Modifier.width(gap))

                Box(
                    modifier = Modifier
                        .width(colWidth)
                        .height(itemHeight)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryBlue)
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                WheelColumn(
                    width = colWidth,
                    rangeMax = 23,
                    initialValue = hour.coerceIn(0, 23),
                    itemHeight = itemHeight,
                    visibleCount = visibleCount,
                    onValueChange = onHourChange
                )

                Spacer(Modifier.width(gap))

                Box(
                    modifier = Modifier.width(colonWidth),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ":",
                        color = Color.Transparent,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(Modifier.width(gap))

                WheelColumn(
                    width = colWidth,
                    rangeMax = 59,
                    initialValue = minute.coerceIn(0, 59),
                    itemHeight = itemHeight,
                    visibleCount = visibleCount,
                    onValueChange = onMinuteChange
                )
            }
        }
    }
}

@Composable
private fun WheelColumn(
    width: Dp,
    rangeMax: Int,
    initialValue: Int,
    itemHeight: Dp,
    visibleCount: Int,
    onValueChange: (Int) -> Unit
) {
    val paddingCount = visibleCount / 2
    val values = remember(rangeMax, paddingCount) {
        val list = mutableListOf<Int?>()
        repeat(paddingCount) { list.add(null) }
        for (v in 0..rangeMax) list.add(v)
        repeat(paddingCount) { list.add(null) }
        list
    }

    val startIndex = remember(initialValue, paddingCount) { initialValue + paddingCount }
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = (startIndex - paddingCount).coerceAtLeast(0)
    )

    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val density = LocalDensity.current

    LaunchedEffect(state, values, paddingCount, itemHeight) {
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collect { (idx, offsetPx) ->
                val itemPx = with(density) { itemHeight.toPx() }.coerceAtLeast(1f)
                val shift = if (offsetPx / itemPx >= 0.5f) 1 else 0
                val centerIndex = idx + paddingCount + shift
                val v = values.getOrNull(centerIndex) ?: return@collect
                onValueChange(v)
            }
    }

    LazyColumn(
        modifier = Modifier.width(width),
        state = state,
        flingBehavior = fling,
        contentPadding = PaddingValues(vertical = 0.dp),
        verticalArrangement = Arrangement.Center
    ) {
        items(values.size) { i ->
            val v = values[i]
            val text = v?.toString()?.padStart(2, '0') ?: ""

            val center = state.firstVisibleItemIndex + paddingCount
            val dist = abs(i - center).coerceAtMost(paddingCount + 1)

            val alpha = when (dist) {
                0 -> 1.0f
                1 -> 0.55f
                2 -> 0.28f
                else -> 0.16f
            }

            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = Color.White.copy(alpha = alpha),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
            }
        }
    }
}