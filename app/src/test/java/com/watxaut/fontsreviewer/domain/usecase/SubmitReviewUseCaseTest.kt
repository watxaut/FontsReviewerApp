package com.watxaut.fontsreviewer.domain.usecase

import com.watxaut.fontsreviewer.domain.repository.ReviewRepository
import com.watxaut.fontsreviewer.util.TestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SubmitReviewUseCaseTest {

    private lateinit var reviewRepository: ReviewRepository
    private lateinit var submitReviewUseCase: SubmitReviewUseCase

    @Before
    fun setup() {
        reviewRepository = mockk()
        submitReviewUseCase = SubmitReviewUseCase(reviewRepository)
    }

    @Test
    fun `invoke with valid request returns success with review`() = runTest {
        // Given
        val request = TestData.testCreateReviewRequest
        val expectedReview = TestData.testReview
        coEvery { reviewRepository.submitReview(request) } returns Result.success(expectedReview)

        // When
        val result = submitReviewUseCase(request)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedReview, result.getOrNull())
        coVerify(exactly = 1) { reviewRepository.submitReview(request) }
    }

    @Test
    fun `invoke with duplicate review returns failure`() = runTest {
        // Given
        val request = TestData.testCreateReviewRequest
        val exception = Exception("User has already reviewed this fountain")
        coEvery { reviewRepository.submitReview(request) } returns Result.failure(exception)

        // When
        val result = submitReviewUseCase(request)

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
        coVerify(exactly = 1) { reviewRepository.submitReview(request) }
    }

    @Test
    fun `invoke calls repository with correct request`() = runTest {
        // Given
        val request = TestData.testCreateReviewRequest
        coEvery { reviewRepository.submitReview(request) } returns Result.success(TestData.testReview)

        // When
        submitReviewUseCase(request)

        // Then
        coVerify(exactly = 1) { reviewRepository.submitReview(request) }
    }
}
