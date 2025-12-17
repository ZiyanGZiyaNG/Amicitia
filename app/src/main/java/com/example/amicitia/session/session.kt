package com.example.amicitia.session

import android.util.Log
import com.example.amicitia.presence.PresenceManager
import com.google.firebase.database.FirebaseDatabase

object SessionPresence {

    private const val RTDB_URL =
        "https://amicitia-481215-default-rtdb.asia-southeast1.firebasedatabase.app"

    val db: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance(RTDB_URL)
    }

    private var manager: PresenceManager? = null
    private var currentUid: String? = null

    fun start(uid: String) {
        if (currentUid == uid && manager != null) {
            Log.i("Presence", "SessionPresence.start ignored (same uid) uid=$uid")
            return
        }
        stop()

        currentUid = uid
        manager = PresenceManager(uid = uid, db = db).also { it.start() }
        Log.i("Presence", "SessionPresence.start ok uid=$uid")
    }

    fun stop() {
        manager?.stop()
        manager = null
        currentUid = null
        Log.i("Presence", "SessionPresence.stop")
    }

    fun setIdle() = manager?.setIdle()
    fun setInMultiRun() = manager?.setInMultiRun()
    fun setInSession() = manager?.setInSession()
}