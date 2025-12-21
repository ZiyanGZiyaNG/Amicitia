package com.example.amicitia.ui.menu.chat

class StartChatHelper(
    private val repo: ChatRepository = ChatRepository()
) {
    fun startTwoPeopleChat(myUid: String, otherUid: String, onDone: (roomId: String?) -> Unit) {
        val roomId = repo.roomIdForTwoUids(myUid, otherUid)
        repo.ensureRoom(roomId, listOf(myUid, otherUid)) { ok, _ ->
            onDone(if (ok) roomId else null)
        }
    }
}