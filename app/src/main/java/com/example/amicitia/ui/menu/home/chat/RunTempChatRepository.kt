package com.example.amicitia.ui.menu.home.chat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class RunTempMessage(
    val id: String,
    val senderUid: String,
    val text: String,
    val type: String = "text",
    val createdAt: Timestamp? = null
)

class RunTempChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val roomsCol get() = db.collection("run_temp_rooms")

    suspend fun ensureRoom(sessionId: String, members: List<String>) {
        val ref = roomsCol.document(sessionId)
        val snap = ref.get().await()
        if (snap.exists()) return

        val now = FieldValue.serverTimestamp()
        val membersClean = members.distinct().filter { it.isNotBlank() }

        val data = hashMapOf(
            "sessionId" to sessionId,
            "status" to "active",
            "members" to membersClean,
            "createdAt" to now,
            "lastMessageAt" to now,
            "lastMessageText" to "",
            "lastMessageType" to "text",
            "lastSenderUid" to "",
            "deletedFor" to membersClean.associateWith { false },
            "lastReadAt" to membersClean.associateWith { FieldValue.serverTimestamp() }
        )

        ref.set(data).await()
    }

    fun observeMessages(sessionId: String): Flow<List<RunTempMessage>> = callbackFlow {
        val reg = roomsCol.document(sessionId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snap.documents.map { d ->
                    RunTempMessage(
                        id = d.id,
                        senderUid = d.getString("senderUid") ?: "",
                        text = d.getString("text") ?: "",
                        type = d.getString("type") ?: "text",
                        createdAt = d.getTimestamp("createdAt")
                    )
                }
                trySend(list)
            }

        awaitClose { reg.remove() }
    }

    suspend fun sendMessage(sessionId: String, senderUid: String, text: String) {
        val roomRef = roomsCol.document(sessionId)
        val msgRef = roomRef.collection("messages").document()

        val msgData = hashMapOf(
            "senderUid" to senderUid,
            "text" to text,
            "type" to "text",
            "createdAt" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()
        batch.set(msgRef, msgData)
        batch.update(
            roomRef, mapOf(
                "lastMessageText" to text,
                "lastMessageType" to "text",
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "lastSenderUid" to senderUid
            )
        )
        batch.update(roomRef, "lastReadAt.$senderUid", FieldValue.serverTimestamp())
        batch.commit().await()
    }

    fun getUserProfileOnce(uid: String, onDone: (nickname: String, avatarUrl: String) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { d ->
                val nickname = d.getString("nickname").orEmpty().ifBlank { "使用者" }
                val avatarUrl = d.getString("avatarUrl").orEmpty()
                onDone(nickname, avatarUrl)
            }
            .addOnFailureListener {
                onDone("使用者", "")
            }
    }
}