package com.bolao.copa2026.data.source

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreGroupDataSource @Inject constructor(
    private val db: FirebaseFirestore
) {
    suspend fun createGroup(
        groupId: String,
        name: String,
        betMode: String,
        inviteCode: String,
        adminUserId: String,
        adminDisplayName: String,
        scoringSystem: Map<String, Any>
    ): DocumentSnapshot {
        val data = mapOf(
            "name" to name,
            "betMode" to betMode,
            "inviteCode" to inviteCode,
            "adminUserId" to adminUserId,
            "memberIds" to listOf(adminUserId),
            "scoringSystem" to scoringSystem,
            "memberCount" to 1,
            "rankingStale" to false,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        db.collection("groups").document(groupId).set(data).await()
        db.collection("groups").document(groupId)
            .collection("members").document(adminUserId)
            .set(
                mapOf(
                    "displayName" to adminDisplayName,
                    "totalScore" to 0,
                    "joinedAt" to com.google.firebase.Timestamp.now()
                )
            )
            .await()
        return db.collection("groups").document(groupId).get().await()
    }

    suspend fun getGroupByInviteCode(inviteCode: String): DocumentSnapshot? =
        db.collection("groups")
            .whereEqualTo("inviteCode", inviteCode)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()

    suspend fun addMember(groupId: String, userId: String, displayName: String) {
        val groupRef = db.collection("groups").document(groupId)
        val memberRef = groupRef.collection("members").document(userId)
        db.runTransaction { tx ->
            val group = tx.get(groupRef)
            val count = (group.getLong("memberCount") ?: 0L) + 1
            tx.update(groupRef, "memberCount", count)
            tx.update(groupRef, "memberIds", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
            tx.set(
                memberRef,
                mapOf(
                    "displayName" to displayName,
                    "totalScore" to 0,
                    "joinedAt" to com.google.firebase.Timestamp.now()
                )
            )
        }.await()
    }

    fun observeUserGroups(userId: String): Flow<List<DocumentSnapshot>> = callbackFlow {
        val registration = db.collection("groups")
            .whereArrayContains("memberIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    fun observeGroup(groupId: String): Flow<DocumentSnapshot?> = callbackFlow {
        val registration = db.collection("groups").document(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot)
            }
        awaitClose { registration.remove() }
    }

    suspend fun updateScoringSystem(groupId: String, scoringSystem: Map<String, Any>) {
        db.collection("groups").document(groupId)
            .update("scoringSystem", scoringSystem)
            .await()
    }
}
