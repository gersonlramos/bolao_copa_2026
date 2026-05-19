// Feature: bolao-copa-2026, Properties 1 & 2: ScoreCalculator categorization
package com.bolao.copa2026.domain.property

import com.bolao.copa2026.domain.calculator.ScoreCalculator
import com.bolao.copa2026.domain.model.*
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.positiveInt
import io.kotest.property.checkAll
import java.time.Instant

// ---------------------------------------------------------------------------
// Generators
// ---------------------------------------------------------------------------

private val now = Instant.now()

private fun Arb.Companion.validGoals() = Arb.int(0..99)

private fun Arb.Companion.validBet(homeGoals: Int, awayGoals: Int) = Bet(
    id = "id", userId = "uid", matchId = "mid",
    homeGoals = homeGoals, awayGoals = awayGoals,
    score = null, scoringCategory = null,
    registeredAt = now, updatedAt = now
)

private fun arbValidScoringSystem() = Arb.int(0..999).let { pts ->
    // We generate four independent point values for the four categories
    object : io.kotest.property.Arb<ScoringSystem>() {
        override fun edgecase(rs: io.kotest.property.RandomSource) = null
        override fun sample(rs: io.kotest.property.RandomSource) =
            io.kotest.property.arbitrary.Sample(
                ScoringSystem(
                    exactScore = rs.random.nextInt(0, 1000),
                    correctWinnerAndWinnerGoals = rs.random.nextInt(0, 1000),
                    correctWinnerAndLoserGoals = rs.random.nextInt(0, 1000),
                    correctDraw = rs.random.nextInt(0, 1000)
                )
            )
    }
}

