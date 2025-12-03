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

    suspend fun getSportStats(uid: String, sportKey: String): SportStats {
        return try {
            val snap = sportDoc(uid, sportKey).get().await()

            if (!snap.exists()) {
                // 不存在就建立預設值
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
                // 比起 toObject()，我們自己一個欄位一個欄位讀，避免型別炸掉
                val activity = snap.getDouble("activityScore")
                    ?: snap.getLong("activityScore")?.toDouble()
                    ?: 0.0

                val skill = snap.getDouble("skillRating")
                    ?: snap.getLong("skillRating")?.toDouble()
                    ?: 1000.0

                val total = snap.getDouble("totalScore")
                    ?: snap.getLong("totalScore")?.toDouble()
                    ?: (skill + activity)

                SportStats(
                    activityScore = activity,
                    skillRating = skill,
                    totalScore = total
                )
            }
        } catch (e: Exception) {
            Log.e("SportsRepository", "getSportStats failed", e)
            SportStats()   // 爆掉就回傳預設，不要讓 app 掛掉
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