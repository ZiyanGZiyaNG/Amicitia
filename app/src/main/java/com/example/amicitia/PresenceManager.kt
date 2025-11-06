package com.example.amicitia.presence

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kotlin.time.Duration.Companion.seconds

class PresenceManager(
    private val uid: String,
    private val db: FirebaseFirestore
) {
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 開始心跳（每 10 秒更新 lastSeen），並立即標記 online=true */
    fun start() {
        if (job != null) return
        job = scope.launch {
            setOnline()
            while (isActive) {
                delay(10.seconds)
                updateLastSeen()
            }
        }
    }

    /** 停止心跳（不保證寫入完成；通常在切畫面或 app 背景時用） */
    fun stop() {
        job?.cancel()
        job = null
        // 不阻塞 UI；真正要保證離線請用 stopAndAwait()
        scope.launch { setOffline() }
    }

    /** 停止並「確保」寫入 offline；登出時請用這個 */
    suspend fun stopAndAwait() {
        job?.cancelAndJoin()
        job = null
        try {
            setOffline()
        } catch (_: Exception) { /* 靜默失敗即可，不卡住登出 */ }
    }

    private suspend fun setOnline() {
        db.collection("onlineUsers").document(uid).set(
            mapOf(
                "online" to true,
                "lastSeen" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    private suspend fun updateLastSeen() {
        db.collection("onlineUsers").document(uid).set(
            mapOf("lastSeen" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        ).await()
    }

    private suspend fun setOffline() {
        db.collection("onlineUsers").document(uid).set(
            mapOf(
                "online" to false,
                "lastSeen" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }
}