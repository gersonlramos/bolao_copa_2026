package com.bolao.copa2026.data.dto

import java.time.Instant

data class LoginAttemptsDto(
    val failCount: Int,
    val blockedUntil: Instant?,
    val lastAttemptAt: Instant
)
