package com.example.amicitia.presence

import android.util.Log
import com.google.firebase.database.*
import kotlinx.coroutines.*

class PresenceManager(
    private val uid: String,
    private val db: FirebaseDatabase
) {
    private val statusRef = db.getReference("status").child(uid)
    private val connectedRef = db.getReference(".info/connected")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null

    private var started = false
    private var connectedListener: ValueEventListener? = null

    fun start() {
        if (started) {
            Log.i("Presence", "start ignored (already started) uid=$uid")
            return
        }
        started = true
        Log.i("Presence", "start() uid=$uid")

        statusRef.updateChildren(
            mapOf(
                "state" to "online",
                "availability" to "idle",
                "last_changed" to ServerValue.TIMESTAMP
            )
        )

        connectedListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) == true
                Log.i("Presence", "connected=$connected uid=$uid")
                if (!connected) return

                statusRef.onDisconnect().updateChildren(
                    mapOf(
                        "state" to "offline",
                        "availability" to "idle",
                        "last_changed" to ServerValue.TIMESTAMP
                    )
                )

                statusRef.updateChildren(
                    mapOf(
                        "state" to "online",
                        "last_changed" to ServerValue.TIMESTAMP
                    )
                )

                startHeartbeat()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Presence", "connected listener cancelled: ${error.message}", error.toException())
            }
        }

        connectedRef.addValueEventListener(connectedListener!!)
    }

    fun stop() {
        if (!started) return
        started = false
        Log.i("Presence", "stop uid=$uid")

        heartbeatJob?.cancel()
        heartbeatJob = null

        connectedListener?.let { connectedRef.removeEventListener(it) }
        connectedListener = null

        statusRef.updateChildren(
            mapOf(
                "state" to "offline",
                "availability" to "idle",
                "last_changed" to ServerValue.TIMESTAMP
            )
        )
    }

    fun setAvailability(value: String) {
        statusRef.updateChildren(
            mapOf(
                "availability" to value,
                "last_changed" to ServerValue.TIMESTAMP
            )
        ).addOnFailureListener { e ->
            Log.e("Presence", "setAvailability failed", e)
        }
    }

    fun setIdle() = setAvailability("idle")
    fun setInMultiRun() = setAvailability("multi_run")
    fun setInSession() = setAvailability("in_session")

    private fun startHeartbeat() {
        if (heartbeatJob != null) return
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(30_000L)
                statusRef.child("last_changed").setValue(ServerValue.TIMESTAMP)
            }
        }
    }
}