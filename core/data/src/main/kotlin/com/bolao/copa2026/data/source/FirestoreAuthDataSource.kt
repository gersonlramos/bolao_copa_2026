package com.bolao.copa2026.data.source

import com.bolao.copa2026.data.dto.LoginAttemptsDto
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

class FirestoreAuthDataSource @Inject constructor(
    private val db: FirebaseFirestore
) {
    suspend fun getUserDocument(userId: String): DocumentSnapshot? =
        db.collection("users").document(userId).get().await()

    suspend fun getAppVersionDoc(): DocumentSnapshot? =
        runCatching { db.collection("config").document("appVersion").get().await() }.getOrNull()

    fun observeUserDocument(userId: String): Flow<DocumentSnapshot?> = callbackFlow {
        val reg = db.collection("users").document(userId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap)
            }
        awaitClose { reg.remove() }
    }

    suspend fun saveUserDocument(userId: String, displayName: String, email: String) {
        db.collection("users").document(userId).set(
            mapOf("displayName" to displayName, "email" to email)
        ).await()
    }

    suspend fun getLoginAttempts(emailHash: String): LoginAttemptsDto? {
        val doc = db.collection("loginAttempts").document(emailHash).get().await()
        if (!doc.exists()) return null
        return LoginAttemptsDto(
            failCount = (doc.getLong("failCount") ?: 0L).toInt(),
            blockedUntil = doc.getTimestamp("blockedUntil")?.toDate()?.toInstant(),
            lastAttemptAt = doc.getTimestamp("lastAttemptAt")?.toDate()?.toInstant()
                ?: Instant.EPOCH
        )
    }

    suspend fun incrementLoginAttempts(emailHash: String) {
        val ref = db.collection("loginAttempts").document(emailHash)
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val current = (snap.getLong("failCount") ?: 0L) + 1
            tx.set(
                ref,
                mapOf(
                    "failCount" to current,
                    "lastAttemptAt" to com.google.firebase.Timestamp.now()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
        }.await()
    }

    suspend fun blockLogin(emailHash: String, blockedUntil: Instant) {
        db.collection("loginAttempts").document(emailHash).update(
            mapOf(
                "blockedUntil" to com.google.firebase.Timestamp(
                    blockedUntil.epochSecond,
                    blockedUntil.nano
                )
            )
        ).await()
    }
}
