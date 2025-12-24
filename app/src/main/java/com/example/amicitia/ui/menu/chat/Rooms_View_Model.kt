package com.example.amicitia.ui.menu.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RoomsViewModel(
    private val repo: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    private var job: Job? = null
    private var currentUid: String? = null

    fun start(myUid: String) {
        if (currentUid == myUid && job?.isActive == true) return
        currentUid = myUid
        job?.cancel()

        job = viewModelScope.launch {
            repo.observeRooms(myUid).collect { _rooms.value = it }
        }
    }

    fun hasUnread(room: Room, myUid: String): Boolean {
        val lastMessageAt = room.lastMessageAt ?: return false
        val lastReadAt = room.lastReadAt?.get(myUid) ?: return true

        return lastMessageAt.seconds > lastReadAt.seconds ||
                (lastMessageAt.seconds == lastReadAt.seconds &&
                        lastMessageAt.nanoseconds > lastReadAt.nanoseconds)
    }
}