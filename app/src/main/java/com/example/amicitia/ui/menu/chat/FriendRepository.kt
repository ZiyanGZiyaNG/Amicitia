package com.example.amicitia.ui.menu.chat

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

sealed class AddFriendResult {
    object SuccessNew : AddFriendResult()
    object SuccessAlreadyFriend : AddFriendResult()
    object ErrorSelf : AddFriendResult()
    object ErrorUserNotFound : AddFriendResult()
    object ErrorRequestNotAllowed : AddFriendResult()
    data class ErrorOther(val message: String?) : AddFriendResult()
}

object FriendRepository {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    private suspend fun addFriendBothSides(myUid: String, targetUid: String) {
        val data = mapOf(
            "since" to FieldValue.serverTimestamp()
        )

        db.collection("users")
            .document(myUid)
            .collection("friends")
            .document(targetUid)
            .set(data, SetOptions.merge())
            .await()

        db.collection("users")
            .document(targetUid)
            .collection("friends")
            .document(myUid)
            .set(data, SetOptions.merge())
            .await()
    }

    suspend fun addFriendByEmail(targetEmail: String): AddFriendResult {
        val me = auth.currentUser ?: return AddFriendResult.ErrorOther("尚未登入")
        val myUid = me.uid
        val email = targetEmail.trim().lowercase()

        return try {
            val snap = db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (snap.isEmpty) return AddFriendResult.ErrorUserNotFound

            val doc = snap.documents.first()
            val targetUid = doc.getString("uid") ?: doc.id

            if (targetUid == myUid) return AddFriendResult.ErrorSelf

            val allow = doc.getBoolean("allowFriendRequests") ?: true
            if (!allow) return AddFriendResult.ErrorRequestNotAllowed

            val myFriendDoc = db.collection("users")
                .document(myUid)
                .collection("friends")
                .document(targetUid)
                .get()
                .await()

            if (myFriendDoc.exists()) {
                AddFriendResult.SuccessAlreadyFriend
            } else {
                addFriendBothSides(myUid, targetUid)
                AddFriendResult.SuccessNew
            }
        } catch (e: Exception) {
            AddFriendResult.ErrorOther(e.message)
        }
    }
}