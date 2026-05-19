package com.bolao.copa2026.common.validation

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()

    val isValid: Boolean get() = this is Valid
}

fun validateDisplayName(name: String): ValidationResult {
    if (name.length < 3) return ValidationResult.Invalid("Nome deve ter pelo menos 3 caracteres")
    if (name.length > 50) return ValidationResult.Invalid("Nome deve ter no máximo 50 caracteres")
    return ValidationResult.Valid
}

private val emailRegex = Regex(
    "^[a-zA-Z0-9!#\$%&'*+/=?^_`{|}~.-]+" +
        "@[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?" +
        "(\\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)+\$"
)

fun validateEmail(email: String): ValidationResult {
    if (email.length > 254) return ValidationResult.Invalid("E-mail inválido")
    if (!emailRegex.matches(email)) return ValidationResult.Invalid("E-mail inválido")
    return ValidationResult.Valid
}

fun validatePassword(password: String): ValidationResult {
    if (password.length < 8) return ValidationResult.Invalid("Senha deve ter pelo menos 8 caracteres")
    if (password.length > 128) return ValidationResult.Invalid("Senha deve ter no máximo 128 caracteres")
    return ValidationResult.Valid
}
