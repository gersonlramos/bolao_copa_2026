package com.bolao.copa2026.domain.usecase

import com.bolao.copa2026.domain.calculator.ScoreCalculator
import com.bolao.copa2026.domain.model.Bet
import com.bolao.copa2026.domain.model.MatchResult
import com.bolao.copa2026.domain.model.ScoredBet
import com.bolao.copa2026.domain.model.ScoringSystem
import javax.inject.Inject

class CalculateScoreUseCase @Inject constructor() {
    operator fun invoke(bet: Bet, result: MatchResult, system: ScoringSystem): ScoredBet =
        ScoreCalculator.calculate(bet, result, system)
}
