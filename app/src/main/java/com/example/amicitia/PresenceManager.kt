package com.example.amicitia.presence

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.tasks.await

class PresenceManager(
    private val uid: String,
    private val db: FirebaseFirestore
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = CoroutineScope(Dispatchers.IO).launch {
            setOnline()
            while (isActive) {
                updateLastSeen()
                delay(10.seconds)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        CoroutineScope(Dispatchers.IO).launch { setOffline() }
    }

    private suspend fun setOnline() {
        try {
            db.collection("onlineUsers").document(uid).set(
                mapOf(
                    "online" to true,
                    "lastSeen" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            // 不讓 presence 影響整個 app；必要時加 log
        }
    }

    private suspend fun updateLastSeen() {
        try {
            db.collection("onlineUsers").document(uid).set(
                mapOf("lastSeen" to FieldValue.serverTimestamp()),
                SetOptions.merge()
            ).await()
        } catch (_: Exception) { }
    }

    private suspend fun setOffline() {
        try {
            db.collection("onlineUsers").document(uid).set(
                mapOf("online" to false, "lastSeen" to FieldValue.serverTimestamp()),
                SetOptions.merge()
            ).await()
        } catch (_: Exception) { }
    }
}