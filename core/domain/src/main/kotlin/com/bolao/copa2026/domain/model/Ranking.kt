package com.bolao.copa2026.domain.model

data class RankingEntry(
    val position: Int,
    val userId: String,
    val displayName: String,
    val totalScore: Int
)

data class BetWithUser(
    val bet: Bet?,           // null = no bet placed
    val userId: String,
    val displayName: String
)
