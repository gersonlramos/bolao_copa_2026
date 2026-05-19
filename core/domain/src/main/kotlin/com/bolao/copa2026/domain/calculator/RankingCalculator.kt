package com.bolao.copa2026.domain.calculator

import com.bolao.copa2026.domain.model.RankingEntry
import com.bolao.copa2026.domain.model.ScoredBet

object RankingCalculator {

    /** Accumulates total score per user from a list of scored bets. */
    fun accumulateScores(scoredBets: List<ScoredBet>): Map<String, Int> =
        scoredBets.groupBy { it.bet.userId }
            .mapValues { (_, bets) -> bets.sumOf { it.points } }

    /**
     * Builds a ranked list from a map of userId → (displayName, totalScore).
     * Users with the same score share the same position.
     * Sorted by totalScore descending.
     */
    fun buildRanking(userScores: Map<String, Pair<String, Int>>): List<RankingEntry> {
        val sorted = userScores.entries
            .sortedByDescending { it.value.second }

        var position = 1
        return sorted.mapIndexed { index, entry ->
            if (index > 0 && entry.value.second < sorted[index - 1].value.second) {
                position = index + 1
            }
            RankingEntry(
                position = position,
                userId = entry.key,
                displayName = entry.value.first,
                totalScore = entry.value.second
            )
        }
    }

    /**
     * Full pipeline: given scored bets and a display-name lookup, returns the ranked list.
     * Requirements: 8.7, 8.9, 9.1, 9.2
     */
    fun calculateRanking(
        scoredBets: List<ScoredBet>,
        displayNames: Map<String, String>
    ): List<RankingEntry> {
        val scores = accumulateScores(scoredBets)
        val userScores = displayNames.mapValues { (uid, name) ->
            name to (scores[uid] ?: 0)
        }
        return buildRanking(userScores)
    }
}
