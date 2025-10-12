package com.watxaut.fontsreviewer.presentation.addfountain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watxaut.fontsreviewer.domain.usecase.CreateFountainUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AddFountainUiState {
    object Initial : AddFountainUiState()
    object Loading : AddFountainUiState()
    data class Success(val fountainId: String) : AddFountainUiState()
    data class Error(val message: String) : AddFountainUiState()
}

@HiltViewModel
class AddFountainViewModel @Inject constructor(
    private val createFountainUseCase: CreateFountainUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddFountainUiState>(AddFountainUiState.Initial)
    val uiState: StateFlow<AddFountainUiState> = _uiState.asStateFlow()

    fun createFountain(
        name: String,
        street: String,
        streetNumber: String,
        latitude: Double,
        longitude: Double
    ) {
        viewModelScope.launch {
            _uiState.value = AddFountainUiState.Loading
            
            createFountainUseCase(
                nom = name,
                carrer = street,
                numeroCarrer = streetNumber,
                latitude = latitude,
                longitude = longitude
            ).fold(
                onSuccess = { fountainId ->
                    _uiState.value = AddFountainUiState.Success(fountainId)
                },
                onFailure = { error ->
                    _uiState.value = AddFountainUiState.Error(
                        error.message ?: "Unknown error occurred"
                    )
                }
            )
        }
    }
}
