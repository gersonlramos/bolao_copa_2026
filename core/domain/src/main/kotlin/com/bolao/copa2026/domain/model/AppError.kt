package com.bolao.copa2026.domain.model

import java.time.Instant

sealed class AppError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    // Auth
    object EmailAlreadyRegistered : AppError()
    object InvalidCredentials : AppError()
    data class AccountBlocked(val blockedUntil: Instant) : AppError()

    // Groups
    object InvalidInviteCode : AppError()
    object ExpiredInviteCode : AppError()
    object AlreadyMember : AppError()
    object GroupFull : AppError()

    // Bets
    object BetDeadlinePassed : AppError()
    object InvalidBetValue : AppError()

    // Network / System
    object NetworkUnavailable : AppError()
    object ServiceUnavailable : AppError()
    data class Unknown(val error: Throwable) : AppError(cause = error)
}
