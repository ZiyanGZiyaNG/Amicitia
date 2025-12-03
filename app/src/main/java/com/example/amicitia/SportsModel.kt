package com.example.amicitia

// 對應 Firestore: users/{uid}/sports/{sportKey}
data class SportStats(
    val activityScore: Double = 0.0,
    val skillRating: Double = 1000.0,
    val totalScore: Double = 1000.0
)