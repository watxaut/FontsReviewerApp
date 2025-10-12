package com.watxaut.fontsreviewer.presentation.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watxaut.fontsreviewer.domain.model.CreateReviewRequest
import com.watxaut.fontsreviewer.domain.usecase.SubmitReviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val submitReviewUseCase: SubmitReviewUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val fountainId: String = checkNotNull(savedStateHandle["fountainId"])

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    fun onTasteChange(value: Float) {
        _uiState.update { it.copy(taste = value.toInt()) }
    }

    fun onFreshnessChange(value: Float) {
        _uiState.update { it.copy(freshness = value.toInt()) }
    }

    fun onLocationChange(value: Float) {
        _uiState.update { it.copy(locationRating = value.toInt()) }
    }

    fun onAestheticsChange(value: Float) {
        _uiState.update { it.copy(aesthetics = value.toInt()) }
    }

    fun onSplashChange(value: Float) {
        _uiState.update { it.copy(splash = value.toInt()) }
    }

    fun onJetChange(value: Float) {
        _uiState.update { it.copy(jet = value.toInt()) }
    }

    fun onSubmit() {
        val state = _uiState.value

        // Validation
        if (state.taste < 1 || state.freshness < 1 || state.locationRating < 1 ||
            state.aesthetics < 1 || state.splash < 1 || state.jet < 1) {
            _uiState.update { it.copy(errorMessage = "Please rate all categories") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            val request = CreateReviewRequest(
                fountainId = fountainId,
                taste = state.taste,
                freshness = state.freshness,
                locationRating = state.locationRating,
                aesthetics = state.aesthetics,
                splash = state.splash,
                jet = state.jet,
                comment = null
            )

            submitReviewUseCase(request)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false, submitSuccess = true) }
                }
                .onFailure { error ->
                    // Parse error message to show user-friendly text
                    val userFriendlyMessage = when {
                        error.message?.contains("unique_user_fountain", ignoreCase = true) == true -> 
                            "You have already reviewed this fountain"
                        error.message?.contains("duplicate", ignoreCase = true) == true -> 
                            "You have already reviewed this fountain"
                        error.message?.contains("already reviewed", ignoreCase = true) == true -> 
                            "You have already reviewed this fountain"
                        else -> error.message ?: "Failed to submit review"
                    }
                    
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = userFriendlyMessage
                        )
                    }
                }
        }
    }

    fun onSubmitSuccess() {
        _uiState.update { it.copy(submitSuccess = false) }
    }
}

data class ReviewUiState(
    val taste: Int = 0,
    val freshness: Int = 0,
    val locationRating: Int = 0,
    val aesthetics: Int = 0,
    val splash: Int = 0,
    val jet: Int = 0,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val submitSuccess: Boolean = false
)
