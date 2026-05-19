package com.bolao.copa2026.domain.model

data class Group(
    val id: String,
    val name: String,
    val betMode: BetMode,
    val inviteCode: String,
    val adminUserId: String,
    val scoringSystem: ScoringSystem,
    val memberCount: Int,
    val rankingStale: Boolean = false
)

enum class BetMode { PRE_CUP, PRE_MATCH }
