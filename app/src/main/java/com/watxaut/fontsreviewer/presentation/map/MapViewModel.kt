package com.watxaut.fontsreviewer.presentation.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watxaut.fontsreviewer.domain.model.Fountain
import com.watxaut.fontsreviewer.domain.repository.AuthRepository
import com.watxaut.fontsreviewer.domain.usecase.GetFountainsUseCase
import com.watxaut.fontsreviewer.domain.usecase.GetUserReviewedFountainsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val getFountainsUseCase: GetFountainsUseCase,
    private val authRepository: AuthRepository,
    private val getUserReviewedFountainsUseCase: GetUserReviewedFountainsUseCase,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<com.watxaut.fontsreviewer.domain.model.User?>(null)
    
    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()
    
    private val _showDeletedFountains = MutableStateFlow(false)
    val showDeletedFountains: StateFlow<Boolean> = _showDeletedFountains.asStateFlow()
    
    // Trigger to force refresh
    private val _refreshTrigger = MutableStateFlow(0)

    init {
        // Listen to Supabase session status changes
        listenToSessionChanges()
        loadCurrentUser()
        loadFountains()
    }
    
    private fun listenToSessionChanges() {
        viewModelScope.launch {
            supabaseClient.auth.sessionStatus.collect { status: SessionStatus ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        // Reload user when session changes
                        loadCurrentUser()
                        loadFountains()
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _currentUser.value = null
                        loadFountains()
                    }
                    else -> {
                        // Other session states
                    }
                }
            }
        }
    }
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            _currentUser.value = user
        }
    }

    private fun loadFountains() {
        viewModelScope.launch {
            combine(
                _showDeletedFountains,
                _currentUser,
                _refreshTrigger
            ) { showDeleted, user, _ ->
                Triple(showDeleted, user, Unit)
            }.collect { (showDeleted, user, _) ->
                // Fetch fountains from use case
                getFountainsUseCase(includeDeleted = showDeleted)
                    .catch { e ->
                        val errorMessage = when {
                            e.message?.contains("No internet", ignoreCase = true) == true ->
                                "No internet connection. Please check your network settings and try again."
                            else -> e.message ?: "Unknown error"
                        }
                        _uiState.value = MapUiState.Error(errorMessage)
                    }
                    .collect { fountains ->
                        if (fountains.isEmpty()) {
                            _uiState.value = MapUiState.Empty
                        } else {
                            // Find the best rated fountain globally
                            val bestFountainId = fountains
                                .filter { it.totalReviews > 0 }
                                .maxByOrNull { it.averageRating }?.codi
                            
                            // Get user's best fountain
                            val userBestFountainId = user?.bestFountainId
                            
                            // Get user's reviewed fountain IDs
                            val userReviewedIds = if (user != null) {
                                getUserReviewedFountainsUseCase(user.id)
                                    .getOrNull()
                                    ?: emptySet()
                            } else {
                                emptySet()
                            }
                            
                            _uiState.value = MapUiState.Success(
                                fountains = fountains,
                                bestFountainId = bestFountainId,
                                userBestFountainId = userBestFountainId,
                                userReviewedFountainIds = userReviewedIds,
                                currentUser = user
                            )
                        }
                    }
            }
        }
    }

    fun refreshUser() {
        viewModelScope.launch {
            _currentUser.value = authRepository.getCurrentUser()
        }
    }
    
    /**
     * Refreshes both fountain data and user data
     * Call this after submitting a review to recalculate best fountains
     */
    fun refresh() {
        viewModelScope.launch {
            // Reload user data first
            _currentUser.value = authRepository.getCurrentUser()
            // Trigger refresh by incrementing the counter
            _refreshTrigger.value += 1
        }
    }
    
    /**
     * Update user's current location
     */
    fun updateUserLocation(location: Location?) {
        _userLocation.value = location
    }
    
    /**
     * Toggle showing deleted fountains (admin only)
     */
    fun toggleShowDeletedFountains() {
        _showDeletedFountains.value = !_showDeletedFountains.value
        // Trigger refresh to reload fountains with new filter
        _refreshTrigger.value += 1
    }
}

sealed class MapUiState {
    object Loading : MapUiState()
    object Empty : MapUiState()
    data class Success(
        val fountains: List<Fountain>,
        val bestFountainId: String? = null,
        val userBestFountainId: String? = null,
        val userReviewedFountainIds: Set<String> = emptySet(),
        val currentUser: com.watxaut.fontsreviewer.domain.model.User? = null
    ) : MapUiState()
    data class Error(val message: String) : MapUiState()
}

data class UserLocationState(
    val location: Location? = null,
    val hasPermission: Boolean = false
)
