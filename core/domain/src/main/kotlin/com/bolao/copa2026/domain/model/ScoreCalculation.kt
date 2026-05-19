package com.bolao.copa2026.domain.model

data class MatchResult(val homeGoals: Int, val awayGoals: Int)

data class ScoredBet(val bet: Bet, val category: ScoringCategory, val points: Int)

enum class Winner { HOME, AWAY, DRAW }
