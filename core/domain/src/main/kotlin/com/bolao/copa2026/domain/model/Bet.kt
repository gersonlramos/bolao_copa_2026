package com.bolao.copa2026.domain.model

import java.time.Instant

/**
 * Represents a user's score prediction for a match within a group.
 * Both [homeGoals] and [awayGoals] must be in the range [0, 99].
 */
data class Bet(
    val id: String,
    val userId: String,
    val matchId: String,
    val homeGoals: Int,
    val awayGoals: Int,
    val score: Int?,
    val scoringCategory: ScoringCategory?,
    val registeredAt: Instant,
    val updatedAt: Instant
) {
    init {
        require(homeGoals in 0..99) {
            "homeGoals must be in 0..99, was $homeGoals"
        }
        require(awayGoals in 0..99) {
            "awayGoals must be in 0..99, was $awayGoals"
        }
    }
}

enum class ScoringCategory {
    EXACT_SCORE,
    CORRECT_WINNER_AND_WINNER_GOALS,
    CORRECT_WINNER_AND_LOSER_GOALS,
    CORRECT_DRAW,
    CORRECT_WINNER,
    NO_SCORE
}
