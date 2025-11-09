package com.example.amicitia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.example.amicitia.nav.AppNavHost
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import android.util.Log

class MainActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { Firebase.auth }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        Log.d("Presence", "onStart uid = $uid")
    }
}