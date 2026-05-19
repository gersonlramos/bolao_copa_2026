// Feature: bolao-copa-2026, Properties 3, 5 & 6: ranking accumulation, idempotency, ordering
package com.bolao.copa2026.domain.property

import com.bolao.copa2026.domain.calculator.RankingCalculator
import com.bolao.copa2026.domain.calculator.ScoreCalculator
import com.bolao.copa2026.domain.model.*
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeSortedWith
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.time.Instant

private val now = Instant.now()

private fun makeBet(userId: String, homeGoals: Int, awayGoals: Int) = Bet(
    id = "$userId-bet", userId = userId, matchId = "match1",
    homeGoals = homeGoals, awayGoals = awayGoals,
    score = null, scoringCategory = null,
    registeredAt = now, updatedAt = now
)

private val defaultSystem = ScoringSystem(10, 7, 5, 4)

private fun scoredBet(userId: String, points: Int): ScoredBet {
    val bet = makeBet(userId, 1, 0)
    return ScoredBet(bet, ScoringCategory.EXACT_SCORE, points)
}

class RankingPropertyTest : FreeSpec({

    // Property 3: Total score is sum of individual scores
    "Property 3 — total score is sum of individual scored bets" {
        checkAll(500, Arb.int(0..5), Arb.int(0..100)) { numBets, basePoints ->
            val userId = "user1"
            val bets = (0 until numBets).map { scoredBet(userId, basePoints) }
            val scores = RankingCalculator.accumulateScores(bets)
            val expected = numBets * basePoints
            (scores[userId] ?: 0) shouldBe expected
        }
    }

    "Property 3 — accumulation across multiple users" {
        checkAll(500, Arb.list(Arb.int(0..200), 1..5)) { pointsList ->
            val userScores = pointsList.mapIndexed { i, pts ->
                scoredBet("user$i", pts)
            }
            val accumulated = RankingCalculator.accumulateScores(userScores)
            pointsList.forEachIndexed { i, pts ->
                (accumulated["user$i"] ?: 0) shouldBe pts
            }
        }
    }

    // Property 5: Recalculation is idempotent
    "Property 5 — score recalculation is idempotent" {
        checkAll(500, Arb.int(0..99), Arb.int(0..99), Arb.int(0..99), Arb.int(0..99)) { bh, ba, rh, ra ->
            val bet = makeBet("uid", bh, ba)
            val result = MatchResult(rh, ra)
            val scored1 = ScoreCalculator.calculate(bet, result, defaultSystem)
            val scored2 = ScoreCalculator.calculate(bet, result, defaultSystem)
            scored1.category shouldBe scored2.category
            scored1.points shouldBe scored2.points
        }
    }

    "Property 5 — ranking recalculation with same inputs is idempotent" {
        checkAll(500, Arb.int(0..50), Arb.int(0..50), Arb.int(0..50)) { s1, s2, s3 ->
            val bets = listOf(scoredBet("a", s1), scoredBet("b", s2), scoredBet("c", s3))
            val names = mapOf("a" to "Alice", "b" to "Bob", "c" to "Carol")
            val r1 = RankingCalculator.calculateRanking(bets, names)
            val r2 = RankingCalculator.calculateRanking(bets, names)
            r1.map { it.userId to it.totalScore } shouldBe r2.map { it.userId to it.totalScore }
        }
    }

    // Property 6: Ranking is sorted descending; ties share the same position
    "Property 6 — ranking is sorted descending by totalScore" {
        checkAll(500, Arb.list(Arb.int(0..1000), 1..8)) { scores ->
            val names = scores.mapIndexed { i, _ -> "u$i" to "User$i" }.toMap()
            val bets = scores.mapIndexed { i, pts -> scoredBet("u$i", pts) }
            val ranking = RankingCalculator.calculateRanking(bets, names)
            ranking shouldBeSortedWith compareByDescending { it.totalScore }
        }
    }

    "Property 6 — tied users share the same position" {
        checkAll(500, Arb.int(0..500)) { tiedScore ->
            val bets = listOf(scoredBet("x", tiedScore), scoredBet("y", tiedScore))
            val names = mapOf("x" to "X", "y" to "Y")
            val ranking = RankingCalculator.calculateRanking(bets, names)
            ranking[0].position shouldBe ranking[1].position
        }
    }

    "Property 6 — position after a tie group skips correctly" {
        // [100, 100, 50] → positions [1, 1, 3]
        checkAll(500, Arb.int(1..500)) { high ->
            val low = high - 1
            if (low >= 0) {
                val bets = listOf(scoredBet("a", high), scoredBet("b", high), scoredBet("c", low))
                val names = mapOf("a" to "A", "b" to "B", "c" to "C")
                val ranking = RankingCalculator.calculateRanking(bets, names)
                val sorted = ranking.sortedByDescending { it.totalScore }
                sorted[0].position shouldBe 1
                sorted[1].position shouldBe 1
                sorted[2].position shouldBe 3
            }
        }
    }

    "Property 6 — all positions are positive" {
        checkAll(500, Arb.list(Arb.int(0..200), 1..6)) { scores ->
            val names = scores.mapIndexed { i, _ -> "u$i" to "U$i" }.toMap()
            val bets = scores.mapIndexed { i, pts -> scoredBet("u$i", pts) }
            val ranking = RankingCalculator.calculateRanking(bets, names)
            ranking.forEach { it.position shouldBeGreaterThanOrEqualTo 1 }
        }
    }
})
