package com.bolao.copa2026.domain.model

import java.time.Instant

data class Match(
    val id: String,
    val groupStage: String,
    val homeTeam: String,
    val homeTeamTla: String,
    val awayTeam: String,
    val awayTeamTla: String,
    val venue: String?,
    val scheduledAt: Instant,
    val status: MatchStatus,
    val scoreHome: Int?,
    val scoreAway: Int?
)

enum class MatchStatus { SCHEDULED, IN_PROGRESS, FINISHED }
