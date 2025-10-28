package com.example.amicitia.presence

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class PresenceManager(
    private val uid: String,
    private val db: FirebaseFirestore
) {
    private var job: Job? = null

    // App / Activity 進到前景時呼叫
    fun start() {
        if (job != null) return // 已經在跑就不要重複開

        job = CoroutineScope(Dispatchers.IO).launch {
            // 一進來先宣告「我在線上」
            setOnline()

            // 然後固定更新 lastSeen
            while (isActive) {
                updateLastSeen()
                delay(10.seconds)
            }
        }
    }

    // App / Activity 離開前景、或使用者要登出時呼叫
    fun stop() {
        job?.cancel()
        job = null

        // 停掉 loop 之後再送一包 offline 狀態
        CoroutineScope(Dispatchers.IO).launch {
            setOffline()
        }
    }

    // -- private helpers --

    private suspend fun setOnline() {
        db.collection("onlineUsers")
            .document(uid)
            .set(
                mapOf(
                    "online" to true,
                    "lastSeen" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
    }

    private suspend fun updateLastSeen() {
        db.collection("onlineUsers")
            .document(uid)
            .set(
                mapOf(
                    "lastSeen" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
    }

    private suspend fun setOffline() {
        db.collection("onlineUsers")
            .document(uid)
            .set(
                mapOf(
                    "online" to false,
                    "lastSeen" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
    }
}