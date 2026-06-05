package com.bolao.copa2026.domain.model

data class User(
    val id: String,
    val displayName: String,
    val email: String,
    val isVip: Boolean = false
)
