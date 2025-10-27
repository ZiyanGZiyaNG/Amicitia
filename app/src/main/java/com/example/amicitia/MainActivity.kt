package com.example.amicitia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.amicitia.nav.AppNavHost
import com.example.amicitia.nav.Routes
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // 拿 Firebase 服務
    private val auth by lazy { Firebase.auth }
    private val db by lazy { com.google.firebase.ktx.Firebase.firestore }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 決定啟動要進哪一頁 (LOGIN or MENU)
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

    // App 在前景 → 玩家視為「上線」
    override fun onStart() {
        super.onStart()

        val uid = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
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
    }

    // App 被切走 / 跳到背景 → 玩家視為「下線」
    override fun onStop() {
        super.onStop()

        val uid = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
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
}