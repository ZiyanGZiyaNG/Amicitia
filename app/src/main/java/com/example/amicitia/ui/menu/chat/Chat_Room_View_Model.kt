package com.example.amicitia.ui.menu.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class ChatRoomViewModel(
    private val repo: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _otherNickname = MutableStateFlow("使用者")
    val otherNickname: StateFlow<String> = _otherNickname.asStateFlow()

    private val _otherAvatarUrl = MutableStateFlow("")
    val otherAvatarUrl: StateFlow<String> = _otherAvatarUrl.asStateFlow()

    private val _messagesDesc = MutableStateFlow<List<Message>>(emptyList())
    val messagesDesc: StateFlow<List<Message>> = _messagesDesc.asStateFlow()

    private val _otherTyping = MutableStateFlow(false)
    val otherTyping: StateFlow<Boolean> = _otherTyping.asStateFlow()

    private var msgJob: Job? = null
    private var typingJob: Job? = null

    private var currentRoomId: String? = null
    private var currentMyUid: String? = null
    private var otherUid: String? = null

    fun start(roomId: String, myUid: String) {
        if (currentRoomId == roomId && currentMyUid == myUid && msgJob?.isActive == true) return
        currentRoomId = roomId
        currentMyUid = myUid

        msgJob?.cancel()
        typingJob?.cancel()

        // 先抓 room -> 找 otherUid -> 抓對方 profile
        repo.getRoomOnce(roomId) { room ->
            if (room == null) return@getRoomOnce
            val ouid = room.members.firstOrNull { it != myUid } ?: return@getRoomOnce
            otherUid = ouid

            repo.getUserProfileOnce(ouid) { nick, avatar ->
                _otherNickname.value = nick
                _otherAvatarUrl.value = avatar
            }

            // 監聽訊息
            msgJob = viewModelScope.launch {
                repo.observeMessages(roomId).collect { list ->
                    _messagesDesc.value = list
                }
            }

            // 監聽 typingAt
            typingJob = viewModelScope.launch {
                repo.observeTypingAt(roomId).collect { typingMap ->
                    val ts = typingMap[ouid]
                    _otherTyping.value = isRecent(ts, withinMs = 4000)
                }
            }
        }
    }

    fun send(roomId: String, myUid: String, text: String, onDone: (ok: Boolean) -> Unit) {
        repo.sendTextMessage(roomId, myUid, text) { ok, _ ->
            // 送出後順便清掉 typing，避免你這邊一直顯示輸入狀態
            repo.clearTyping(roomId, myUid)
            onDone(ok)
        }
    }

    fun markRead(roomId: String, myUid: String) {
        repo.markRead(roomId, myUid)
    }

    fun onTyping(roomId: String, myUid: String) {
        repo.setTyping(roomId, myUid)
    }

    fun onStopTyping(roomId: String, myUid: String) {
        repo.clearTyping(roomId, myUid)
    }

    override fun onCleared() {
        msgJob?.cancel()
        typingJob?.cancel()
        super.onCleared()
    }

    private fun isRecent(ts: com.google.firebase.Timestamp?, withinMs: Long): Boolean {
        if (ts == null) return false
        val diff = Date().time - ts.toDate().time
        return diff in 0..withinMs
    }
}