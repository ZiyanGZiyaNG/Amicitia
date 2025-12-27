package com.example.amicitia.ui.menu.home.chat

import android.util.Log
import android.view.ContextThemeWrapper
import android.widget.NumberPicker
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.util.Calendar
import android.graphics.drawable.ColorDrawable
import android.os.Build

private val BgDark = Color(0xFF1E1E1E)
private val PrimaryBlue = Color(0xFF3F51B5)

// 讓卡片「一定看得到」的安全值
private val CardBg = Color.White.copy(alpha = 0.14f)
private val CardTitle = Color.White.copy(alpha = 0.85f)
private val CardSub = Color.White.copy(alpha = 0.75f)
private val CardHint = Color.White.copy(alpha = 0.70f)
private val CardIcon = Color.White.copy(alpha = 0.80f)

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

    val otherUid = remember(messages, meUid) {
        messages.firstOrNull { it.senderUid.isNotBlank() && it.senderUid != meUid }?.senderUid.orEmpty()
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
        scope.launch { snackbar.showSnackbar(msg) }
    }

    // ===== 目標狀態：地點＋開始時間（滾輪）=====
    var goalPlace by remember { mutableStateOf("") }

    val now = remember {
        Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) to it.get(Calendar.MINUTE) }
    }
    var goalStartHour by remember { mutableStateOf(now.first) }
    var goalStartMinute by remember { mutableStateOf(now.second) }

    var showGoalSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BgDark,
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
                // ✅ 修正：輸入文字改白色（含游標、placeholder）
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "輸入訊息",
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
                        val msg = input.trim()
                        if (msg.isBlank()) return@Button
                        scope.launch {
                            runCatching {
                                repo.sendMessage(sessionId, meUid, msg)
                            }.onFailure {
                                Log.e("RunTempChat", "send failed", it)
                                pushSnack("送出失敗")
                            }
                        }
                        input = ""
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
                    place = goalPlace,
                    startHour = goalStartHour,
                    startMinute = goalStartMinute,
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

    // ===== BottomSheet：地點＋滾輪時間（直接顯示滾輪）=====
    if (showGoalSheet) {
        var draftPlace by remember(goalPlace) { mutableStateOf(goalPlace) }
        var draftHour by remember(goalStartHour) { mutableStateOf(goalStartHour) }
        var draftMinute by remember(goalStartMinute) { mutableStateOf(goalStartMinute) }

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

                Text(text = "跑去哪裡", color = CardSub)
                // ✅ 修正：這個輸入框也一起白字，避免又黑字
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

                Text(text = "幾點開始", color = CardSub)

                WheelTimeInline(
                    hour = draftHour,
                    minute = draftMinute,
                    onHourChange = { draftHour = it },
                    onMinuteChange = { draftMinute = it }
                )

                Button(
                    onClick = {
                        goalPlace = draftPlace.trim()
                        goalStartHour = draftHour
                        goalStartMinute = draftMinute
                        showGoalSheet = false
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
}

@Composable
private fun RunGoalCard(
    place: String,
    startHour: Int,
    startMinute: Int,
    onClick: () -> Unit
) {
    val hasGoal = place.isNotBlank()
    val hh = startHour.toString().padStart(2, '0')
    val mm = startMinute.toString().padStart(2, '0')

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
                    text = "🏃 跑步目標",
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
                text = if (hasGoal) "📍 ${place.trim()}" else "尚未設定跑步目標",
                color = Color.White.copy(alpha = 0.90f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (hasGoal) {
                Text(
                    text = "🕒 $hh:$mm 開始",
                    color = CardSub,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "點擊設定地點與時間",
                    color = CardHint,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
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
    val context = LocalContext.current
    val highlightHeight = 46.dp

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.10f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
        ) {
            // ===== 選中列：跟「設定完成」同色 =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .height(highlightHeight)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryBlue)
                )
                Spacer(Modifier.width(24.dp))
                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .height(highlightHeight)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryBlue)
                )
            }

            // ===== 滾輪本體（移除內建藍線：selectionDividerHeight = 0）=====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AndroidView(
                    modifier = Modifier.width(128.dp),
                    factory = {
                        NumberPicker(ContextThemeWrapper(context, android.R.style.Theme_Holo_Dialog_NoActionBar)).apply {
                            minValue = 0
                            maxValue = 23
                            value = hour
                            setFormatter { v -> v.toString().padStart(2, '0') }
                            wrapSelectorWheel = true
                            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                            selectionDividerHeight = 0
                            setOnValueChangedListener { _, _, newVal -> onHourChange(newVal) }
                        }
                    },
                    update = { if (it.value != hour) it.value = hour }
                )

                Text(
                    text = ":",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleLarge
                )

                AndroidView(
                    modifier = Modifier.width(128.dp),
                    factory = {
                        NumberPicker(ContextThemeWrapper(context, android.R.style.Theme_Holo_Dialog_NoActionBar)).apply {
                            minValue = 0
                            maxValue = 59
                            value = minute
                            setFormatter { v -> v.toString().padStart(2, '0') }
                            wrapSelectorWheel = true
                            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                            selectionDividerHeight = 0
                            setOnValueChangedListener { _, _, newVal -> onMinuteChange(newVal) }
                        }
                    },
                    update = { if (it.value != minute) it.value = minute }
                )
            }
        }
    }
}