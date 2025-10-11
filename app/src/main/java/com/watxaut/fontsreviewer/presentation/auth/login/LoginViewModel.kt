package com.watxaut.fontsreviewer.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watxaut.fontsreviewer.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun onLoginClick() {
        val email = _uiState.value.email
        val password = _uiState.value.password

        // Validation
        var hasError = false
        if (email.isBlank()) {
            _uiState.update { it.copy(emailError = "Email is required") }
            hasError = true
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Password is required") }
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            loginUseCase(email, password)
                .onSuccess { user ->
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Login failed"
                        )
                    }
                }
        }
    }

    fun onRegisterClick() {
        _uiState.update { it.copy(navigateToRegister = true) }
    }

    fun onNavigatedToRegister() {
        _uiState.update { it.copy(navigateToRegister = false) }
    }

    fun onLoginSuccess() {
        _uiState.update { it.copy(loginSuccess = false) }
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false,
    val navigateToRegister: Boolean = false
)
