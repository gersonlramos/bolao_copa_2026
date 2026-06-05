package com.bolao.copa2026.domain.model

import java.time.Instant

sealed class AppError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    // Auth
    object EmailAlreadyRegistered : AppError("EmailAlreadyRegistered")
    object InvalidCredentials : AppError("InvalidCredentials")
    data class AccountBlocked(val blockedUntil: Instant) : AppError("AccountBlocked")

    // Groups
    object InvalidInviteCode : AppError("InvalidInviteCode")
    object ExpiredInviteCode : AppError("ExpiredInviteCode")
    object AlreadyMember : AppError("AlreadyMember")
    object GroupFull : AppError("GroupFull")
    object GroupLimitReached : AppError("GroupLimitReached")

    // Bets
    object BetDeadlinePassed : AppError("BetDeadlinePassed")
    object InvalidBetValue : AppError("InvalidBetValue")

    // Network / System
    object NetworkUnavailable : AppError("NetworkUnavailable")
    object ServiceUnavailable : AppError("ServiceUnavailable")
    data class Unknown(val error: Throwable) : AppError("Unknown", error)
}
