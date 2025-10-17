package com.watxaut.fontsreviewer.domain.usecase

import com.watxaut.fontsreviewer.domain.repository.ReviewRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GetUserReviewedFountainsUseCaseTest {

    private lateinit var reviewRepository: ReviewRepository
    private lateinit var getUserReviewedFountainsUseCase: GetUserReviewedFountainsUseCase

    @Before
    fun setup() {
        reviewRepository = mockk()
        getUserReviewedFountainsUseCase = GetUserReviewedFountainsUseCase(reviewRepository)
    }

    @Test
    fun `invoke with valid userId returns success with fountain ids`() = runTest {
        // Given
        val userId = "user-123"
        val expectedFountainIds = setOf("fountain-1", "fountain-2", "fountain-3")
        coEvery { 
            reviewRepository.getUserReviewedFountainIds(userId) 
        } returns Result.success(expectedFountainIds)

        // When
        val result = getUserReviewedFountainsUseCase(userId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedFountainIds, result.getOrNull())
        assertEquals(3, result.getOrNull()?.size)
        coVerify(exactly = 1) { reviewRepository.getUserReviewedFountainIds(userId) }
    }

    @Test
    fun `invoke returns empty set when user has no reviews`() = runTest {
        // Given
        val userId = "user-123"
        coEvery { 
            reviewRepository.getUserReviewedFountainIds(userId) 
        } returns Result.success(emptySet())

        // When
        val result = getUserReviewedFountainsUseCase(userId)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
        coVerify(exactly = 1) { reviewRepository.getUserReviewedFountainIds(userId) }
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        // Given
        val userId = "user-123"
        val exception = Exception("Failed to fetch reviewed fountains")
        coEvery { 
            reviewRepository.getUserReviewedFountainIds(userId) 
        } returns Result.failure(exception)

        // When
        val result = getUserReviewedFountainsUseCase(userId)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Failed to fetch reviewed fountains", result.exceptionOrNull()?.message)
        coVerify(exactly = 1) { reviewRepository.getUserReviewedFountainIds(userId) }
    }

    @Test
    fun `invoke calls repository with correct userId`() = runTest {
        // Given
        val userId = "specific-user-id"
        coEvery { 
            reviewRepository.getUserReviewedFountainIds(userId) 
        } returns Result.success(emptySet())

        // When
        getUserReviewedFountainsUseCase(userId)

        // Then
        coVerify(exactly = 1) { reviewRepository.getUserReviewedFountainIds(userId) }
    }

    @Test
    fun `invoke returns unique fountain ids`() = runTest {
        // Given
        val userId = "user-123"
        val fountainIds = setOf("fountain-1", "fountain-2", "fountain-1")
        coEvery { 
            reviewRepository.getUserReviewedFountainIds(userId) 
        } returns Result.success(fountainIds)

        // When
        val result = getUserReviewedFountainsUseCase(userId)

        // Then
        // Set should only contain unique values
        assertEquals(2, result.getOrNull()?.size)
    }
}
