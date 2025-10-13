package com.example.amicitia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.example.amicitia.nav.AppNavHost
import com.example.amicitia.nav.Routes
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val firebaseAuth = FirebaseAuth.getInstance()
        val startDestination = if (firebaseAuth.currentUser != null) {
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
}