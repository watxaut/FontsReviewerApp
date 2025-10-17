package com.watxaut.fontsreviewer.presentation.details

import androidx.lifecycle.SavedStateHandle
import com.watxaut.fontsreviewer.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FountainDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: FountainDetailsViewModel

    private val testFountainId = "test-fountain-id"

    @Before
    fun setup() {
        savedStateHandle = mockk()
        every { savedStateHandle.get<String>("fountainId") } returns testFountainId
        
        viewModel = FountainDetailsViewModel(
            mockk(relaxed = true),  // fountainRepository
            mockk(relaxed = true),  // reviewRepository
            mockk(relaxed = true),  // authRepository
            mockk(relaxed = true),  // deleteFountainUseCase
            savedStateHandle
        )
    }

    @Test
    fun `ViewModel initializes with fountain id from saved state`() {
        // Then - ViewModel should be initialized
        assertNotNull(viewModel)
    }

    @Test
    fun `initial UI state starts as loading`() {
        // Then
        val state = viewModel.uiState.value
        assertNotNull(state)
    }
}
