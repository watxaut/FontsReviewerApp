package com.watxaut.fontsreviewer.presentation.review

import androidx.lifecycle.SavedStateHandle
import com.watxaut.fontsreviewer.domain.model.CreateReviewRequest
import com.watxaut.fontsreviewer.domain.usecase.SubmitReviewUseCase
import com.watxaut.fontsreviewer.util.MainDispatcherRule
import com.watxaut.fontsreviewer.util.TestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var submitReviewUseCase: SubmitReviewUseCase
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: ReviewViewModel

    private val testFountainId = "test-fountain-id"

    @Before
    fun setup() {
        submitReviewUseCase = mockk()
        savedStateHandle = mockk()
        every { savedStateHandle.get<String>("fountainId") } returns testFountainId
        viewModel = ReviewViewModel(submitReviewUseCase, savedStateHandle)
    }

    @Test
    fun `initial state has all ratings at 0`() {
        val state = viewModel.uiState.value
        
        assertEquals(0, state.taste)
        assertEquals(0, state.freshness)
        assertEquals(0, state.locationRating)
        assertEquals(0, state.aesthetics)
        assertEquals(0, state.splash)
        assertEquals(0, state.jet)
        assertEquals("", state.comment)
        assertFalse(state.isSubmitting)
        assertNull(state.errorMessage)
        assertFalse(state.submitSuccess)
    }

    @Test
    fun `onTasteChange updates taste rating`() {
        viewModel.onTasteChange(4f)
        
        assertEquals(4, viewModel.uiState.value.taste)
    }

    @Test
    fun `onFreshnessChange updates freshness rating`() {
        viewModel.onFreshnessChange(5f)
        
        assertEquals(5, viewModel.uiState.value.freshness)
    }

    @Test
    fun `onLocationChange updates location rating`() {
        viewModel.onLocationChange(3f)
        
        assertEquals(3, viewModel.uiState.value.locationRating)
    }

    @Test
    fun `onAestheticsChange updates aesthetics rating`() {
        viewModel.onAestheticsChange(4f)
        
        assertEquals(4, viewModel.uiState.value.aesthetics)
    }

    @Test
    fun `onSplashChange updates splash rating`() {
        viewModel.onSplashChange(2f)
        
        assertEquals(2, viewModel.uiState.value.splash)
    }

    @Test
    fun `onJetChange updates jet rating`() {
        viewModel.onJetChange(5f)
        
        assertEquals(5, viewModel.uiState.value.jet)
    }

    @Test
    fun `onCommentChange updates comment`() {
        val comment = "Great fountain!"
        viewModel.onCommentChange(comment)
        
        assertEquals(comment, viewModel.uiState.value.comment)
    }

    @Test
    fun `onCommentChange limits comment to MAX_COMMENT_LENGTH`() {
        val longComment = "a".repeat(600)
        viewModel.onCommentChange(longComment)
        
        // Comment should not exceed MAX_COMMENT_LENGTH (500)
        assertTrue(viewModel.uiState.value.comment.length <= ReviewViewModel.MAX_COMMENT_LENGTH)
    }

    @Test
    fun `onCommentChange accepts exactly MAX_COMMENT_LENGTH characters`() {
        val exactComment = "a".repeat(ReviewViewModel.MAX_COMMENT_LENGTH)
        viewModel.onCommentChange(exactComment)
        
        assertEquals(ReviewViewModel.MAX_COMMENT_LENGTH, viewModel.uiState.value.comment.length)
    }

    @Test
    fun `onSubmit with incomplete ratings shows error`() {
        viewModel.onTasteChange(4f)
        viewModel.onFreshnessChange(5f)
        // Missing other ratings
        viewModel.onSubmit()
        
        val state = viewModel.uiState.value
        assertEquals("Please rate all categories", state.errorMessage)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `onSubmit with all zero ratings shows error`() {
        viewModel.onSubmit()
        
        assertEquals("Please rate all categories", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onSubmit with valid ratings succeeds`() = runTest {
        // Given
        viewModel.onTasteChange(4f)
        viewModel.onFreshnessChange(5f)
        viewModel.onLocationChange(3f)
        viewModel.onAestheticsChange(4f)
        viewModel.onSplashChange(3f)
        viewModel.onJetChange(5f)
        viewModel.onCommentChange("Great fountain!")
        
        coEvery { submitReviewUseCase(any()) } returns Result.success(TestData.testReview)

        // When
        viewModel.onSubmit()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.submitSuccess)
        assertFalse(state.isSubmitting)
        assertNull(state.errorMessage)
        coVerify(exactly = 1) { submitReviewUseCase(any()) }
    }

    @Test
    fun `onSubmit handles duplicate review error`() = runTest {
        // Given
        viewModel.onTasteChange(4f)
        viewModel.onFreshnessChange(5f)
        viewModel.onLocationChange(3f)
        viewModel.onAestheticsChange(4f)
        viewModel.onSplashChange(3f)
        viewModel.onJetChange(5f)
        
        coEvery { submitReviewUseCase(any()) } returns Result.failure(
            Exception("unique_user_fountain")
        )

        // When
        viewModel.onSubmit()

        // Then
        val state = viewModel.uiState.value
        assertEquals("You have already reviewed this fountain", state.errorMessage)
        assertFalse(state.submitSuccess)
    }

    @Test
    fun `onSubmit handles generic error`() = runTest {
        // Given
        viewModel.onTasteChange(4f)
        viewModel.onFreshnessChange(5f)
        viewModel.onLocationChange(3f)
        viewModel.onAestheticsChange(4f)
        viewModel.onSplashChange(3f)
        viewModel.onJetChange(5f)
        
        coEvery { submitReviewUseCase(any()) } returns Result.failure(
            Exception("Network error")
        )

        // When
        viewModel.onSubmit()

        // Then
        assertEquals("Network error", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onSubmit with empty comment succeeds`() = runTest {
        // Given
        viewModel.onTasteChange(4f)
        viewModel.onFreshnessChange(5f)
        viewModel.onLocationChange(3f)
        viewModel.onAestheticsChange(4f)
        viewModel.onSplashChange(3f)
        viewModel.onJetChange(5f)
        // No comment
        
        coEvery { submitReviewUseCase(any()) } returns Result.success(TestData.testReview)

        // When
        viewModel.onSubmit()

        // Then
        assertTrue(viewModel.uiState.value.submitSuccess)
    }

    @Test
    fun `onSubmitSuccess clears success flag`() = runTest {
        // Given
        viewModel.onTasteChange(4f)
        viewModel.onFreshnessChange(5f)
        viewModel.onLocationChange(3f)
        viewModel.onAestheticsChange(4f)
        viewModel.onSplashChange(3f)
        viewModel.onJetChange(5f)
        
        coEvery { submitReviewUseCase(any()) } returns Result.success(TestData.testReview)
        viewModel.onSubmit()
        assertTrue(viewModel.uiState.value.submitSuccess)

        // When
        viewModel.onSubmitSuccess()

        // Then
        assertFalse(viewModel.uiState.value.submitSuccess)
    }

    @Test
    fun `onSubmit calls use case with correct fountain id`() = runTest {
        // Given
        viewModel.onTasteChange(4f)
        viewModel.onFreshnessChange(5f)
        viewModel.onLocationChange(3f)
        viewModel.onAestheticsChange(4f)
        viewModel.onSplashChange(3f)
        viewModel.onJetChange(5f)
        
        coEvery { submitReviewUseCase(any()) } returns Result.success(TestData.testReview)

        // When
        viewModel.onSubmit()

        // Then
        coVerify { submitReviewUseCase(match { it.fountainId == testFountainId }) }
    }
}
