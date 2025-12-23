package com.example.amicitia.ui.menu.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatRoomViewModel(
    private val repo: ChatRepository = ChatRepository(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _messagesDesc = MutableStateFlow<List<Message>>(emptyList())
    val messagesDesc: StateFlow<List<Message>> = _messagesDesc.asStateFlow()

    private val _otherNickname = MutableStateFlow("使用者")
    val otherNickname: StateFlow<String> = _otherNickname.asStateFlow()

    private val _otherAvatarUrl = MutableStateFlow("")
    val otherAvatarUrl: StateFlow<String> = _otherAvatarUrl.asStateFlow()

    private var msgJob: Job? = null
    private var currentRoomId: String? = null

    private var roomReg: ListenerRegistration? = null
    private var otherUserReg: ListenerRegistration? = null

    /**
     * 進入房間後：
     * 1) 監聽 messages
     * 2) 讀 rooms/{roomId} 抓 members -> 找對方 uid
     * 3) 監聽 users/{otherUid} -> nickname/avatarUrl
     */
    fun start(roomId: String, myUid: String) {
        if (currentRoomId == roomId && msgJob?.isActive == true) return
        currentRoomId = roomId

        // 先停掉舊監聽
        msgJob?.cancel()
        roomReg?.remove()
        otherUserReg?.remove()
        roomReg = null
        otherUserReg = null

        // 1) 監聽訊息（Firestore）
        msgJob = viewModelScope.launch {
            repo.observeMessages(roomId).collect { _messagesDesc.value = it } // desc：新到舊
        }

        // 2) 監聽 room 文件，找對方 uid
        roomReg = db.collection("rooms")
            .document(roomId)
            .addSnapshotListener { snap, _ ->
                val members = snap?.get("members") as? List<*>
                val uids = members?.mapNotNull { it as? String }.orEmpty()
                val otherUid = uids.firstOrNull { it != myUid } ?: ""

                if (otherUid.isBlank()) {
                    _otherNickname.value = "使用者"
                    _otherAvatarUrl.value = ""
                    return@addSnapshotListener
                }

                // 3) 監聽對方 user 文件（即時更新 nickname / avatarUrl）
                otherUserReg?.remove()
                otherUserReg = db.collection("users")
                    .document(otherUid)
                    .addSnapshotListener { uSnap, _ ->
                        val nn = uSnap?.getString("nickname").orEmpty().ifBlank { "使用者" }
                        val av = uSnap?.getString("avatarUrl").orEmpty()
                        _otherNickname.value = nn
                        _otherAvatarUrl.value = av
                    }
            }
    }

    fun markRead(roomId: String, myUid: String) {
        repo.markRead(roomId, myUid)
    }

    fun send(roomId: String, myUid: String, text: String, onDone: (Boolean) -> Unit) {
        val t = text.trim()
        if (t.isEmpty()) return
        repo.sendTextMessage(roomId, myUid, t) { ok, _ -> onDone(ok) }
    }

    override fun onCleared() {
        super.onCleared()
        msgJob?.cancel()
        roomReg?.remove()
        otherUserReg?.remove()
    }
}