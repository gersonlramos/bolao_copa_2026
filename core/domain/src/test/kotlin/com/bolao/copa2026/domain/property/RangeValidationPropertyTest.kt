// Feature: bolao-copa-2026, Property 4: Validação de intervalo de gols e pontos
package com.bolao.copa2026.domain.property

import com.bolao.copa2026.domain.model.Bet
import com.bolao.copa2026.domain.model.ScoringSystem
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import java.time.Instant

class RangeValidationPropertyTest : FreeSpec({

    val now = Instant.now()

    fun makeBet(homeGoals: Int, awayGoals: Int) = Bet(
        id = "id",
        userId = "uid",
        matchId = "mid",
        homeGoals = homeGoals,
        awayGoals = awayGoals,
        score = null,
        scoringCategory = null,
        registeredAt = now,
        updatedAt = now
    )

    fun makeScoring(exactScore: Int, cwwg: Int = 0, cwlg: Int = 0, cd: Int = 0) =
        ScoringSystem(
            exactScore = exactScore,
            correctWinnerAndWinnerGoals = cwwg,
            correctWinnerAndLoserGoals = cwlg,
            correctDraw = cd
        )

    "Bet validation" - {
        "rejects homeGoals below 0" {
            checkAll(1000, Arb.int(Int.MIN_VALUE..-1)) { v ->
                shouldThrow<IllegalArgumentException> { makeBet(homeGoals = v, awayGoals = 0) }
            }
        }

        "rejects homeGoals above 99" {
            checkAll(1000, Arb.int(100..Int.MAX_VALUE)) { v ->
                shouldThrow<IllegalArgumentException> { makeBet(homeGoals = v, awayGoals = 0) }
            }
        }

        "rejects awayGoals below 0" {
            checkAll(1000, Arb.int(Int.MIN_VALUE..-1)) { v ->
                shouldThrow<IllegalArgumentException> { makeBet(homeGoals = 0, awayGoals = v) }
            }
        }

        "rejects awayGoals above 99" {
            checkAll(1000, Arb.int(100..Int.MAX_VALUE)) { v ->
                shouldThrow<IllegalArgumentException> { makeBet(homeGoals = 0, awayGoals = v) }
            }
        }

        "accepts valid goals in [0, 99]" {
            checkAll(1000, Arb.int(0..99), Arb.int(0..99)) { h, a ->
                makeBet(h, a) shouldNotBe null
            }
        }
    }

    "ScoringSystem validation" - {
        "rejects exactScore below 0" {
            checkAll(1000, Arb.int(Int.MIN_VALUE..-1)) { v ->
                shouldThrow<IllegalArgumentException> { makeScoring(exactScore = v) }
            }
        }

        "rejects exactScore above 999" {
            checkAll(1000, Arb.int(1000..Int.MAX_VALUE)) { v ->
                shouldThrow<IllegalArgumentException> { makeScoring(exactScore = v) }
            }
        }

        "rejects any field below 0" {
            checkAll(1000, Arb.int(Int.MIN_VALUE..-1)) { v ->
                shouldThrow<IllegalArgumentException> { makeScoring(0, cwwg = v) }
                shouldThrow<IllegalArgumentException> { makeScoring(0, cwlg = v) }
                shouldThrow<IllegalArgumentException> { makeScoring(0, cd = v) }
            }
        }

        "rejects any field above 999" {
            checkAll(1000, Arb.int(1000..Int.MAX_VALUE)) { v ->
                shouldThrow<IllegalArgumentException> { makeScoring(0, cwwg = v) }
                shouldThrow<IllegalArgumentException> { makeScoring(0, cwlg = v) }
                shouldThrow<IllegalArgumentException> { makeScoring(0, cd = v) }
            }
        }

        "accepts valid points in [0, 999]" {
            checkAll(1000, Arb.int(0..999), Arb.int(0..999), Arb.int(0..999), Arb.int(0..999)) { a, b, c, d ->
                makeScoring(a, b, c, d) shouldNotBe null
            }
        }
    }
})
