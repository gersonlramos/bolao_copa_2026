package com.bolao.copa2026.data.source

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreBetDataSource @Inject constructor(
    private val db: FirebaseFirestore
) {
    suspend fun saveBet(betId: String, data: Map<String, Any>) {
        db.collection("bets").document(betId)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    fun observeBetsForMatch(groupId: String, matchId: String): Flow<List<DocumentSnapshot>> =
        callbackFlow {
            val registration = db.collection("bets")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("matchId", matchId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { close(error); return@addSnapshotListener }
                    trySend(snapshot?.documents ?: emptyList())
                }
            awaitClose { registration.remove() }
        }

    fun observeUserBets(groupId: String, userId: String): Flow<List<DocumentSnapshot>> =
        callbackFlow {
            val registration = db.collection("bets")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { close(error); return@addSnapshotListener }
                    trySend(snapshot?.documents ?: emptyList())
                }
            awaitClose { registration.remove() }
        }
}
