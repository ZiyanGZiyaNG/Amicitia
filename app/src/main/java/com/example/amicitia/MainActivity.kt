package com.example.amicitia

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val uid = auth.currentUser?.uid
        Log.d("Presence", "Activity onStart uid = $uid")
    }
}