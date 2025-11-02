package com.example.amicitia

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.example.amicitia.nav.AppNavHost
import com.example.amicitia.nav.Routes
import com.example.amicitia.presence.PresenceManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ktx.firestore

class MainActivity : ComponentActivity() {

    private val auth by lazy { Firebase.auth }
    private val db by lazy { com.google.firebase.ktx.Firebase.firestore }

    private var presenceManager: PresenceManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // app 啟動要去哪個畫面
        val startDestination = if (auth.currentUser != null) {
            Routes.MENU
        } else {
            Routes.LOGIN
        }

        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val uid = auth.currentUser?.uid
        Log.d("Presence", "onStart uid = $uid")

        if (uid != null) {
            // 如果現在有登入 → 確保 presenceManager 啟動心跳
            if (presenceManager == null) {
                presenceManager = PresenceManager(uid, db)
            }
            presenceManager?.start()
        } else {
            // 如果沒登入(登入畫面)，不應該還有舊的 presenceManager
            presenceManager?.stop()
            presenceManager = null
        }
    }

    override fun onStop() {
        super.onStop()
        presenceManager?.stop()
    }
}