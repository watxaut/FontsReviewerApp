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

class GetLeaderboardUseCaseTest {

    private lateinit var reviewRepository: ReviewRepository
    private lateinit var getLeaderboardUseCase: GetLeaderboardUseCase

    @Before
    fun setup() {
        reviewRepository = mockk()
        getLeaderboardUseCase = GetLeaderboardUseCase(reviewRepository)
    }

    @Test
    fun `invoke returns success with leaderboard entries`() = runTest {
        // Given
        val expectedEntries = TestData.testLeaderboardEntries
        coEvery { reviewRepository.getLeaderboard() } returns Result.success(expectedEntries)

        // When
        val result = getLeaderboardUseCase()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedEntries, result.getOrNull())
        assertEquals(3, result.getOrNull()?.size)
        coVerify(exactly = 1) { reviewRepository.getLeaderboard() }
    }

    @Test
    fun `invoke returns empty list when no entries exist`() = runTest {
        // Given
        coEvery { reviewRepository.getLeaderboard() } returns Result.success(emptyList())

        // When
        val result = getLeaderboardUseCase()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
        coVerify(exactly = 1) { reviewRepository.getLeaderboard() }
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        // Given
        val exception = Exception("Failed to fetch leaderboard")
        coEvery { reviewRepository.getLeaderboard() } returns Result.failure(exception)

        // When
        val result = getLeaderboardUseCase()

        // Then
        assertTrue(result.isFailure)
        assertEquals("Failed to fetch leaderboard", result.exceptionOrNull()?.message)
        coVerify(exactly = 1) { reviewRepository.getLeaderboard() }
    }

    @Test
    fun `invoke returns entries in correct rank order`() = runTest {
        // Given
        val entries = TestData.testLeaderboardEntries
        coEvery { reviewRepository.getLeaderboard() } returns Result.success(entries)

        // When
        val result = getLeaderboardUseCase()

        // Then
        val leaderboard = result.getOrNull()!!
        assertEquals(1, leaderboard[0].rank)
        assertEquals(2, leaderboard[1].rank)
        assertEquals(3, leaderboard[2].rank)
    }
}
