package com.watxaut.fontsreviewer.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watxaut.fontsreviewer.domain.model.User
import com.watxaut.fontsreviewer.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                _uiState.value = ProfileUiState.Success(user)
            } else {
                _uiState.value = ProfileUiState.NotAuthenticated
            }
        }
    }

    fun refreshProfile() {
        _uiState.value = ProfileUiState.Loading
        loadUserProfile()
    }

    fun onLogoutClick() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = ProfileUiState.NotAuthenticated
        }
    }
    
    fun onDeleteAccountClick() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.DeletingAccount
            
            authRepository.deleteAccount()
                .onSuccess {
                    _uiState.value = ProfileUiState.AccountDeleted
                }
                .onFailure { error ->
                    val currentState = _uiState.value
                    if (currentState is ProfileUiState.Success) {
                        _uiState.value = ProfileUiState.DeleteAccountError(
                            user = currentState.user,
                            errorMessage = error.message ?: "Failed to delete account"
                        )
                    } else {
                        _uiState.value = ProfileUiState.Error(
                            error.message ?: "Failed to delete account"
                        )
                    }
                }
        }
    }
    
    fun onDismissDeleteError() {
        loadUserProfile()
    }
}

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    object NotAuthenticated : ProfileUiState()
    data class Success(val user: User) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
    object DeletingAccount : ProfileUiState()
    object AccountDeleted : ProfileUiState()
    data class DeleteAccountError(val user: User, val errorMessage: String) : ProfileUiState()
}