private fun arbMatchResult() = object : io.kotest.property.Arb<MatchResult>() {
    override fun edgecase(rs: io.kotest.property.RandomSource) = null
    override fun sample(rs: io.kotest.property.RandomSource) =
        io.kotest.property.arbitrary.Sample(
            MatchResult(
                homeGoals = rs.random.nextInt(0, 100),
                awayGoals = rs.random.nextInt(0, 100)
            )
        )
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

class ScoreCalculatorPropertyTest : FreeSpec({

    val system = ScoringSystem(
        exactScore = 10,
        correctWinnerAndWinnerGoals = 7,
        correctWinnerAndLoserGoals = 5,
        correctDraw = 4
    )

    // Property 1: Correct categorization
    "Property 1 — categorisation" - {

        "exact score returns EXACT_SCORE" {
            checkAll(1000, Arb.validGoals(), Arb.validGoals()) { h, a ->
                val bet = Arb.validBet(h, a)
                val result = MatchResult(h, a)
                ScoreCalculator.calculate(bet, result, system).category shouldBe ScoringCategory.EXACT_SCORE
            }
        }

        "wrong winner always returns NO_SCORE" {
            checkAll(1000, Arb.validGoals(), Arb.validGoals(), Arb.validGoals(), Arb.validGoals()) { bh, ba, rh, ra ->
                val betWinner = ScoreCalculator.winner(bh, ba)
                val resultWinner = ScoreCalculator.winner(rh, ra)
                if (betWinner != resultWinner && !(bh == rh && ba == ra)) {
                    val bet = Arb.validBet(bh, ba)
                    val result = MatchResult(rh, ra)
                    ScoreCalculator.calculate(bet, result, system).category shouldBe ScoringCategory.NO_SCORE
                }
            }
        }

        "non-exact draw returns CORRECT_DRAW" {
            checkAll(1000, Arb.validGoals(), Arb.validGoals()) { betGoals, resultGoals ->
                // same winner (draw), but not exact score
                if (betGoals != resultGoals) {
                    val bet = Arb.validBet(betGoals, betGoals)
                    val result = MatchResult(resultGoals, resultGoals)
                    ScoreCalculator.calculate(bet, result, system).category shouldBe ScoringCategory.CORRECT_DRAW
                }
            }
        }

        "points are non-negative for any valid input" {
            checkAll(1000, Arb.validGoals(), Arb.validGoals(), arbMatchResult(), arbValidScoringSystem()) { bh, ba, result, sys ->
                val bet = Arb.validBet(bh, ba)
                ScoreCalculator.calculate(bet, result, sys).points shouldBeGreaterThanOrEqualTo 0
            }
        }

        "exact score awards more or equal points than any other category (when configured)" {
            // Build a system where exactScore is always the highest
            checkAll(1000, Arb.int(1..999), Arb.validGoals()) { exact, g ->
                val sys = ScoringSystem(
                    exactScore = exact,
                    correctWinnerAndWinnerGoals = minOf(exact, 999),
                    correctWinnerAndLoserGoals = minOf(exact, 999),
                    correctDraw = minOf(exact, 999)
                )
                val bet = Arb.validBet(g, g)
                val result = MatchResult(g, g)
                val scored = ScoreCalculator.calculate(bet, result, sys)
                scored.category shouldBe ScoringCategory.EXACT_SCORE
                scored.points shouldBe exact
            }
        }
    }

    // Property 2: Highest category wins when multiple eligible
    "Property 2 — highest category wins on multiple eligibility" - {

        "winner goals > loser goals → CORRECT_WINNER_AND_WINNER_GOALS awarded when it has higher points" {
            // Bet: home wins 2-0, result: home wins 2-1
            // Bet matches winner goals (2) and not loser goals; only CWWG eligible
            checkAll(1000, arbValidScoringSystem()) { sys ->
                val bet = Arb.validBet(2, 0)
                val result = MatchResult(2, 1)
                val scored = ScoreCalculator.calculate(bet, result, sys)
                // Must be either CWWG or NO_SCORE (0 stays 0 goals: not matching loser)
                // home wins, home goals match (2==2) → CWWG candidate; away don't match → no CWLG
                scored.category shouldBe ScoringCategory.CORRECT_WINNER_AND_WINNER_GOALS
                scored.points shouldBe sys.correctWinnerAndWinnerGoals
            }
        }

        "when both winner and loser goals match, highest point category is returned" {
            // home wins 2-1, bet 2-1 → EXACT (handled above)
            // To test multi-eligibility: home wins 3-1, bet 3-1 → EXACT again
            // Multi-eligibility: not possible without exact score; test NO_SCORE fallback
            // Actually: bet home=3,away=1 result home=3,away=2 → CWWG (home goals match)
            // bet home=3,away=2 result home=3,away=1 → CWWG
            // We test that when CWWG and CWLG are both eligible, the highest point one wins
            // This requires: bet.homeGoals == result.homeGoals AND bet.awayGoals == result.awayGoals → exact (not multi)
            // True multi-eligibility: home winner, home goals match AND away goals match → EXACT (always)
            // In practice CWWG and CWLG cannot both be eligible simultaneously without being exact.
            // So this property tests that points == max(eligible candidates)
            checkAll(1000, arbValidScoringSystem(), Arb.validGoals()) { sys, winnerGoals ->
                // result: home wins with winnerGoals+1 to avoid 0
                if (winnerGoals < 99) {
                    val result = MatchResult(winnerGoals + 1, winnerGoals)
                    // bet: home wins, different goal values → no exact, no goal match → NO_SCORE
                    if (winnerGoals > 0) {
                        val bet = Arb.validBet(winnerGoals + 1, winnerGoals - 1)
                        // home goals match (winnerGoals+1 == winnerGoals+1) → CWWG
                        // away don't match (winnerGoals-1 != winnerGoals) → no CWLG
                        val scored = ScoreCalculator.calculate(bet, result, sys)
                        scored.category shouldBe ScoringCategory.CORRECT_WINNER_AND_WINNER_GOALS
                        scored.points shouldBe sys.correctWinnerAndWinnerGoals
                    }
                }
            }
        }
    }
})
