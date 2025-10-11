package com.watxaut.fontsreviewer.presentation.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watxaut.fontsreviewer.domain.model.LeaderboardEntry
import com.watxaut.fontsreviewer.domain.repository.AuthRepository
import com.watxaut.fontsreviewer.domain.usecase.GetLeaderboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val getLeaderboardUseCase: GetLeaderboardUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LeaderboardUiState>(LeaderboardUiState.Loading)
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            _uiState.value = LeaderboardUiState.Loading

            // Get current user to highlight them
            val currentUser = authRepository.getCurrentUser()
            val currentNickname = currentUser?.nickname

            getLeaderboardUseCase()
                .onSuccess { entries ->
                    _uiState.value = if (entries.isEmpty()) {
                        LeaderboardUiState.Empty
                    } else {
                        LeaderboardUiState.Success(
                            entries = entries,
                            currentUserNickname = currentNickname
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = LeaderboardUiState.Error(
                        error.message ?: "Failed to load leaderboard"
                    )
                }
        }
    }

    fun onRefresh() {
        loadLeaderboard()
    }
}

sealed class LeaderboardUiState {
    object Loading : LeaderboardUiState()
    object Empty : LeaderboardUiState()
    data class Success(
        val entries: List<LeaderboardEntry>,
        val currentUserNickname: String?
    ) : LeaderboardUiState()
    data class Error(val message: String) : LeaderboardUiState()
}
