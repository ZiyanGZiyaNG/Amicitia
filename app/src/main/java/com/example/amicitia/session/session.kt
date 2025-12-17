package com.example.amicitia.session

import com.example.amicitia.presence.PresenceManager

object SessionPresence {
    private var manager: PresenceManager? = null
    private var currentUid: String? = null

    fun start(uid: String) {
        if (currentUid == uid && manager != null) return
        stop()
        currentUid = uid
        manager = PresenceManager(uid).also { it.start() }
    }

    fun stop() {
        manager?.stop()
        manager = null
        currentUid = null
    }
}