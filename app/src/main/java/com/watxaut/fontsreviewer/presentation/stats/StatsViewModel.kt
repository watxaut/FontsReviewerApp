package com.watxaut.fontsreviewer.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watxaut.fontsreviewer.domain.model.UserStats
import com.watxaut.fontsreviewer.domain.repository.AuthRepository
import com.watxaut.fontsreviewer.domain.usecase.GetUserStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val getUserStatsUseCase: GetUserStatsUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            _uiState.value = StatsUiState.Loading

            // Check if user is logged in
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                _uiState.value = StatsUiState.NotLoggedIn
                return@launch
            }

            // Get user stats
            getUserStatsUseCase(currentUser.id)
                .onSuccess { stats ->
                    _uiState.value = StatsUiState.Success(
                        nickname = currentUser.nickname,
                        stats = stats
                    )
                }
                .onFailure { error ->
                    _uiState.value = StatsUiState.Error(
                        error.message ?: "Failed to load stats"
                    )
                }
        }
    }

    fun onRefresh() {
        loadStats()
    }

    fun onBestFountainClick() {
        // Navigate to best fountain on map
        // This will be handled by the screen
    }
}

sealed class StatsUiState {
    object Loading : StatsUiState()
    object NotLoggedIn : StatsUiState()
    data class Success(
        val nickname: String,
        val stats: UserStats
    ) : StatsUiState()
    data class Error(val message: String) : StatsUiState()
}
