package com.example.amicitia.ui.menu.home.run

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.navOptions
import com.example.amicitia.session.SessionPresence
import com.example.amicitia.ui.menu.home.chat.RunTempChatRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import java.util.UUID
import kotlinx.coroutines.launch

data class MultiCandidate(
    val uid: String,
    val lastChanged: Long? = null
)

data class IncomingInvite(
    val inviteId: String,
    val fromUid: String,
    val createdAt: Long? = null
)

private enum class InviteUiState { Idle, Sending, Sent }

/* ---------- Theme tokens（跟 HomeRoute 完全同一套） ---------- */

private val BgDark = Color(0xFF1E1E1E)
private val PrimaryBlue = Color(0xFF3F51B5)
private val SolidGray = Color(0xFF2A2A2A)

private val TitleText = Color.White.copy(alpha = 0.92f)
private val BodyText = Color.White.copy(alpha = 0.68f)

/* ---------- Pure-color button tokens（統一按鈕格式） ---------- */

private val BtnPrimary = PrimaryBlue
private val BtnSecondary = SolidGray
private val BtnDisabled = Color(0xFF3A3A3A)
private val BtnText = Color.White
private val BtnShape = RoundedCornerShape(12.dp)
private val BtnHeight = 44.dp

/* ---------- Background（跟 HomeRoute 同一套） ---------- */

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

/* ---------- Solid card（顏色/陰影/圓角 跟 HomeRoute 的 SolidColorCard 一致） ---------- */

