package com.bolao.copa2026.domain.model

import java.time.Instant

sealed class LoginBlockStatus {
    object NotBlocked : LoginBlockStatus()
    data class Blocked(val blockedUntil: Instant) : LoginBlockStatus()
}
