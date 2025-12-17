package com.example.amicitia.session

import com.example.amicitia.presence.PresenceManager

object SessionPresence {
    private var currentUid: String? = null
    private var manager: PresenceManager? = null

    fun start(uid: String) {
        if (currentUid == uid && manager != null) return
        stop() // 切帳號才會停掉舊的
        currentUid = uid
        manager = PresenceManager(uid).also { it.start() }
    }

    fun stop() {
        manager?.stop()
        manager = null
        currentUid = null
    }
}