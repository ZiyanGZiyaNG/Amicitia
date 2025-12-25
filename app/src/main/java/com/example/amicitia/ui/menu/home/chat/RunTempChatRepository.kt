package com.example.amicitia.ui.menu.home.chat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val type: String = "text",
    val createdAt: Timestamp? = null
)

class RunTempChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun roomDoc(sessionId: String) =
        db.collection("run_temp_rooms").document(sessionId)

    private fun messagesCol(sessionId: String) =
        roomDoc(sessionId).collection("messages")

    /** 確保聊天室存在（接受邀請時呼叫） */
    suspend fun ensureRoom(
        sessionId: String,
        members: List<String>
    ) {
        val ref = roomDoc(sessionId)
        val snap = ref.get().await()

        if (snap.exists()) {
            ref.update("members", FieldValue.arrayUnion(*members.toTypedArray())).await()
            return
        }

        ref.set(
            mapOf(
                "sessionId" to sessionId,
                "members" to members,
                "status" to "active",
                "createdAt" to FieldValue.serverTimestamp(),
                "lastMessageText" to "",
                "lastMessageType" to "text",
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "lastSenderUid" to "",
                "lastReadAt" to members.associateWith { FieldValue.serverTimestamp() },
                "deletedFor" to members.associateWith { false }
            )
        ).await()
    }

    fun observeMessages(sessionId: String): Flow<List<ChatMessage>> = callbackFlow {
        val reg = messagesCol(sessionId)
            .orderBy("createdAt")
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    ChatMessage(
                        id = it.id,
                        senderId = it.getString("senderId").orEmpty(),
                        text = it.getString("text").orEmpty(),
                        type = it.getString("type") ?: "text",
                        createdAt = it.getTimestamp("createdAt")
                    )
                }.orEmpty()
                trySend(list)
            }

        awaitClose { reg.remove() }
    }

    suspend fun sendMessage(
        sessionId: String,
        senderId: String,
        text: String
    ) {
        val roomRef = roomDoc(sessionId)
        val msgRef = messagesCol(sessionId).document()

        db.runBatch { batch ->
            batch.set(
                msgRef,
                mapOf(
                    "senderId" to senderId,
                    "text" to text,
                    "type" to "text",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                roomRef,
                mapOf(
                    "lastMessageText" to text,
                    "lastMessageType" to "text",
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "lastSenderUid" to senderId
                )
            )
        }.await()
    }
}