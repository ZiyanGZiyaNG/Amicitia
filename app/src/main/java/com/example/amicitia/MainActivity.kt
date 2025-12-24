package com.example.amicitia

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.example.amicitia.nav.AppNavHost
import com.example.amicitia.session.SessionPresence
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { Firebase.auth }

    // 用來接住通知/深連結帶進來的 Uri（包含 onNewIntent 的情況）
    private var pendingDeepLink: Uri? by mutableStateOf(null)

    private val requestPostNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d("NotifyPerm", "POST_NOTIFICATIONS granted=$granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 第一次啟動若是從通知/深連結進來，先存起來，交給 Compose 導頁
        pendingDeepLink = intent?.data

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                val uid = auth.currentUser?.uid
                Log.d("Presence", "Process onStart uid=$uid")
                if (uid != null) SessionPresence.start(uid)
            }

            override fun onStop(owner: LifecycleOwner) {
                Log.d("Presence", "Process onStop -> stop presence")
                SessionPresence.stop()
            }
        })

        // Android 13+：主動要通知權限（Service 不能要，只能 Activity 要）
        ensureNotificationPermissionIfNeeded()

        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                val navController = rememberNavController()

                // 每次 pendingDeepLink 更新（包含 onNewIntent），就導頁一次
                val uri = pendingDeepLink
                LaunchedEffect(uri) {
                    if (uri == null) return@LaunchedEffect

                    // 期望格式：amicitia://chat/room/{roomId}
                    // pathSegments: ["room", "{roomId}"]
                    val segments = uri.pathSegments
                    val roomId = if (segments.size >= 2 && segments[0] == "room") {
                        segments[1]
                    } else {
                        null
                    }

                    if (!roomId.isNullOrBlank()) {
                        Log.d("DeepLink", "Navigate to room/$roomId from uri=$uri")

                        // 這裡假設你聊天室 route 是： "room/{roomId}"
                        navController.navigate("room/$roomId") {
                            // 避免同一個通知點多次造成堆疊爆炸
                            launchSingleTop = true
                        }
                    } else {
                        Log.d("DeepLink", "No roomId parsed from uri=$uri")
                    }

                    // 導完就清掉，避免重組重導
                    pendingDeepLink = null
                }

                AppNavHost(navController = navController)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val uid = auth.currentUser?.uid
        Log.d("Presence", "Activity onStart uid = $uid")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // App 已在執行時，點通知會走這裡
        pendingDeepLink = intent.data
    }

    private fun ensureNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            requestPostNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}