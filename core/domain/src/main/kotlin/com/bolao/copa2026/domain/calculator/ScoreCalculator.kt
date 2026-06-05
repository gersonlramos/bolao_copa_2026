package com.bolao.copa2026.domain.calculator

import com.bolao.copa2026.domain.model.*

/**
 * Pure scoring logic — no I/O, no side effects.
 *
 * Given a [Bet], a [MatchResult] and the group's [ScoringSystem], returns a
 * [ScoredBet] with the applicable [ScoringCategory] and the corresponding
 * point value.
 *
 * When a bet qualifies for more than one category simultaneously (e.g. both
 * CORRECT_WINNER_AND_WINNER_GOALS and CORRECT_WINNER_AND_LOSER_GOALS), only
 * the category with the highest configured point value is awarded (Req. 8.6).
 */
object ScoreCalculator {

    /**
     * Calculates the score for a single bet against an official match result.
     */
    fun calculate(bet: Bet, result: MatchResult, system: ScoringSystem): ScoredBet {
        val category = determineCategory(bet, result, system)
        val points = when (category) {
            ScoringCategory.EXACT_SCORE -> system.exactScore
            ScoringCategory.CORRECT_WINNER_AND_WINNER_GOALS -> system.correctWinnerAndWinnerGoals
            ScoringCategory.CORRECT_WINNER_AND_LOSER_GOALS -> system.correctWinnerAndLoserGoals
            ScoringCategory.CORRECT_DRAW -> system.correctDraw
            ScoringCategory.CORRECT_WINNER -> system.correctWinner
            ScoringCategory.NO_SCORE -> 0
        }
        return ScoredBet(bet, category, points)
    }

    /**
     * Determines the scoring category for a bet/result pair.
     *
     * Evaluation order (mutually exclusive after the first match):
     * 1. Exact score  → EXACT_SCORE
     * 2. Wrong winner → NO_SCORE
     * 3. Correct draw (non-exact) → CORRECT_DRAW
     * 4. Correct winner — pick the candidate with the highest point value;
     *    if no goal matches → NO_SCORE
     */
    fun determineCategory(bet: Bet, result: MatchResult, system: ScoringSystem): ScoringCategory {
        // 1. Exact score check (Req. 8.2)
        if (bet.homeGoals == result.homeGoals && bet.awayGoals == result.awayGoals)
            return ScoringCategory.EXACT_SCORE

        val betWinner = winner(bet.homeGoals, bet.awayGoals)
        val resultWinner = winner(result.homeGoals, result.awayGoals)

        // 2. Wrong winner (Req. 8.8)
        if (betWinner != resultWinner) return ScoringCategory.NO_SCORE

        // 3. Correct draw — not exact (already handled above) (Req. 8.5)
        if (betWinner == Winner.DRAW) return ScoringCategory.CORRECT_DRAW

        // 4. Correct winner — check which individual goal counts match (Req. 8.3, 8.4, 8.6)
        val candidates = mutableListOf<Pair<ScoringCategory, Int>>()

        if (betWinner == Winner.HOME && bet.homeGoals == result.homeGoals)
            candidates += ScoringCategory.CORRECT_WINNER_AND_WINNER_GOALS to system.correctWinnerAndWinnerGoals
        if (betWinner == Winner.AWAY && bet.awayGoals == result.awayGoals)
            candidates += ScoringCategory.CORRECT_WINNER_AND_WINNER_GOALS to system.correctWinnerAndWinnerGoals
        if (betWinner == Winner.HOME && bet.awayGoals == result.awayGoals)
            candidates += ScoringCategory.CORRECT_WINNER_AND_LOSER_GOALS to system.correctWinnerAndLoserGoals
        if (betWinner == Winner.AWAY && bet.homeGoals == result.homeGoals)
            candidates += ScoringCategory.CORRECT_WINNER_AND_LOSER_GOALS to system.correctWinnerAndLoserGoals

        // Award the highest-valued category; fallback to CORRECT_WINNER when no goal matched
        return candidates.maxByOrNull { it.second }?.first ?: ScoringCategory.CORRECT_WINNER
    }

    /**
     * Returns the winner of a match given the goal counts.
     */
    fun winner(home: Int, away: Int): Winner = when {
        home > away -> Winner.HOME
        away > home -> Winner.AWAY
        else -> Winner.DRAW
    }
}
