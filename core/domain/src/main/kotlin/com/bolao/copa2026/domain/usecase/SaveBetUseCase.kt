package com.bolao.copa2026.domain.usecase

import com.bolao.copa2026.domain.model.Bet
import com.bolao.copa2026.domain.repository.BetRepository
import javax.inject.Inject

class SaveBetUseCase @Inject constructor(
    private val betRepository: BetRepository
) {
    suspend operator fun invoke(
        groupId: String,
        matchId: String,
        homeGoals: Int,
        awayGoals: Int
    ): Result<Bet> = betRepository.saveBet(groupId, matchId, homeGoals, awayGoals)
}
