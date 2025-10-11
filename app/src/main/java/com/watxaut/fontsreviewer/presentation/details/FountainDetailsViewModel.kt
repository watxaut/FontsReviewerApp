package com.watxaut.fontsreviewer.presentation.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watxaut.fontsreviewer.domain.model.Fountain
import com.watxaut.fontsreviewer.domain.model.Review
import com.watxaut.fontsreviewer.domain.model.User
import com.watxaut.fontsreviewer.domain.repository.AuthRepository
import com.watxaut.fontsreviewer.domain.repository.FountainRepository
import com.watxaut.fontsreviewer.domain.repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class FountainDetailsViewModel @Inject constructor(
    private val fountainRepository: FountainRepository,
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val fountainId: String = checkNotNull(savedStateHandle["fountainId"])

    private val _uiState = MutableStateFlow<FountainDetailsUiState>(FountainDetailsUiState.Loading)
    val uiState: StateFlow<FountainDetailsUiState> = _uiState.asStateFlow()
    
    private var currentUser: User? = null

    init {
        loadCurrentUser()
        loadFountainDetails()
    }
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            currentUser = authRepository.getCurrentUser()
        }
    }
    
    fun getCurrentUser(): User? = currentUser

    private fun loadFountainDetails() {
        viewModelScope.launch {
            _uiState.value = FountainDetailsUiState.Loading

            try {
                val fountain = fountainRepository.getFountainByCodi(fountainId)
                if (fountain == null) {
                    _uiState.value = FountainDetailsUiState.Error("Fountain not found")
                    return@launch
                }

                // Load reviews
                reviewRepository.getReviewsForFountain(fountainId)
                    .onSuccess { reviews ->
                        _uiState.value = FountainDetailsUiState.Success(
                            fountain = fountain,
                            reviews = reviews
                        )
                    }
                    .onFailure { error ->
                        // Show fountain even if reviews fail to load
                        _uiState.value = FountainDetailsUiState.Success(
                            fountain = fountain,
                            reviews = emptyList(),
                            reviewsError = error.message
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = FountainDetailsUiState.Error(
                    e.message ?: "Unknown error"
                )
            }
        }
    }

    fun onRefresh() {
        loadFountainDetails()
    }
}

sealed class FountainDetailsUiState {
    object Loading : FountainDetailsUiState()
    data class Success(
        val fountain: Fountain,
        val reviews: List<Review>,
        val reviewsError: String? = null
    ) : FountainDetailsUiState()
    data class Error(val message: String) : FountainDetailsUiState()
}
