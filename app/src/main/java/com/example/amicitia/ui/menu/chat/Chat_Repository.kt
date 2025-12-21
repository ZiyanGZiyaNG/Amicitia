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
        val q = roomsCol
            .whereArrayContains("members", myUid)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)

        val reg = q.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err); return@addSnapshotListener
            }
            val rooms = snap?.documents.orEmpty().map { d ->
                Room(
                    roomId = d.id,
                    members = d.get("members") as? List<String> ?: emptyList(),
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

    fun observeMessages(roomId: String, limit: Long = 50): Flow<List<Message>> = callbackFlow {
        val q = roomsCol.document(roomId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)

        val reg = q.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err); return@addSnapshotListener
            }
            val msgs = snap?.documents.orEmpty().map { d ->
                Message(
                    id = d.id,
                    senderId = d.getString("senderId") ?: "",
                    type = d.getString("type") ?: "text",
                    text = d.getString("text") ?: "",
                    createdAt = d.getTimestamp("createdAt")
                )
            }
            trySend(msgs) // desc：新到舊
        }
        awaitClose { reg.remove() }
    }

    fun roomIdForTwoUids(uidA: String, uidB: String): String {
        val (a, b) = listOf(uidA, uidB).sorted()
        return "${a}_${b}"
    }

    fun ensureRoom(roomId: String, members: List<String>, onDone: (Boolean, Exception?) -> Unit) {
        val ref = roomsCol.document(roomId)
        ref.get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    onDone(true, null)
                } else {
                    val init = hashMapOf(
                        "members" to members.sorted(),
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "lastMessageText" to "",
                        "lastMessageType" to "text",
                        "lastReadAt" to members.associateWith { Timestamp(0, 0) }
                    )
                    ref.set(init)
                        .addOnSuccessListener { onDone(true, null) }
                        .addOnFailureListener { onDone(false, it) }
                }
            }
            .addOnFailureListener { onDone(false, it) }
    }

    fun sendTextMessage(
        roomId: String,
        myUid: String,
        text: String,
        onDone: (Boolean, Exception?) -> Unit
    ) {
        val roomRef = roomsCol.document(roomId)
        val msgRef = roomRef.collection("messages").document()

        val msgData = hashMapOf(
            "senderId" to myUid,
            "type" to "text",
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()
        batch.set(msgRef, msgData)
        batch.update(roomRef, mapOf(
            "lastMessageAt" to FieldValue.serverTimestamp(),
            "lastMessageText" to text,
            "lastMessageType" to "text"
        ))

        batch.commit()
            .addOnSuccessListener { onDone(true, null) }
            .addOnFailureListener { onDone(false, it) }
    }

    fun markRead(roomId: String, myUid: String) {
        roomsCol.document(roomId).update("lastReadAt.$myUid", FieldValue.serverTimestamp())
    }

    fun hasUnread(room: Room, myUid: String): Boolean {
        val last = room.lastMessageAt ?: return false
        val read = room.lastReadAt?.get(myUid) ?: return true
        return last.seconds > read.seconds || (last.seconds == read.seconds && last.nanoseconds > read.nanoseconds)
    }
}