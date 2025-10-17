package com.watxaut.fontsreviewer.data.mapper

import com.watxaut.fontsreviewer.data.remote.dto.CreateReviewDto
import com.watxaut.fontsreviewer.data.remote.dto.LeaderboardDto
import com.watxaut.fontsreviewer.data.remote.dto.ReviewDto
import com.watxaut.fontsreviewer.domain.model.CreateReviewRequest
import com.watxaut.fontsreviewer.util.SecureLog
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ReviewMapperTest {
    
    @Before
    fun setup() {
        // Mock SecureLog to avoid Android framework dependencies
        mockkObject(SecureLog)
        every { SecureLog.e(any(), any(), any<Throwable>()) } returns Unit
    }
    
    @After
    fun tearDown() {
        unmockkObject(SecureLog)
    }

    @Test
    fun `ReviewDto toDomain maps all fields correctly`() {
        val dto = ReviewDto(
            id = "review-123",
            fountainId = "fountain-456",
            userId = "user-789",
            userNickname = "TestUser",
            taste = 4,
            freshness = 5,
            locationRating = 3,
            aesthetics = 4,
            splash = 3,
            jet = 5,
            overall = 4.0,
            comment = "Great fountain!",
            createdAt = "2023-10-01T12:00:00Z",
            updatedAt = "2023-10-01T12:00:00Z"
        )

        val domain = dto.toDomain()

        assertEquals("review-123", domain.id)
        assertEquals("fountain-456", domain.fountainId)
        assertEquals("user-789", domain.userId)
        assertEquals("TestUser", domain.userNickname)
        assertEquals(4, domain.taste)
        assertEquals(5, domain.freshness)
        assertEquals(3, domain.locationRating)
        assertEquals(4, domain.aesthetics)
        assertEquals(3, domain.splash)
        assertEquals(5, domain.jet)
        assertEquals(4.0, domain.overall, 0.01)
        assertEquals("Great fountain!", domain.comment)
        assertTrue(domain.createdAt > 0)
        assertTrue(domain.updatedAt > 0)
    }

    @Test
    fun `ReviewDto toDomain handles null comment`() {
        val dto = ReviewDto(
            id = "review-123",
            fountainId = "fountain-456",
            userId = "user-789",
            userNickname = "TestUser",
            taste = 4,
            freshness = 5,
            locationRating = 3,
            aesthetics = 4,
            splash = 3,
            jet = 5,
            overall = 4.0,
            comment = null,
            createdAt = "2023-10-01T12:00:00Z",
            updatedAt = "2023-10-01T12:00:00Z"
        )

        val domain = dto.toDomain()

        assertNull(domain.comment)
    }

    @Test
    fun `CreateReviewRequest toDto maps all fields correctly`() {
        val request = CreateReviewRequest(
            fountainId = "fountain-123",
            taste = 4,
            freshness = 5,
            locationRating = 3,
            aesthetics = 4,
            splash = 3,
            jet = 5,
            comment = "Test comment"
        )

        val dto = request.toDto(userId = "user-456", userNickname = "TestUser")

        assertEquals("fountain-123", dto.fountainId)
        assertEquals("user-456", dto.userId)
        assertEquals("TestUser", dto.userNickname)
        assertEquals(4, dto.taste)
        assertEquals(5, dto.freshness)
        assertEquals(3, dto.locationRating)
        assertEquals(4, dto.aesthetics)
        assertEquals(3, dto.splash)
        assertEquals(5, dto.jet)
        assertEquals("Test comment", dto.comment)
    }

    @Test
    fun `CreateReviewRequest toDto handles null comment`() {
        val request = CreateReviewRequest(
            fountainId = "fountain-123",
            taste = 4,
            freshness = 5,
            locationRating = 3,
            aesthetics = 4,
            splash = 3,
            jet = 5,
            comment = null
        )

        val dto = request.toDto(userId = "user-456", userNickname = "TestUser")

        assertNull(dto.comment)
    }

    @Test
    fun `LeaderboardDto toDomain maps all fields correctly`() {
        val dto = LeaderboardDto(
            nickname = "TopUser",
            totalRatings = 50,
            averageScore = 4.8,
            rank = 1
        )

        val domain = dto.toDomain()

        assertEquals("TopUser", domain.nickname)
        assertEquals(50, domain.totalRatings)
        assertEquals(4.8, domain.averageScore, 0.01)
        assertEquals(1, domain.rank)
    }

    @Test
    fun `timestamp parsing handles valid ISO format`() {
        val dto = ReviewDto(
            id = "review-123",
            fountainId = "fountain-456",
            userId = "user-789",
            userNickname = "TestUser",
            taste = 4,
            freshness = 5,
            locationRating = 3,
            aesthetics = 4,
            splash = 3,
            jet = 5,
            overall = 4.0,
            comment = null,
            createdAt = "2023-10-01T12:00:00Z",
            updatedAt = "2023-10-01T13:30:00Z"
        )

        val domain = dto.toDomain()

        assertTrue(domain.createdAt > 0)
        assertTrue(domain.updatedAt > 0)
        // Updated timestamp should be after created timestamp
        assertTrue(domain.updatedAt >= domain.createdAt)
    }

    @Test
    fun `timestamp parsing handles invalid format gracefully`() {
        val dto = ReviewDto(
            id = "review-123",
            fountainId = "fountain-456",
            userId = "user-789",
            userNickname = "TestUser",
            taste = 4,
            freshness = 5,
            locationRating = 3,
            aesthetics = 4,
            splash = 3,
            jet = 5,
            overall = 4.0,
            comment = null,
            createdAt = "invalid-timestamp",
            updatedAt = "invalid-timestamp"
        )

        val domain = dto.toDomain()

        // Should fall back to 0L (epoch) when timestamp parsing fails
        // This makes it obvious when timestamps fail (shows as Jan 1, 1970)
        assertEquals(0L, domain.createdAt)
        assertEquals(0L, domain.updatedAt)
    }
}
