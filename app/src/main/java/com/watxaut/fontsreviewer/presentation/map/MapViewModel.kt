package com.watxaut.fontsreviewer.presentation.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watxaut.fontsreviewer.domain.model.Fountain
import com.watxaut.fontsreviewer.domain.repository.AuthRepository
import com.watxaut.fontsreviewer.domain.usecase.GetFountainsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<com.watxaut.fontsreviewer.domain.model.User?>(null)
    
    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    init {
        loadCurrentUser()
        loadFountains()
    }
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            _currentUser.value = authRepository.getCurrentUser()
        }
    }

    private fun loadFountains() {
        viewModelScope.launch {
            getFountainsUseCase()
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
                        
                        // Debug logging
                        android.util.Log.i("MapViewModel", "=== MAP DATA ===")
                        android.util.Log.i("MapViewModel", "Total fountains: ${fountains.size}")
                        android.util.Log.i("MapViewModel", "Fountains with reviews: ${fountains.count { it.totalReviews > 0 }}")
                        android.util.Log.i("MapViewModel", "Best fountain globally: $bestFountainId")
                        android.util.Log.i("MapViewModel", "Current user: ${user?.nickname}")
                        android.util.Log.i("MapViewModel", "User best fountain: $userBestFountainId")
                        
                        // Log top 5 fountains by rating
                        fountains
                            .filter { it.totalReviews > 0 }
                            .sortedByDescending { it.averageRating }
                            .take(5)
                            .forEachIndexed { index, fountain ->
                                android.util.Log.i("MapViewModel", "Top ${index + 1}: ${fountain.codi} - ${fountain.nom} (${fountain.averageRating} avg, ${fountain.totalReviews} reviews)")
                            }
                        
                        MapUiState.Success(
                            fountains = fountains,
                            bestFountainId = bestFountainId,
                            userBestFountainId = userBestFountainId,
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
}

sealed class MapUiState {
    object Loading : MapUiState()
    object Empty : MapUiState()
    data class Success(
        val fountains: List<Fountain>,
        val bestFountainId: String? = null,
        val userBestFountainId: String? = null,
        val currentUser: com.watxaut.fontsreviewer.domain.model.User? = null
    ) : MapUiState()
    data class Error(val message: String) : MapUiState()
}

data class UserLocationState(
    val location: Location? = null,
    val hasPermission: Boolean = false
)
