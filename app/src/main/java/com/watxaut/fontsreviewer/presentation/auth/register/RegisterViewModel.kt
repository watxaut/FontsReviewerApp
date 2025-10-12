package com.watxaut.fontsreviewer.presentation.auth.register

import com.watxaut.fontsreviewer.util.SecureLog
import com.watxaut.fontsreviewer.util.InputValidator
import com.watxaut.fontsreviewer.util.ValidationResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watxaut.fontsreviewer.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "RegisterViewModel"
    }

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun onNicknameChange(nickname: String) {
        _uiState.update { it.copy(nickname = nickname, nicknameError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null) }
    }

    fun onRegisterClick() {
        val email = _uiState.value.email
        val nickname = _uiState.value.nickname
        val password = _uiState.value.password
        val confirmPassword = _uiState.value.confirmPassword

        // SECURITY: Validate all inputs before processing
        var hasError = false
        
        // Validate email
        when (val result = InputValidator.validateEmail(email)) {
            is ValidationResult.Error -> {
                _uiState.update { it.copy(emailError = result.message) }
                hasError = true
            }
            ValidationResult.Valid -> { /* Valid */ }
        }

        // Validate nickname
        when (val result = InputValidator.validateNickname(nickname)) {
            is ValidationResult.Error -> {
                _uiState.update { it.copy(nicknameError = result.message) }
                hasError = true
            }
            ValidationResult.Valid -> { /* Valid */ }
        }

        // Validate password (using simple validation for backward compatibility)
        if (password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Password is required") }
            hasError = true
        } else if (password.length < 6) {
            _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            hasError = true
        }

        // Validate password confirmation
        if (confirmPassword.isBlank()) {
            _uiState.update { it.copy(confirmPasswordError = "Confirm password is required") }
            hasError = true
        } else if (password != confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Passwords do not match") }
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            SecureLog.d(TAG, "=== REGISTER VIEW MODEL: START ===")
            // SECURITY: Never log user email in production
            SecureLog.d(TAG, "Nickname: $nickname")
            
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            SecureLog.d(TAG, "Calling registerUseCase...")
            registerUseCase(email, nickname, password)
                .onSuccess { user ->
                    SecureLog.d(TAG, "=== REGISTER VIEW MODEL: SUCCESS ===")
                    SecureLog.d(TAG, "User: $user")
                    _uiState.update { it.copy(isLoading = false, registerSuccess = true) }
                }
                .onFailure { error ->
                    SecureLog.e(TAG, "=== REGISTER VIEW MODEL: FAILURE ===")
                    SecureLog.e(TAG, "Error: ${error.message}", error)
                    
                    // Parse error message to show user-friendly text
                    val userFriendlyMessage = when {
                        error.message?.contains("already registered", ignoreCase = true) == true -> "Email already exists"
                        error.message?.contains("email exists", ignoreCase = true) == true -> "Email already exists"
                        error.message?.contains("user already exists", ignoreCase = true) == true -> "Email already exists"
                        error.message?.contains("nickname", ignoreCase = true) == true -> "Nickname is already taken"
                        error.message?.contains("invalid email", ignoreCase = true) == true -> "Invalid email format"
                        error.message?.contains("weak password", ignoreCase = true) == true -> "Password is too weak"
                        error.message?.contains("network", ignoreCase = true) == true -> "Network error. Please check your connection"
                        error.message?.contains("timeout", ignoreCase = true) == true -> "Connection timeout. Please try again"
                        else -> "Registration failed. Please try again"
                    }
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = userFriendlyMessage
                        )
                    }
                }
        }
    }

    fun onBackClick() {
        _uiState.update { it.copy(navigateBack = true) }
    }

    fun onNavigatedBack() {
        _uiState.update { it.copy(navigateBack = false) }
    }

    fun onRegisterSuccess() {
        _uiState.update { it.copy(registerSuccess = false) }
    }
}

data class RegisterUiState(
    val email: String = "",
    val nickname: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val nicknameError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val errorMessage: String? = null,
    val registerSuccess: Boolean = false,
    val navigateBack: Boolean = false
)