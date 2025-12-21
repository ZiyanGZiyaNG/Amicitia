package com.example.amicitia.ui.menu.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatRoomViewModel(
    private val repo: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _messagesDesc = MutableStateFlow<List<Message>>(emptyList())
    val messagesDesc: StateFlow<List<Message>> = _messagesDesc.asStateFlow()

    private var job: Job? = null
    private var currentRoomId: String? = null

    fun start(roomId: String) {
        if (currentRoomId == roomId && job?.isActive == true) return
        currentRoomId = roomId
        job?.cancel()

        job = viewModelScope.launch {
            repo.observeMessages(roomId).collect { _messagesDesc.value = it }
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
}