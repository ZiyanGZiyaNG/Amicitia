package com.example.amicitia.ui.menu.home.chat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class RunTempMessage(
    val id: String = "",
    val senderUid: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null
)

data class RunRoomState(
    val goalPlace: String = "",
    val goalStartHour: Int = 0,
    val goalStartMinute: Int = 0,
    val ready: Map<String, Boolean> = emptyMap(),
    val members: List<String> = emptyList()
)

class RunTempChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun roomDoc(sessionId: String) =
        db.collection("run_temp_rooms").document(sessionId)

    private fun msgCol(sessionId: String) =
        roomDoc(sessionId).collection("messages")

    fun observeMessages(sessionId: String): Flow<List<RunTempMessage>> = callbackFlow {
        val reg = msgCol(sessionId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents.orEmpty().map { d ->
                    RunTempMessage(
                        id = d.id,
                        senderUid = d.getString("senderUid").orEmpty(),
                        text = d.getString("text").orEmpty(),
                        createdAt = d.getTimestamp("createdAt")
                    )
                }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeRoom(sessionId: String): Flow<RunRoomState> = callbackFlow {
        val reg = roomDoc(sessionId).addSnapshotListener { snap, err ->
            if (err != null || snap == null || !snap.exists()) {
                trySend(RunRoomState())
                return@addSnapshotListener
            }

            val place = snap.getString("goalPlace").orEmpty()
            val h = (snap.getLong("goalStartHour") ?: 0L).toInt().coerceIn(0, 23)
            val m = (snap.getLong("goalStartMinute") ?: 0L).toInt().coerceIn(0, 59)

            val readyAny = snap.get("ready")
            val readyMap: Map<String, Boolean> = (readyAny as? Map<*, *>)?.mapNotNull { (k, v) ->
                val key = k as? String ?: return@mapNotNull null
                val b = v as? Boolean ?: return@mapNotNull null
                key to b
            }?.toMap().orEmpty()

            val members = (snap.get("members") as? List<*>)?.mapNotNull { it as? String }.orEmpty()

            trySend(
                RunRoomState(
                    goalPlace = place,
                    goalStartHour = h,
                    goalStartMinute = m,
                    ready = readyMap,
                    members = members
                )
            )
        }
        awaitClose { reg.remove() }
    }

    suspend fun ensureRoom(sessionId: String, members: List<String>) {
        val roomRef = roomDoc(sessionId)
        val snapshot = roomRef.get().await()
        if (snapshot.exists()) return

        val data = mapOf(
            "sessionId" to sessionId,
            "members" to members,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "goalPlace" to "",
            "goalStartHour" to 0,
            "goalStartMinute" to 0,
            "ready" to members.associateWith { false }
        )

        roomRef.set(data).await()
    }

    suspend fun sendMessage(sessionId: String, senderUid: String, text: String) {
        val data = hashMapOf(
            "senderUid" to senderUid,
            "text" to text,
            "createdAt" to Timestamp.now()
        )
        msgCol(sessionId).add(data).await()

        roomDoc(sessionId).set(
            mapOf(
                "lastMessageAt" to Timestamp.now(),
                "lastMessageText" to text,
                "lastMessageType" to "text",
                "lastSenderUid" to senderUid
            ),
            SetOptions.merge()
        ).await()
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

    suspend fun updateGoal(
        sessionId: String,
        place: String? = null,
        hour: Int? = null,
        minute: Int? = null
    ) {
        val updates = mutableMapOf<String, Any>()
        place?.let { updates["goalPlace"] = it }
        hour?.let { updates["goalStartHour"] = it.coerceIn(0, 23) }
        minute?.let { updates["goalStartMinute"] = it.coerceIn(0, 59) }

        if (updates.isEmpty()) return

        roomDoc(sessionId).set(
            mapOf("updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        ).await()

        roomDoc(sessionId).set(updates, SetOptions.merge()).await()
    }

    suspend fun setReady(sessionId: String, uid: String, ready: Boolean) {
        if (uid.isBlank()) return

        roomDoc(sessionId).set(
            mapOf("updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        ).await()

        roomDoc(sessionId).set(
            mapOf("ready.$uid" to ready),
            SetOptions.merge()
        ).await()
    }
}