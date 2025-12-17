package com.example.amicitia.ui.menu.home.run

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.amicitia.session.SessionPresence
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import java.util.UUID
import kotlinx.coroutines.delay

data class MultiCandidate(
    val uid: String,
    val lastChanged: Long? = null
)

data class IncomingInvite(
    val inviteId: String,
    val fromUid: String,
    val createdAt: Long? = null
)

private enum class InviteUiState {
    Idle, Sending, Sent
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiRunScreen(navController: NavHostController) {

    val auth = Firebase.auth
    val meUid = auth.currentUser?.uid

    val db = SessionPresence.db
    val statusRef = remember { db.getReference("status") }
    val invitesRef = remember { db.getReference("invites") }
    val invitesSentRef = remember { db.getReference("invitesSent") }
    val sessionsRef = remember { db.getReference("sessions") }

    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var toastMsg by remember { mutableStateOf<String?>(null) }

    var candidates by remember { mutableStateOf<List<MultiCandidate>>(emptyList()) }
    var incomingInvite by remember { mutableStateOf<IncomingInvite?>(null) }

    // UI：每個 uid 的邀請狀態（送出中 / 已送出）
    val inviteUiMap = remember { mutableStateMapOf<String, InviteUiState>() }

    // 進入：multi_run；離開：idle（除非你已進房）
    DisposableEffect(meUid) {
        if (meUid != null) SessionPresence.setInMultiRun()
        onDispose { SessionPresence.setIdle() }
    }

    // 讀在線且 availability=multi_run 的人
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

    // 收邀請：invites/{meUid}
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

    // 邀請者監聽：invitesSent/{meUid}，看對方是否 accepted -> 直接進房
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
                        toastMsg = "對方已拒絕邀請"
                    }
                }

                if (status != "accepted") return
                val sessionId = snapshot.child("sessionId").getValue(String::class.java) ?: return

                SessionPresence.setInSession()
                navController.navigate("run_session/$sessionId") { launchSingleTop = true }
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // 如果你重新進頁面，sent 可能已經存在 pending
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
            toastMsg = "你尚未登入"
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
                toastMsg = "已送出邀請"
            }
            .addOnFailureListener { e ->
                inviteUiMap[targetUid] = InviteUiState.Idle
                toastMsg = "邀請失敗：${e.message ?: "unknown"}"
                Log.e("MultiRun", "invite failed", e)
            }
    }

    fun acceptInvite(inv: IncomingInvite) {
        if (meUid == null) return

        val sessionId = UUID.randomUUID().toString()
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

                SessionPresence.setInSession()
                navController.navigate("run_session/$sessionId") { launchSingleTop = true }
            }
            .addOnFailureListener { e ->
                toastMsg = "接受邀請失敗：${e.message ?: "unknown"}"
                Log.e("MultiRun", "accept failed", e)
            }
    }

    fun declineInvite(inv: IncomingInvite) {
        if (meUid == null) return

        invitesRef.child(meUid).child(inv.inviteId).updateChildren(mapOf("status" to "declined"))
        invitesSentRef.child(inv.fromUid).child(inv.inviteId).updateChildren(mapOf("status" to "declined"))

        incomingInvite = null
        toastMsg = "已拒絕邀請"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("多人跑步") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
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

            // Header 狀態列
            ElevatedCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DirectionsRun, contentDescription = null)
                    Spacer(Modifier.width(10.dp))

                    Column(Modifier.weight(1f)) {
                        Text("多人跑步大廳", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (meUid == null) "尚未登入" else "已登入，等待其他人加入",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AssistChip(
                        onClick = {},
                        label = { Text("人數 ${candidates.size}") }
                    )
                }
            }

            if (errorMsg != null) {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            errorMsg!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // 收到邀請：做成「明顯」卡片（你現在就是要做 UI）
            incomingInvite?.let { inv ->
                ElevatedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("收到邀請", style = MaterialTheme.typography.titleMedium)

                        Text(
                            "來自：${inv.fromUid}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { declineInvite(inv) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("拒絕")
                            }

                            Button(
                                onClick = { acceptInvite(inv) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("接受")
                            }
                        }
                    }
                }
            }

            // 名單區
            if (loading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) { CircularProgressIndicator() }
            } else {
                Text(
                    "正在多人跑步頁的人數：${candidates.size}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (candidates.isEmpty()) {
                    ElevatedCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("目前沒有其他人在大廳", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "提醒：對方也要進入「多人跑步」頁面才會出現在名單，才能互相邀請。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(candidates, key = { it.uid }) { u ->
                            CandidateCardV2(
                                uid = u.uid,
                                uiState = inviteUiMap[u.uid] ?: InviteUiState.Idle,
                                onInvite = { sendInvite(u.uid) }
                            )
                        }
                    }
                }
            }

            // 簡單 toast（你後面可以換 SnackbarHost）
            if (toastMsg != null) {
                ElevatedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(toastMsg!!, modifier = Modifier.weight(1f))
                    }
                }

                LaunchedEffect(toastMsg) {
                    delay(1500)
                    toastMsg = null
                }
            }
        }
    }
}

@Composable
private fun CandidateCardV2(
    uid: String,
    uiState: InviteUiState,
    onInvite: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(uid, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                Text(
                    "狀態：多人跑步大廳",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (uiState) {
                InviteUiState.Idle -> {
                    Button(onClick = onInvite) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("邀請")
                    }
                }

                InviteUiState.Sending -> {
                    FilledTonalButton(onClick = {}, enabled = false) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("送出中")
                    }
                }

                InviteUiState.Sent -> {
                    FilledTonalButton(onClick = {}, enabled = false) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("已送出")
                    }
                }
            }
        }
    }
}