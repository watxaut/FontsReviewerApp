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

class GetUserStatsUseCaseTest {

    private lateinit var reviewRepository: ReviewRepository
    private lateinit var getUserStatsUseCase: GetUserStatsUseCase

    @Before
    fun setup() {
        reviewRepository = mockk()
        getUserStatsUseCase = GetUserStatsUseCase(reviewRepository)
    }

    @Test
    fun `invoke with valid userId returns success with stats`() = runTest {
        // Given
        val userId = "user-123"
        val expectedStats = TestData.testUserStats
        coEvery { reviewRepository.getUserStats(userId) } returns Result.success(expectedStats)

        // When
        val result = getUserStatsUseCase(userId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedStats, result.getOrNull())
        coVerify(exactly = 1) { reviewRepository.getUserStats(userId) }
    }

    @Test
    fun `invoke returns stats with correct values`() = runTest {
        // Given
        val userId = "user-123"
        val stats = TestData.testUserStats
        coEvery { reviewRepository.getUserStats(userId) } returns Result.success(stats)

        // When
        val result = getUserStatsUseCase(userId)

        // Then
        val userStats = result.getOrNull()!!
        assertEquals(10, userStats.totalRatings)
        assertEquals(4.5, userStats.averageScore, 0.01)
        assertNotNull(userStats.bestFountain)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        // Given
        val userId = "user-123"
        val exception = Exception("Failed to fetch stats")
        coEvery { reviewRepository.getUserStats(userId) } returns Result.failure(exception)

        // When
        val result = getUserStatsUseCase(userId)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Failed to fetch stats", result.exceptionOrNull()?.message)
        coVerify(exactly = 1) { reviewRepository.getUserStats(userId) }
    }

    @Test
    fun `invoke calls repository with correct userId`() = runTest {
        // Given
        val userId = "specific-user-id"
        coEvery { reviewRepository.getUserStats(userId) } returns Result.success(TestData.testUserStats)

        // When
        getUserStatsUseCase(userId)

        // Then
        coVerify(exactly = 1) { reviewRepository.getUserStats(userId) }
    }
}
