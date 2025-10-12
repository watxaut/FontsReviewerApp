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

    init {
        // Listen to Supabase session status changes
        listenToSessionChanges()
        loadCurrentUser()
        loadFountains()
    }
    
    private fun listenToSessionChanges() {
        viewModelScope.launch {
            supabaseClient.auth.sessionStatus.collect { status: SessionStatus ->
                android.util.Log.e("MapViewModel", "Session status changed: $status")
                when (status) {
                    is SessionStatus.Authenticated -> {
                        android.util.Log.e("MapViewModel", "User authenticated! Source: ${status.source}")
                        // Reload user when session changes
                        loadCurrentUser()
                        loadFountains()
                    }
                    is SessionStatus.NotAuthenticated -> {
                        android.util.Log.e("MapViewModel", "User not authenticated")
                        _currentUser.value = null
                        loadFountains()
                    }
                    else -> {
                        android.util.Log.e("MapViewModel", "Session status: $status")
                    }
                }
            }
        }
    }
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            android.util.Log.e("MapViewModel", "DEBUG: loadCurrentUser() called")
            android.util.Log.e("MapViewModel", "DEBUG: User from authRepository = $user")
            android.util.Log.e("MapViewModel", "DEBUG: User role = ${user?.role}")
            _currentUser.value = user
        }
    }

    private fun loadFountains() {
        viewModelScope.launch {
            getFountainsUseCase(includeDeleted = _showDeletedFountains.value)
                .catch { e ->
                    val errorMessage = when {
                        e.message?.contains("No internet", ignoreCase = true) == true ->
                            "No internet connection. Please check your network settings and try again."
                        else -> e.message ?: "Unknown error"
                    }
                    _uiState.value = MapUiState.Error(errorMessage)
                }
                .combine(_currentUser) { fountains, user ->
                    if (fountains.isEmpty()) {
                        MapUiState.Empty
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
                        
                        MapUiState.Success(
                            fountains = fountains,
                            bestFountainId = bestFountainId,
                            userBestFountainId = userBestFountainId,
                            userReviewedFountainIds = userReviewedIds,
                            currentUser = user
                        )
                    }
                }
                .collect { state ->
                    _uiState.value = state
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
        loadCurrentUser()
        loadFountains()
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
        loadFountains() // Reload fountains with new filter
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
