package com.watxaut.fontsreviewer.util

import android.util.Patterns

/**
 * Input validation utility for user input
 * 
 * NOTE: SQL injection is already prevented by Supabase's parameterized queries.
 * This utility focuses on user-facing validation only.
 */
object InputValidator {
    
    // ============================================================================
    // Email Validation
    // ============================================================================
    
    fun validateEmail(email: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult.Error("Email is required")
            email.length > 255 -> ValidationResult.Error("Email is too long")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> ValidationResult.Error("Invalid email format")
            else -> ValidationResult.Valid
        }
    }
    
    // ============================================================================
    // Nickname Validation
    // ============================================================================
    
    /**
     * Validates nickname:
     * - 3-20 characters
     * - Alphanumeric and underscore only
     * - No spaces or special characters
     */
    fun validateNickname(nickname: String): ValidationResult {
        return when {
            nickname.isBlank() -> ValidationResult.Error("Nickname is required")
            nickname.length < 3 -> ValidationResult.Error("Nickname must be at least 3 characters")
            nickname.length > 20 -> ValidationResult.Error("Nickname must be at most 20 characters")
            !nickname.matches(Regex("^[a-zA-Z0-9_]+$")) -> ValidationResult.Error("Nickname can only contain letters, numbers, and underscores")
            else -> ValidationResult.Valid
        }
    }
}

/**
 * Validation result sealed class
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Error(val message: String) : ValidationResult()
    
    val isValid: Boolean
        get() = this is Valid
    
    val errorMessage: String?
        get() = (this as? Error)?.message
}
