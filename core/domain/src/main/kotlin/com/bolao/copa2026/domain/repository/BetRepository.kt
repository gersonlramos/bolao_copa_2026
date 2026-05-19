package com.bolao.copa2026.domain.repository

import com.bolao.copa2026.domain.model.Bet
import com.bolao.copa2026.domain.model.BetWithUser
import kotlinx.coroutines.flow.Flow

interface BetRepository {
    suspend fun saveBet(groupId: String, matchId: String, homeGoals: Int, awayGoals: Int): Result<Bet>
    fun observeBetsForMatch(groupId: String, matchId: String): Flow<List<BetWithUser>>
    fun observeUserBets(groupId: String): Flow<List<Bet>>
}
