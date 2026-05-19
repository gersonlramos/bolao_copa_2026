package com.bolao.copa2026.data.mapper

import com.bolao.copa2026.domain.model.*
import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant

fun DocumentSnapshot.toUser(): User = User(
    id = id,
    displayName = getString("displayName") ?: "",
    email = getString("email") ?: ""
)

fun DocumentSnapshot.toMatch(): Match = Match(
    id = id,
    groupStage = getString("group") ?: getString("groupStage") ?: "",
    homeTeam = getString("homeTeam") ?: "",
    homeTeamTla = getString("homeTeamTla") ?: "",
    awayTeam = getString("awayTeam") ?: "",
    awayTeamTla = getString("awayTeamTla") ?: "",
    venue = getString("venue"),
    scheduledAt = getTimestamp("scheduledAt")?.toDate()?.toInstant() ?: Instant.EPOCH,
    status = MatchStatus.valueOf(getString("status") ?: "SCHEDULED"),
    scoreHome = getLong("scoreHome")?.toInt(),
    scoreAway = getLong("scoreAway")?.toInt()
)

fun DocumentSnapshot.toGroup(): Group {
    @Suppress("UNCHECKED_CAST")
    val ss = get("scoringSystem") as? Map<String, Any> ?: emptyMap()
    return Group(
        id = id,
        name = getString("name") ?: "",
        betMode = BetMode.valueOf(getString("betMode") ?: "PRE_CUP"),
        inviteCode = getString("inviteCode") ?: "",
        adminUserId = getString("adminUserId") ?: "",
        scoringSystem = ScoringSystem(
            exactScore = (ss["exactScore"] as? Long)?.toInt() ?: 0,
            correctWinnerAndWinnerGoals = (ss["correctWinnerAndWinnerGoals"] as? Long)?.toInt() ?: 0,
            correctWinnerAndLoserGoals = (ss["correctWinnerAndLoserGoals"] as? Long)?.toInt() ?: 0,
            correctDraw = (ss["correctDraw"] as? Long)?.toInt() ?: 0
        ),
        memberCount = getLong("memberCount")?.toInt() ?: 0,
        rankingStale = getBoolean("rankingStale") ?: false
    )
}

fun DocumentSnapshot.toBet(userId: String? = null): Bet = Bet(
    id = id,
    userId = userId ?: getString("userId") ?: "",
    matchId = getString("matchId") ?: "",
    homeGoals = getLong("homeGoals")?.toInt() ?: 0,
    awayGoals = getLong("awayGoals")?.toInt() ?: 0,
    score = getLong("score")?.toInt(),
    scoringCategory = getString("scoringCategory")?.let { ScoringCategory.valueOf(it) },
    registeredAt = getTimestamp("registeredAt")?.toDate()?.toInstant() ?: Instant.EPOCH,
    updatedAt = getTimestamp("updatedAt")?.toDate()?.toInstant() ?: Instant.EPOCH
)

fun DocumentSnapshot.toRankingEntry(position: Int): RankingEntry = RankingEntry(
    position = position,
    userId = id,
    displayName = getString("displayName") ?: "",
    totalScore = getLong("totalScore")?.toInt() ?: 0
)

fun ScoringSystem.toMap(): Map<String, Any> = mapOf(
    "exactScore" to exactScore,
    "correctWinnerAndWinnerGoals" to correctWinnerAndWinnerGoals,
    "correctWinnerAndLoserGoals" to correctWinnerAndLoserGoals,
    "correctDraw" to correctDraw
)
