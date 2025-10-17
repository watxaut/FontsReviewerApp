package com.watxaut.fontsreviewer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CreateReviewRequestTest {

    @Test
    fun `overall score is calculated correctly as average of all ratings`() {
        val request = CreateReviewRequest(
            fountainId = "test-fountain",
            taste = 5,
            freshness = 4,
            locationRating = 3,
            aesthetics = 5,
            splash = 2,
            jet = 5,
            comment = "Test comment"
        )

        val expectedOverall = (5 + 4 + 3 + 5 + 2 + 5) / 6.0
        assertEquals(expectedOverall, request.overall, 0.01)
    }

    @Test
    fun `overall score with all 5s equals 5`() {
        val request = CreateReviewRequest(
            fountainId = "test-fountain",
            taste = 5,
            freshness = 5,
            locationRating = 5,
            aesthetics = 5,
            splash = 5,
            jet = 5
        )

        assertEquals(5.0, request.overall, 0.01)
    }

    @Test
    fun `overall score with all 1s equals 1`() {
        val request = CreateReviewRequest(
            fountainId = "test-fountain",
            taste = 1,
            freshness = 1,
            locationRating = 1,
            aesthetics = 1,
            splash = 1,
            jet = 1
        )

        assertEquals(1.0, request.overall, 0.01)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `taste rating below 1 throws exception`() {
        CreateReviewRequest(
            fountainId = "test-fountain",
            taste = 0,
            freshness = 3,
            locationRating = 3,
            aesthetics = 3,
            splash = 3,
            jet = 3
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `taste rating above 5 throws exception`() {
        CreateReviewRequest(
            fountainId = "test-fountain",
            taste = 6,
            freshness = 3,
            locationRating = 3,
            aesthetics = 3,
            splash = 3,
            jet = 3
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `freshness rating below 1 throws exception`() {
        CreateReviewRequest(
            fountainId = "test-fountain",
            taste = 3,
            freshness = 0,
            locationRating = 3,
            aesthetics = 3,
            splash = 3,
            jet = 3
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `freshness rating above 5 throws exception`() {
        CreateReviewRequest(
            fountainId = "test-fountain",
            taste = 3,
            freshness = 6,
            locationRating = 3,
            aesthetics = 3,
            splash = 3,
            jet = 3
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `locationRating below 1 throws exception`() {
        CreateReviewRequest(
            fountainId = "test-fountain",
            taste = 3,
            freshness = 3,
            locationRating = 0,
            aesthetics = 3,
            splash = 3,
            jet = 3
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `aesthetics rating below 1 throws exception`() {
        CreateReviewRequest(
            fountainId = "test-fountain",
            taste = 3,
            freshness = 3,
            locationRating = 3,
            aesthetics = 0,
            splash = 3,
            jet = 3
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `splash rating below 1 throws exception`() {
        CreateReviewRequest(
            fountainId = "test-fountain",
            taste = 3,
            freshness = 3,
            locationRating = 3,
            aesthetics = 3,
            splash = 0,
            jet = 3
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `jet rating below 1 throws exception`() {
        CreateReviewRequest(
            fountainId = "test-fountain",
            taste = 3,
            freshness = 3,
            locationRating = 3,
            aesthetics = 3,
            splash = 3,
            jet = 0
        )
    }

    @Test
    fun `comment is optional and can be null`() {
        val request = CreateReviewRequest(
            fountainId = "test-fountain",
            taste = 3,
            freshness = 3,
            locationRating = 3,
            aesthetics = 3,
            splash = 3,
            jet = 3,
            comment = null
        )

        assertEquals(null, request.comment)
    }

    @Test
    fun `valid request with all boundary values succeeds`() {
        val request = CreateReviewRequest(
            fountainId = "test-fountain",
            taste = 1,
            freshness = 5,
            locationRating = 1,
            aesthetics = 5,
            splash = 1,
            jet = 5
        )

        assertEquals(1, request.taste)
        assertEquals(5, request.freshness)
        assertEquals(1, request.locationRating)
        assertEquals(5, request.aesthetics)
        assertEquals(1, request.splash)
        assertEquals(5, request.jet)
    }
}
