package com.watxaut.fontsreviewer.domain.usecase

import com.watxaut.fontsreviewer.domain.repository.FountainRepository
import com.watxaut.fontsreviewer.util.TestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GetFountainsUseCaseTest {

    private lateinit var fountainRepository: FountainRepository
    private lateinit var getFountainsUseCase: GetFountainsUseCase

    @Before
    fun setup() {
        fountainRepository = mockk()
        getFountainsUseCase = GetFountainsUseCase(fountainRepository)
    }

    @Test
    fun `invoke without includeDeleted returns active fountains only`() = runTest {
        // Given
        val activeFountains = listOf(TestData.testFountain, TestData.testFountain2)
        every { fountainRepository.getAllFountains(false) } returns flowOf(activeFountains)

        // When
        val result = getFountainsUseCase(includeDeleted = false).first()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { !it.isDeleted })
        verify(exactly = 1) { fountainRepository.getAllFountains(false) }
    }

    @Test
    fun `invoke with includeDeleted true returns all fountains`() = runTest {
        // Given
        val allFountains = listOf(
            TestData.testFountain,
            TestData.testFountain2,
            TestData.testFountain.copy(codi = "deleted-1", isDeleted = true)
        )
        every { fountainRepository.getAllFountains(true) } returns flowOf(allFountains)

        // When
        val result = getFountainsUseCase(includeDeleted = true).first()

        // Then
        assertEquals(3, result.size)
        verify(exactly = 1) { fountainRepository.getAllFountains(true) }
    }

    @Test
    fun `invoke default parameter excludes deleted fountains`() = runTest {
        // Given
        val activeFountains = listOf(TestData.testFountain)
        every { fountainRepository.getAllFountains(false) } returns flowOf(activeFountains)

        // When
        val result = getFountainsUseCase().first()

        // Then
        assertEquals(1, result.size)
        assertFalse(result[0].isDeleted)
        verify(exactly = 1) { fountainRepository.getAllFountains(false) }
    }

    @Test
    fun `invoke returns empty list when no fountains exist`() = runTest {
        // Given
        every { fountainRepository.getAllFountains(false) } returns flowOf(emptyList())

        // When
        val result = getFountainsUseCase().first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke returns flow that updates when fountain data changes`() = runTest {
        // Given
        val initialFountains = listOf(TestData.testFountain)
        every { fountainRepository.getAllFountains(false) } returns flowOf(initialFountains)

        // When
        val flow = getFountainsUseCase()
        val result = flow.first()

        // Then
        assertEquals(1, result.size)
        assertEquals("fountain-1", result[0].codi)
    }
}