@Composable
private fun SolidCard(
    modifier: Modifier = Modifier,
    cornerDp: Dp = 24.dp,
    contentPadding: Dp = 16.dp,
    backgroundColor: Color = SolidGray,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerDp)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onClick() }
                } else Modifier
            )
            .padding(contentPadding)
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiRunScreen(outerNavController: NavHostController) {

    val auth = Firebase.auth
    val meUid = auth.currentUser?.uid

    val db = SessionPresence.db
    val statusRef = remember { db.getReference("status") }
    val invitesRef = remember { db.getReference("invites") }
    val invitesSentRef = remember { db.getReference("invitesSent") }
    val sessionsRef = remember { db.getReference("sessions") }

    val runTempRepo = remember { RunTempChatRepository() }

    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var candidates by remember { mutableStateOf<List<MultiCandidate>>(emptyList()) }
    var incomingInvite by remember { mutableStateOf<IncomingInvite?>(null) }

    val inviteUiMap = remember { mutableStateMapOf<String, InviteUiState>() }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun pushSnack(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(message = msg, withDismissAction = true) }
    }

    DisposableEffect(meUid) {
        if (meUid != null) SessionPresence.setInMultiRun()
        onDispose { SessionPresence.setIdle() }
    }

    DisposableEffect(meUid) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<MultiCandidate>()
                for (child in snapshot.children) {
                    val uid = child.key ?: continue
                    if (uid == meUid) continue

                    val state = child.child("state").getValue(String::class.java) ?: "offline"
                    val availability = child.child("availability").getValue(String::class.java) ?: "idle"
                    val lastChanged = child.child("last_changed").getValue(Long::class.java)

                    if (state == "online" && availability == "multi_run") {
                        list.add(MultiCandidate(uid = uid, lastChanged = lastChanged))
                    }
                }
                candidates = list.sortedByDescending { it.lastChanged ?: 0L }
                loading = false
                errorMsg = null
            }

            override fun onCancelled(error: DatabaseError) {
                loading = false
                errorMsg = "讀取多人跑步在線名單失敗：${error.message}"
                Log.e("MultiRun", "status cancelled: ${error.message}", error.toException())
            }
        }

        statusRef.addValueEventListener(listener)
        onDispose { statusRef.removeEventListener(listener) }
    }

    DisposableEffect(meUid) {
        if (meUid == null) return@DisposableEffect onDispose { }

        val myInvRef = invitesRef.child(meUid)

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val sport = snapshot.child("sport").getValue(String::class.java) ?: return
                val status = snapshot.child("status").getValue(String::class.java) ?: "pending"
                if (sport != "run" || status != "pending") return

                val fromUid = snapshot.child("fromUid").getValue(String::class.java) ?: return
                val createdAt = snapshot.child("createdAt").getValue(Long::class.java)

                incomingInvite = IncomingInvite(
                    inviteId = snapshot.key ?: return,
                    fromUid = fromUid,
                    createdAt = createdAt
                )
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val status = snapshot.child("status").getValue(String::class.java) ?: return
                if (status != "pending" && incomingInvite?.inviteId == snapshot.key) {
                    incomingInvite = null
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                if (incomingInvite?.inviteId == snapshot.key) incomingInvite = null
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("MultiRun", "invite listener cancelled: ${error.message}", error.toException())
            }
        }

        myInvRef.addChildEventListener(listener)
        onDispose { myInvRef.removeEventListener(listener) }
    }

    DisposableEffect(meUid) {
        if (meUid == null) return@DisposableEffect onDispose { }

        val mySentRef = invitesSentRef.child(meUid)
        val listener = object : ChildEventListener {

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val sport = snapshot.child("sport").getValue(String::class.java) ?: return
                if (sport != "run") return

                val status = snapshot.child("status").getValue(String::class.java) ?: return

                val toUid = snapshot.child("toUid").getValue(String::class.java)
                if (toUid != null) {
                    if (status == "pending") inviteUiMap[toUid] = InviteUiState.Sent
                    if (status == "declined") {
                        inviteUiMap[toUid] = InviteUiState.Idle
                        pushSnack("對方已拒絕邀請")
                    }
                }

                if (status != "accepted") return
                val sessionId = snapshot.child("sessionId").getValue(String::class.java).orEmpty()
                if (sessionId.isBlank()) {
                    pushSnack("sessionId 讀取失敗，無法進入聊天室")
                    return
                }

                val otherUid = snapshot.child("toUid").getValue(String::class.java).orEmpty()
                val members = listOf(meUid, otherUid).filter { it.isNotBlank() }

                scope.launch {
                    try {
                        runTempRepo.ensureRoom(sessionId = sessionId, members = members)
                    } catch (e: Exception) {
                        Log.e("MultiRun", "ensureRoom failed", e)
                    }

                    SessionPresence.setInSession()
                    outerNavController.navigate(
                        "run_temp_chat/$sessionId",
                        navOptions { launchSingleTop = true }
                    )
                }
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val toUid = snapshot.child("toUid").getValue(String::class.java) ?: return
                val status = snapshot.child("status").getValue(String::class.java) ?: "pending"
                if (status == "pending") inviteUiMap[toUid] = InviteUiState.Sent
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("MultiRun", "invitesSent listener cancelled: ${error.message}", error.toException())
            }
        }

        mySentRef.addChildEventListener(listener)
        onDispose { mySentRef.removeEventListener(listener) }
    }

    fun sendInvite(targetUid: String) {
        if (meUid == null) {
            pushSnack("你尚未登入")
            return
        }

        inviteUiMap[targetUid] = InviteUiState.Sending

        val inviteId = UUID.randomUUID().toString()
        val data = mapOf(
            "fromUid" to meUid,
            "toUid" to targetUid,
            "sport" to "run",
            "status" to "pending",
            "createdAt" to ServerValue.TIMESTAMP
        )

        invitesRef.child(targetUid).child(inviteId).setValue(data)
            .addOnSuccessListener {
                invitesSentRef.child(meUid).child(inviteId).setValue(data)
                inviteUiMap[targetUid] = InviteUiState.Sent
                pushSnack("已送出邀請")
            }
            .addOnFailureListener { e ->
                inviteUiMap[targetUid] = InviteUiState.Idle
                pushSnack("邀請失敗：${e.message ?: "unknown"}")
                Log.e("MultiRun", "invite failed", e)
            }
    }

    fun acceptInvite(inv: IncomingInvite) {
        if (meUid == null) return

        val sessionId = UUID.randomUUID().toString()
        if (sessionId.isBlank()) {
            pushSnack("sessionId 生成失敗，無法進入聊天室")
            return
        }

        val sessionData = mapOf(
            "sport" to "run",
            "createdAt" to ServerValue.TIMESTAMP,
            "state" to "active",
            "members" to mapOf(meUid to true, inv.fromUid to true)
        )

        sessionsRef.child(sessionId).setValue(sessionData)
            .addOnSuccessListener {

                invitesRef.child(meUid).child(inv.inviteId).updateChildren(
                    mapOf("status" to "accepted", "sessionId" to sessionId)
                )

                invitesSentRef.child(inv.fromUid).child(inv.inviteId).updateChildren(
                    mapOf("status" to "accepted", "sessionId" to sessionId)
                )

                val members = listOf(meUid, inv.fromUid).filter { it.isNotBlank() }

                scope.launch {
                    try {
                        runTempRepo.ensureRoom(sessionId = sessionId, members = members)
                    } catch (e: Exception) {
                        Log.e("MultiRun", "ensureRoom failed", e)
                    }

                    SessionPresence.setInSession()
                    outerNavController.navigate(
                        "run_temp_chat/$sessionId",
                        navOptions { launchSingleTop = true }
                    )
                }
            }
            .addOnFailureListener { e ->
                pushSnack("接受邀請失敗：${e.message ?: "unknown"}")
                Log.e("MultiRun", "accept failed", e)
            }
    }

    fun declineInvite(inv: IncomingInvite) {
        if (meUid == null) return

        invitesRef.child(meUid).child(inv.inviteId).updateChildren(mapOf("status" to "declined"))
        invitesSentRef.child(inv.fromUid).child(inv.inviteId).updateChildren(mapOf("status" to "declined"))

        incomingInvite = null
        pushSnack("已拒絕邀請")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        AuthBackground(Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("多人跑步", color = TitleText) },
                    navigationIcon = {
                        IconButton(onClick = { outerNavController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = TitleText
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TitleText,
                        navigationIconContentColor = TitleText
                    )
                )
            }
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                SolidCard(
                    backgroundColor = SolidGray,
                    contentPadding = 16.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = PrimaryBlue
                        )
                        Spacer(Modifier.width(10.dp))

                        Column(Modifier.weight(1f)) {
                            Text("多人跑步大廳", style = MaterialTheme.typography.titleMedium, color = TitleText)
                            Text(
                                if (meUid == null) "尚未登入" else "已登入，等待其他人加入",
                                style = MaterialTheme.typography.bodySmall,
                                color = BodyText
                            )
                        }
                    }
                }

                errorMsg?.let { msg ->
                    SolidCard(
                        backgroundColor = SolidGray,
                        contentPadding = 16.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                msg,
                                color = TitleText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                incomingInvite?.let { inv ->
                    SolidCard(
                        backgroundColor = SolidGray,
                        contentPadding = 16.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("收到邀請", style = MaterialTheme.typography.titleMedium, color = TitleText)

                            Text(
                                "來自：${inv.fromUid}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = BodyText
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                                Button(
                                    onClick = { declineInvite(inv) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(BtnHeight),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BtnSecondary,
                                        contentColor = BtnText
                                    ),
                                    shape = BtnShape
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("拒絕")
                                }

                                Button(
                                    onClick = { acceptInvite(inv) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(BtnHeight),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BtnPrimary,
                                        contentColor = BtnText
                                    ),
                                    shape = BtnShape
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("接受")
                                }
                            }
                        }
                    }
                }

                if (loading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) { CircularProgressIndicator(color = PrimaryBlue) }
                } else {

                    Text(
                        "正在多人跑步頁的人數：${candidates.size}",
                        style = MaterialTheme.typography.titleSmall,
                        color = BodyText
                    )

                    if (candidates.isEmpty()) {
                        SolidCard(
                            backgroundColor = SolidGray,
                            contentPadding = 16.dp
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("目前沒有其他人在大廳", style = MaterialTheme.typography.titleMedium, color = TitleText)
                                Text(
                                    "提醒：對方也要進入「多人跑步」頁面才會出現在名單，才能互相邀請。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BodyText
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(candidates, key = { it.uid }) { u ->
                                CandidateCardSolid(
                                    uid = u.uid,
                                    uiState = inviteUiMap[u.uid] ?: InviteUiState.Idle,
                                    onInvite = { sendInvite(u.uid) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateCardSolid(
    uid: String,
    uiState: InviteUiState,
    onInvite: () -> Unit
) {
    SolidCard(
        backgroundColor = SolidGray,
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    uid,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = TitleText
                )
                Text(
                    "狀態：多人跑步大廳",
                    style = MaterialTheme.typography.bodySmall,
                    color = BodyText
                )
            }

            when (uiState) {

                InviteUiState.Idle -> {
                    Button(
                        onClick = onInvite,
                        modifier = Modifier.height(BtnHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BtnPrimary,
                            contentColor = BtnText
                        ),
                        shape = BtnShape
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("邀請")
                    }
                }

                InviteUiState.Sending -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.height(BtnHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BtnDisabled,
                            contentColor = BtnText
                        ),
                        shape = BtnShape
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = BtnText
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("送出中")
                    }
                }

                InviteUiState.Sent -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.height(BtnHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BtnDisabled,
                            contentColor = BtnText
                        ),
                        shape = BtnShape
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("已送出")
                    }
                }
            }
        }
    }
}