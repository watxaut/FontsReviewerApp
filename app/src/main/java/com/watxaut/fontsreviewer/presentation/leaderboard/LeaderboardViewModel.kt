package com.watxaut.fontsreviewer.presentation.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watxaut.fontsreviewer.domain.model.LeaderboardEntry
import com.watxaut.fontsreviewer.domain.repository.AuthRepository
import com.watxaut.fontsreviewer.domain.usecase.GetLeaderboardUseCase
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
class LeaderboardViewModel @Inject constructor(
    private val getLeaderboardUseCase: GetLeaderboardUseCase,
    private val authRepository: AuthRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<LeaderboardUiState>(LeaderboardUiState.Loading)
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        loadLeaderboard()
        listenToProfileChanges()
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
    
    /**
     * Listen to realtime changes in profiles table to auto-refresh leaderboard
     * The profiles table is updated by database triggers when reviews are inserted/updated/deleted
     */
    private fun listenToProfileChanges() {
        viewModelScope.launch {
            try {
                val channel = supabaseClient.channel("leaderboard_profiles")
                
                // Listen to all profile updates (triggered by review changes)
                channel.postgresChangeFlow<PostgresAction>("public:profiles").onEach { action ->
                    // Profile was updated (stats changed) - refresh leaderboard
                    loadLeaderboard()
                }.launchIn(viewModelScope)
                
                channel.subscribe()
            } catch (e: Exception) {
                // Realtime might not be available, that's OK
                // The LaunchedEffect in LeaderboardScreen will handle refresh anyway
            }
        }
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
