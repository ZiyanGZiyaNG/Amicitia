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

                    Room(
                        roomId = d.id,
                        members = members,
                        lastMessageAt = d.getTimestamp("lastMessageAt"),
                        lastMessageText = d.getString("lastMessageText"),
                        lastMessageType = d.getString("lastMessageType"),
                        lastReadAt = d.get("lastReadAt") as? Map<String, Timestamp>
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

        // 更新 room 摘要（讓 Chat 列表自動更新）
        batch.update(
            roomRef, mapOf(
                "lastMessageText" to text,
                "lastMessageType" to "text",
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "lastSenderUid" to myUid
            )
        )

        // 我方已讀時間（最小可用）
        batch.update(roomRef, "lastReadAt.$myUid", FieldValue.serverTimestamp())

        batch.commit()
            .addOnSuccessListener { onDone(true, null) }
            .addOnFailureListener { onDone(false, it) }
    }

    fun markRead(roomId: String, myUid: String) {
        // 你 DB 有 unreadCount 的話，也一起清；沒有也不會炸
        roomsCol.document(roomId).update(
            mapOf(
                "lastReadAt.$myUid" to FieldValue.serverTimestamp(),
                "unreadCount.$myUid" to 0
            )
        )
    }

    fun hasUnread(room: Room, myUid: String): Boolean {
        val last = room.lastMessageAt ?: return false
        val read = room.lastReadAt?.get(myUid) ?: return true
        return last.seconds > read.seconds ||
                (last.seconds == read.seconds && last.nanoseconds > read.nanoseconds)
    }

    // ---- 給 ChatRoom TopBar 用：一次性讀 room + 讀 user ----

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
                        lastReadAt = d.get("lastReadAt") as? Map<String, Timestamp>
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
}