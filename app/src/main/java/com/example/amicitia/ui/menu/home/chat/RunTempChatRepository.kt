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
    val members: List<String> = emptyList(),
    val ready: Map<String, Boolean> = emptyMap()
)

class RunTempChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // 你現在聊天室路由是 run_temp_chat/$sessionId
    // 這邊 collection 跟你之前一致用 run_temp_rooms
    private fun roomDoc(sessionId: String) =
        db.collection("run_temp_rooms").document(sessionId)

    private fun messageCol(sessionId: String) =
        roomDoc(sessionId).collection("messages")

    // ------------------------
    // Messages
    // ------------------------

    fun observeMessages(sessionId: String): Flow<List<RunTempMessage>> = callbackFlow {
        val listener = messageCol(sessionId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snap.documents.map { d ->
                    RunTempMessage(
                        id = d.id,
                        senderUid = d.getString("senderUid") ?: "",
                        text = d.getString("text") ?: "",
                        createdAt = d.getTimestamp("createdAt")
                    )
                }
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(sessionId: String, senderUid: String, text: String) {
        messageCol(sessionId).add(
            mapOf(
                "senderUid" to senderUid,
                "text" to text,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()

        roomDoc(sessionId).set(
            mapOf("updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        ).await()
    }

    // ------------------------
    // Room
    // ------------------------

    fun observeRoom(sessionId: String): Flow<RunRoomState> = callbackFlow {
        val listener = roomDoc(sessionId)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null || !snap.exists()) {
                    trySend(RunRoomState())
                    return@addSnapshotListener
                }

                val state = RunRoomState(
                    goalPlace = snap.getString("goalPlace") ?: "",
                    goalStartHour = (snap.getLong("goalStartHour") ?: 0L).toInt(),
                    goalStartMinute = (snap.getLong("goalStartMinute") ?: 0L).toInt(),
                    members = (snap.get("members") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    ready = (snap.get("ready") as? Map<*, *>)?.mapNotNull { (k, v) ->
                        val key = k as? String ?: return@mapNotNull null
                        val value = v as? Boolean ?: return@mapNotNull null
                        key to value
                    }?.toMap() ?: emptyMap()
                )

                trySend(state)
            }

        awaitClose { listener.remove() }
    }

    suspend fun ensureRoom(sessionId: String, members: List<String>) {
        val roomRef = roomDoc(sessionId)
        val cleanMembers = members.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (cleanMembers.isEmpty()) return

        db.runTransaction { tx ->
            val snap = tx.get(roomRef)

            if (!snap.exists()) {
                val data = mapOf(
                    "sessionId" to sessionId,
                    "members" to cleanMembers,
                    "goalPlace" to "",
                    "goalStartHour" to 0,
                    "goalStartMinute" to 0,
                    "ready" to cleanMembers.associateWith { false },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                tx.set(roomRef, data)
                return@runTransaction null
            }

            // 房間已存在：合併 members（避免覆蓋）
            tx.set(
                roomRef,
                mapOf(
                    "members" to FieldValue.arrayUnion(*cleanMembers.toTypedArray()),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )

            // 補齊 ready 缺少的 uid（避免覆蓋對方）
            val readyAny = snap.get("ready")
            val readyMap: Map<String, Boolean> =
                (readyAny as? Map<*, *>)?.mapNotNull { (k, v) ->
                    val key = k as? String ?: return@mapNotNull null
                    val b = v as? Boolean ?: return@mapNotNull null
                    key to b
                }?.toMap().orEmpty()

            cleanMembers.forEach { uid ->
                if (!readyMap.containsKey(uid)) {
                    tx.set(roomRef, mapOf("ready.$uid" to false), SetOptions.merge())
                }
            }

            null
        }.await()
    }

    // ------------------------
    // Goal
    // ------------------------

    suspend fun updateGoal(sessionId: String, place: String? = null, hour: Int? = null, minute: Int? = null) {
        val updates = mutableMapOf<String, Any>()
        place?.let { updates["goalPlace"] = it }
        hour?.let { updates["goalStartHour"] = it }
        minute?.let { updates["goalStartMinute"] = it }
        if (updates.isEmpty()) return

        updates["updatedAt"] = FieldValue.serverTimestamp()
        roomDoc(sessionId).set(updates, SetOptions.merge()).await()
    }

    // ------------------------
    // Ready (唯一 finish 狀態)
    // ------------------------

    suspend fun setReady(sessionId: String, uid: String, ready: Boolean) {
        roomDoc(sessionId).update(
            mapOf(
                "ready.$uid" to ready,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // ------------------------
    // User profile (顯示暱稱/頭像)
    // ------------------------

    fun getUserProfileOnce(uid: String, onDone: (nickname: String, avatarUrl: String) -> Unit) {
        if (uid.isBlank()) {
            onDone("使用者", "")
            return
        }
        db.collection("users")
            .document(uid)
            .get()
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