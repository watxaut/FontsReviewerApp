package com.watxaut.fontsreviewer.presentation.auth.register

import android.util.Log
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

        // Validation
        var hasError = false
        if (email.isBlank()) {
            _uiState.update { it.copy(emailError = "Email is required") }
            hasError = true
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(emailError = "Invalid email address") }
            hasError = true
        }

        if (nickname.isBlank()) {
            _uiState.update { it.copy(nicknameError = "Nickname is required") }
            hasError = true
        } else if (nickname.length < 3) {
            _uiState.update { it.copy(nicknameError = "Nickname must be at least 3 characters") }
            hasError = true
        } else if (nickname.length > 20) {
            _uiState.update { it.copy(nicknameError = "Nickname must be at most 20 characters") }
            hasError = true
        }

        if (password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Password is required") }
            hasError = true
        } else if (password.length < 6) {
            _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            hasError = true
        }

        if (confirmPassword.isBlank()) {
            _uiState.update { it.copy(confirmPasswordError = "Confirm password is required") }
            hasError = true
        } else if (password != confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Passwords do not match") }
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            Log.d(TAG, "=== REGISTER VIEW MODEL: START ===")
            Log.d(TAG, "Email: $email")
            Log.d(TAG, "Nickname: $nickname")
            
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            Log.d(TAG, "Calling registerUseCase...")
            registerUseCase(email, nickname, password)
                .onSuccess { user ->
                    Log.d(TAG, "=== REGISTER VIEW MODEL: SUCCESS ===")
                    Log.d(TAG, "User: $user")
                    _uiState.update { it.copy(isLoading = false, registerSuccess = true) }
                }
                .onFailure { error ->
                    Log.e(TAG, "=== REGISTER VIEW MODEL: FAILURE ===")
                    Log.e(TAG, "Error: ${error.message}", error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Registration failed"
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