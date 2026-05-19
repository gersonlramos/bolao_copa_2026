// Feature: bolao-copa-2026, Property 7: Bet visibility determined by match status
package com.bolao.copa2026.feature.bets

import com.bolao.copa2026.domain.model.*
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.time.Instant

private val now = Instant.now()

private fun makeBetWithUser(userId: String): BetWithUser {
    val bet = Bet(
        id = "$userId-bet", userId = userId, matchId = "m1",
        homeGoals = 1, awayGoals = 0,
        score = null, scoringCategory = null,
        registeredAt = now, updatedAt = now
    )
    return BetWithUser(bet = bet, userId = userId, displayName = "User $userId")
}

private fun makeNoBet(userId: String) = BetWithUser(bet = null, userId = userId, displayName = "User $userId")

/**
 * Domain rule: for non-FINISHED matches, only the current user's own bet should be visible.
 * For FINISHED matches, all bets (including nulls) should be visible.
 */
fun filterBetsByVisibility(
    bets: List<BetWithUser>,
    currentUserId: String,
    matchStatus: MatchStatus
): List<BetWithUser> = when (matchStatus) {
    MatchStatus.FINISHED -> bets
    else -> bets.filter { it.userId == currentUserId }
}

class BetVisibilityPropertyTest : FreeSpec({

    "Property 7 — non-FINISHED match only shows own bet" {
        checkAll(500, Arb.int(1..5)) { numOthers ->
            val currentUserId = "me"
            val myBet = makeBetWithUser(currentUserId)
            val otherBets = (1..numOthers).map { makeBetWithUser("user$it") }
            val allBets = listOf(myBet) + otherBets

            listOf(MatchStatus.SCHEDULED, MatchStatus.IN_PROGRESS).forEach { status ->
                val visible = filterBetsByVisibility(allBets, currentUserId, status)
                visible.all { it.userId == currentUserId } shouldBe true
                visible.size shouldBe 1
            }
        }
    }

    "Property 7 — FINISHED match shows all bets including no-bet entries" {
        checkAll(500, Arb.int(0..5)) { numOthers ->
            val currentUserId = "me"
            val myBet = makeBetWithUser(currentUserId)
            val otherBets = (1..numOthers).map { makeBetWithUser("user$it") }
            val noBets = listOf(makeNoBet("user_absent"))
            val allBets = listOf(myBet) + otherBets + noBets

            val visible = filterBetsByVisibility(allBets, currentUserId, MatchStatus.FINISHED)
            visible.size shouldBe allBets.size
        }
    }

    "Property 7 — absent user shows as no-bet entry in FINISHED match" {
        checkAll(500, Arb.int(1..4)) { numMembers ->
            val members = (1..numMembers).map { "user$it" }
            val bets = members.take(numMembers - 1).map { makeBetWithUser(it) }
            val noBets = listOf(makeNoBet(members.last()))
            val all = bets + noBets

            val visible = filterBetsByVisibility(all, "user1", MatchStatus.FINISHED)
            val nullBets = visible.filter { it.bet == null }
            nullBets.size shouldBe 1
        }
    }
})
