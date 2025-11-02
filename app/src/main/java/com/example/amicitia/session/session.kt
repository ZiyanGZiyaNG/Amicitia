package com.example.amicitia.session

import androidx.navigation.NavHostController
import com.example.amicitia.nav.Routes
import com.example.amicitia.presence.PresenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions

fun performLogout(
    navController: NavHostController,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    presenceManager: PresenceManager?
) {
    val uid = auth.currentUser?.uid

    // 1. 標記離線
    if (uid != null) {
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

    // 2. 停掉心跳
    presenceManager?.stop()

    // 3. Firebase Auth 登出
    auth.signOut()

    // 4. 回登入頁，清掉 MENU 堆疊
    navController.navigate(Routes.LOGIN) {
        popUpTo(Routes.MENU) { inclusive = true }
        launchSingleTop = true
    }
}