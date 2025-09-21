package com.example.amicitia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.example.amicitia.nav.AppNavHost
import com.example.amicitia.nav.Routes
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val currentUser = Firebase.auth.currentUser
            val startDestination =
                if (currentUser != null) Routes.HOME else Routes.LOGIN  // 判斷已登入就跳 HOME

            Surface(color = MaterialTheme.colorScheme.background) {
                AppNavHost(navController = navController, startDestination = startDestination)
            }
        }
    }
}