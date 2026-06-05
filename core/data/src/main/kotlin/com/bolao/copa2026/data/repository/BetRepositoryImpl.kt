package com.bolao.copa2026.data.repository

import com.bolao.copa2026.data.mapper.toBet
import com.bolao.copa2026.data.source.FirestoreBetDataSource
import com.bolao.copa2026.domain.model.AppError
import com.bolao.copa2026.domain.model.Bet
import com.bolao.copa2026.domain.model.BetWithUser
import com.bolao.copa2026.domain.repository.BetRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

class BetRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val betDataSource: FirestoreBetDataSource
) : BetRepository {

    override suspend fun saveBet(
        groupId: String,
        matchId: String,
        homeGoals: Int,
        awayGoals: Int
    ): Result<Bet> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")

        // Verify bet deadline not passed
        val matchDoc = db.collection("matches").document(matchId).get().await()
        val status = matchDoc.getString("status") ?: "SCHEDULED"
        if (status == "FINISHED" || status == "IN_PROGRESS") throw AppError.BetDeadlinePassed

        val betId = "${uid}_${matchId}"
        val now = Instant.now()
        val data = mapOf<String, Any>(
            "userId" to uid,
            "groupId" to groupId,
            "matchId" to matchId,
            "homeGoals" to homeGoals,
            "awayGoals" to awayGoals,
            "registeredAt" to com.google.firebase.Timestamp(now.epochSecond, now.nano),
            "updatedAt" to com.google.firebase.Timestamp(now.epochSecond, now.nano)
        )
        betDataSource.saveBet(betId, data)

        Bet(
            id = betId,
            userId = uid,
            matchId = matchId,
            homeGoals = homeGoals,
            awayGoals = awayGoals,
            score = null,
            scoringCategory = null,
            registeredAt = now,
            updatedAt = now
        )
    }

    override fun observeBetsForMatch(groupId: String, matchId: String): Flow<List<BetWithUser>> =
        combine(
            betDataSource.observeBetsForMatch(groupId, matchId),
            groupMembersFlow(groupId)
        ) { betDocs, memberMap ->
            val betsByUser = betDocs.associate { doc ->
                val bet = doc.toBet()
                bet.userId to bet
            }
            memberMap.entries
                .map { (userId, displayName) ->
                    BetWithUser(
                        bet = betsByUser[userId],
                        userId = userId,
                        displayName = displayName
                    )
                }
                .sortedWith(
                    compareByDescending<BetWithUser> { it.bet?.score ?: -1 }
                        .thenBy { it.displayName }
                )
        }

    private fun groupMembersFlow(groupId: String): Flow<Map<String, String>> = callbackFlow {
        val reg = db.collection("groups").document(groupId)
            .collection("members")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val map = snap?.documents?.associate {
                    it.id to (it.getString("displayName") ?: "")
                } ?: emptyMap()
                trySend(map)
            }
        awaitClose { reg.remove() }
    }

    override fun observeUserBets(groupId: String): Flow<List<Bet>> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return betDataSource.observeUserBets(groupId, uid).map { docs -> docs.map { it.toBet(uid) } }
    }
}
