package com.watxaut.fontsreviewer.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watxaut.fontsreviewer.domain.model.UserStats
import com.watxaut.fontsreviewer.domain.repository.AuthRepository
import com.watxaut.fontsreviewer.domain.usecase.GetUserStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val getUserStatsUseCase: GetUserStatsUseCase,
    private val authRepository: AuthRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
        listenToReviewChanges()
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
    
    /**
     * Listen to realtime changes in profiles table to auto-refresh stats
     * The profiles table is updated by database triggers when reviews are inserted/updated/deleted
     */
    private fun listenToReviewChanges() {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    val channel = supabaseClient.channel("stats_profile_${currentUser.id}")
                    
                    // Listen to profile changes (which are triggered by review changes via DB triggers)
                    channel.postgresChangeFlow<PostgresAction>("public:profiles").onEach { action ->
                        // Profile was updated (stats changed) - refresh if it's the current user's profile
                        loadStats()
                    }.launchIn(viewModelScope)
                    
                    channel.subscribe()
                }
            } catch (e: Exception) {
                // Realtime might not be available, that's OK
                // The LaunchedEffect in StatsScreen will handle refresh anyway
            }
        }
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
