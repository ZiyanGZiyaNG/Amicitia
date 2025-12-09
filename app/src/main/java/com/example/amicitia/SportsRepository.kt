package com.example.amicitia

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SportsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun sportDoc(uid: String, sportKey: String) =
        db.collection("users")
            .document(uid)
            .collection("sports")
            .document(sportKey)

    suspend fun ensureAllSportsExist(uid: String) {
        val keys = listOf(
            "tennis",
            "run",
            "basketball",
            "football",
            "volleyball",
            "badminton"
        )

        for (key in keys) {
            val docRef = sportDoc(uid, key)
            val snap = docRef.get().await()

            if (!snap.exists() || snap.data.isNullOrEmpty()) {
                val default = SportStats()
                docRef.set(
                    mapOf(
                        "activityScore" to default.activityScore,
                        "skillRating" to default.skillRating,
                        "totalScore" to default.totalScore
                    )
                ).await()
            }
        }
    }

    suspend fun getSportStats(uid: String, sportKey: String): SportStats {
        return try {
            val snap = sportDoc(uid, sportKey).get().await()

            val isEmpty = !snap.exists() || snap.data.isNullOrEmpty()

            if (isEmpty) {
                val default = SportStats()
                sportDoc(uid, sportKey).set(
                    mapOf(
                        "activityScore" to default.activityScore,
                        "skillRating" to default.skillRating,
                        "totalScore" to default.totalScore
                    )
                ).await()
                default
            } else {
                val data = snap.data ?: emptyMap<String, Any>()

                fun getNumber(key: String, default: Double): Double {
                    val v = data[key]
                    return when (v) {
                        is Number -> v.toDouble()
                        else -> default
                    }
                }

                SportStats(
                    activityScore = getNumber("activityScore", 0.0),
                    skillRating = getNumber("skillRating", 1000.0),
                    totalScore = getNumber("totalScore", 1000.0)
                )
            }
        } catch (e: Exception) {
            Log.e("SportsRepository", "getSportStats failed", e)
            SportStats()
        }
    }

    suspend fun updateSportStats(uid: String, sportKey: String, stats: SportStats) {
        try {
            sportDoc(uid, sportKey).set(
                mapOf(
                    "activityScore" to stats.activityScore,
                    "skillRating" to stats.skillRating,
                    "totalScore" to stats.totalScore
                )
            ).await()
        } catch (e: Exception) {
            Log.e("SportsRepository", "updateSportStats failed", e)
        }
    }
}