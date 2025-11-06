package com.example.amicitia.session

import com.example.amicitia.presence.PresenceManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object Session {
    private var presence: PresenceManager? = null

    fun startPresence(uid: String) {
        if (presence == null) {
            presence = PresenceManager(uid, Firebase.firestore)
        }
        presence?.start()
    }

    suspend fun stopPresenceAndAwait() {
        presence?.stopAndAwait()
        presence = null
    }

    fun stopPresence() {
        presence?.stop()
        presence = null
    }
}