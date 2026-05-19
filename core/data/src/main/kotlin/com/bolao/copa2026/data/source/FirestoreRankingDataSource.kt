package com.bolao.copa2026.data.source

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class FirestoreRankingDataSource @Inject constructor(
    private val db: FirebaseFirestore
) {
    fun observeRanking(groupId: String): Flow<List<DocumentSnapshot>> = callbackFlow {
        val registration = db.collection("groups").document(groupId)
            .collection("members")
            .orderBy("totalScore", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents ?: emptyList())
            }
        awaitClose { registration.remove() }
    }
}
