package com.example.amicitia.ui.menu.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.Locale

/* ---------------- Theme (跟 Register/Home 同一套) ---------------- */

private val PrimaryBlue = Color(0xFF3F51B5)
private val BgDark = Color(0xFF1E1E1E)

private val CardSolidGray = Color(0xFF2A2A2A)
private val CardBorder = Color.White.copy(alpha = 0.10f)

private val AvatarBg = Color(0xFF3A3A3A)

/* ---------------- Background (跟 Register 同概念：底部光暈) ---------------- */

@Composable
private fun AuthBackground(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(BgDark)) {
        BottomDecorBackground(
            modifier = Modifier.matchParentSize(),
            tint = PrimaryBlue
        )
    }
}

@Composable
private fun BottomDecorBackground(
    modifier: Modifier = Modifier,
    tint: Color
) {
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    tint.copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.88f),
                radius = h * 0.75f
            )
        )
    }
}

/* ---------------- Screen ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    roomId: String,
    onBack: () -> Unit,
    vm: ChatRoomViewModel = viewModel()
) {
    val myUid = Firebase.auth.currentUser?.uid ?: return

    LaunchedEffect(roomId, myUid) { vm.start(roomId, myUid) }
    LaunchedEffect(roomId, myUid) { vm.markRead(roomId, myUid) }

    val otherNickname by vm.otherNickname.collectAsState()
    val otherAvatarUrl by vm.otherAvatarUrl.collectAsState() // 先保留資料（你沒 Coil 先不載圖）
    val messagesDesc by vm.messagesDesc.collectAsState()
    val messagesAsc = remember(messagesDesc) { messagesDesc.asReversed() }

    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(messagesAsc.size) {
        if (messagesAsc.isNotEmpty()) {
            listState.animateScrollToItem(messagesAsc.size - 1)
        }
    }

    // 跟 Register/Home 一致的深色實心輸入匡
    val homeFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        cursorColor = Color.White.copy(alpha = 0.92f),

        focusedLabelColor = Color.White.copy(alpha = 0.80f),
        unfocusedLabelColor = Color.White.copy(alpha = 0.65f),

        focusedBorderColor = Color.White.copy(alpha = 0.22f),
        unfocusedBorderColor = Color.White.copy(alpha = 0.14f),

        focusedLeadingIconColor = Color.White.copy(alpha = 0.80f),
        unfocusedLeadingIconColor = Color.White.copy(alpha = 0.70f),
        focusedTrailingIconColor = Color.White.copy(alpha = 0.80f),
        unfocusedTrailingIconColor = Color.White.copy(alpha = 0.70f),

        focusedContainerColor = CardSolidGray,
        unfocusedContainerColor = CardSolidGray,
        disabledContainerColor = CardSolidGray.copy(alpha = 0.75f),
        errorContainerColor = CardSolidGray
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .systemBarsPadding()
            .imePadding()
    ) {
        AuthBackground(modifier = Modifier.matchParentSize())

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarCircle(nickname = otherNickname, avatarUrl = otherAvatarUrl)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = otherNickname.ifBlank { "使用者" },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "back",
                                tint = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                )
            }
        ) { padding ->

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messagesAsc, key = { it.id }) { msg ->
                        val isMe = msg.senderId == myUid
                        MessageBubble(msg = msg, isMe = isMe)
                    }
                    item { Spacer(Modifier.height(6.dp)) }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

                // 輸入列（同 Home/Register：實心灰 + 白字）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("輸入訊息...", color = Color.White.copy(alpha = 0.60f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = homeFieldColors
                    )

                    Spacer(Modifier.width(10.dp))

                    // 送出按鈕：同色系（PrimaryBlue）
                    IconButton(
                        onClick = {
                            val toSend = input.trim()
                            if (toSend.isBlank()) return@IconButton

                            vm.send(roomId, myUid, toSend) { ok ->
                                if (ok) input = ""
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "send",
                            tint = PrimaryBlue
                        )
                    }
                }
            }
        }
    }
}

/* ---------------- UI Pieces ---------------- */

@Composable
private fun AvatarCircle(
    nickname: String,
    avatarUrl: String
) {
    // 你目前沒有 Coil，所以先一律用首字頭像；avatarUrl 先保留資料
    val initial =
        nickname.trim().firstOrNull()?.toString()?.uppercase(Locale.getDefault()) ?: "?"

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(AvatarBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun MessageBubble(msg: Message, isMe: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        // 我方/對方不同灰度，但都維持深色系，不用白底
        val bubbleColor = if (isMe) {
            CardSolidGray
        } else {
            CardSolidGray.copy(alpha = 0.92f)
        }

        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Text(
                text = msg.text,
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}