package com.example.amicitia.ui.menu.chat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val roomsCol get() = db.collection("rooms")

    private fun parseLastReadAt(any: Any?): Map<String, Timestamp>? {
        val raw = any as? Map<*, *> ?: return null
        val out = mutableMapOf<String, Timestamp>()
        for ((k, v) in raw) {
            val key = k as? String ?: continue
            val ts = v as? Timestamp ?: continue
            out[key] = ts
        }
        return out
    }

    fun observeRooms(myUid: String): Flow<List<Room>> = callbackFlow {
        val reg = roomsCol
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener

                val rooms = snap.documents.mapNotNull { d ->
                    val membersRaw = d.get("members") as? List<*>
                    val members = membersRaw?.mapNotNull { it as? String }.orEmpty()
                    if (!members.contains(myUid)) return@mapNotNull null

                    val deletedFor = d.get("deletedFor") as? Map<*, *>
                    if (deletedFor?.get(myUid) == true) return@mapNotNull null

                    Room(
                        roomId = d.id,
                        members = members,
                        lastMessageAt = d.getTimestamp("lastMessageAt"),
                        lastMessageText = d.getString("lastMessageText"),
                        lastMessageType = d.getString("lastMessageType"),
                        lastReadAt = parseLastReadAt(d.get("lastReadAt"))
                    )
                }

                trySend(rooms)
            }

        awaitClose { reg.remove() }
    }

    fun observeMessages(roomId: String): Flow<List<Message>> = callbackFlow {
        val reg = roomsCol.document(roomId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener

                val msgs = snap.documents.map { d ->
                    val sender = d.getString("senderUid")
                        ?: d.getString("senderId")
                        ?: ""

                    Message(
                        id = d.id,
                        senderId = sender,
                        type = d.getString("type") ?: "text",
                        text = d.getString("text") ?: "",
                        createdAt = d.getTimestamp("createdAt")
                    )
                }

                trySend(msgs)
            }

        awaitClose { reg.remove() }
    }

    fun sendTextMessage(
        roomId: String,
        myUid: String,
        text: String,
        onDone: (ok: Boolean, err: Exception?) -> Unit
    ) {
        val roomRef = roomsCol.document(roomId)
        val msgRef = roomRef.collection("messages").document()

        val msgData = hashMapOf(
            "senderUid" to myUid,
            "type" to "text",
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()
        batch.set(msgRef, msgData)

        batch.update(
            roomRef, mapOf(
                "lastMessageText" to text,
                "lastMessageType" to "text",
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "lastSenderUid" to myUid
            )
        )

        batch.update(roomRef, "lastReadAt.$myUid", FieldValue.serverTimestamp())

        batch.commit()
            .addOnSuccessListener { onDone(true, null) }
            .addOnFailureListener { onDone(false, it) }
    }

    fun markRead(roomId: String, myUid: String) {
        roomsCol.document(roomId).update(
            mapOf(
                "lastReadAt.$myUid" to FieldValue.serverTimestamp(),
                "unreadCount.$myUid" to 0
            )
        )
    }

    fun getRoomOnce(roomId: String, onDone: (Room?) -> Unit) {
        roomsCol.document(roomId).get()
            .addOnSuccessListener { d ->
                if (!d.exists()) {
                    onDone(null); return@addOnSuccessListener
                }

                val membersRaw = d.get("members") as? List<*>
                val members = membersRaw?.mapNotNull { it as? String }.orEmpty()

                onDone(
                    Room(
                        roomId = d.id,
                        members = members,
                        lastMessageAt = d.getTimestamp("lastMessageAt"),
                        lastMessageText = d.getString("lastMessageText"),
                        lastMessageType = d.getString("lastMessageType"),
                        lastReadAt = parseLastReadAt(d.get("lastReadAt"))
                    )
                )
            }
            .addOnFailureListener { onDone(null) }
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
    fun observeTypingAt(roomId: String): Flow<Map<String, Timestamp>> = callbackFlow {
        val reg = roomsCol.document(roomId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyMap())
                    return@addSnapshotListener
                }
                val raw = snap?.get("typingAt") as? Map<*, *>
                val map = raw?.mapNotNull { (k, v) ->
                    val uid = k as? String ?: return@mapNotNull null
                    val ts = v as? Timestamp ?: return@mapNotNull null
                    uid to ts
                }?.toMap().orEmpty()
                trySend(map)
            }
        awaitClose { reg.remove() }
    }

    fun setTyping(roomId: String, myUid: String) {
        roomsCol.document(roomId).update("typingAt.$myUid", FieldValue.serverTimestamp())
    }

    fun clearTyping(roomId: String, myUid: String) {
        roomsCol.document(roomId).update("typingAt.$myUid", FieldValue.delete())
    }
}