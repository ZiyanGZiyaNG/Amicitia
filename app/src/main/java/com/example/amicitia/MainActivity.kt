package com.example.amicitia

import android.os.Bundle
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

        // 一進來就決定進 app 要從哪個畫面開始
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
        android.util.Log.d("Presence", "onStart uid = $uid")
        if (uid == null) return

        if (presenceManager == null) {
            presenceManager = PresenceManager(uid, db)
        }

        presenceManager?.start()
    }

    override fun onStop() {
        super.onStop()

        val uid = auth.currentUser?.uid
        android.util.Log.d("Presence", "onStop uid = $uid")

        // 標記下線 & 停掉心跳更新
        presenceManager?.stop()
    }
}