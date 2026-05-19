package com.bolao.copa2026.data.source

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class FirestoreMatchDataSource @Inject constructor(
    private val db: FirebaseFirestore
) {
    fun observeMatches(): Flow<List<DocumentSnapshot>> = callbackFlow {
        val registration = db.collection("matches")
            .orderBy("scheduledAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    fun observeMatch(matchId: String): Flow<DocumentSnapshot?> = callbackFlow {
        val registration = db.collection("matches").document(matchId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot)
            }
        awaitClose { registration.remove() }
    }
}
