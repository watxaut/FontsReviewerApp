package com.watxaut.fontsreviewer.util

import com.watxaut.fontsreviewer.domain.model.*

/**
 * Test data objects for consistent testing across the codebase.
 */
object TestData {
    
    // Users
    val testUser = User(
        id = "user-123",
        nickname = "TestUser",
        totalRatings = 10,
        averageScore = 4.5,
        bestFountainId = "fountain-1",
        role = UserRole.OPERATOR
    )
    
    val testAdminUser = testUser.copy(
        id = "admin-123",
        nickname = "AdminUser",
        role = UserRole.ADMIN
    )
    
    // Fountains
    val testFountain = Fountain(
        codi = "fountain-1",
        nom = "Test Fountain",
        carrer = "Test Street",
        numeroCarrer = "123",
        latitude = 41.3851,
        longitude = 2.1734,
        averageRating = 4.2,
        totalReviews = 5,
        isDeleted = false
    )
    
    val testFountain2 = testFountain.copy(
        codi = "fountain-2",
        nom = "Second Fountain",
        averageRating = 3.8,
        totalReviews = 3
    )
    
    // Reviews
    val testReview = Review(
        id = "review-1",
        fountainId = "fountain-1",
        userId = "user-123",
        userNickname = "TestUser",
        taste = 4,
        freshness = 5,
        locationRating = 3,
        aesthetics = 4,
        splash = 3,
        jet = 5,
        overall = 4.0,
        comment = "Great fountain!",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    
    val testCreateReviewRequest = CreateReviewRequest(
        fountainId = "fountain-1",
        taste = 4,
        freshness = 5,
        locationRating = 3,
        aesthetics = 4,
        splash = 3,
        jet = 5,
        comment = "Great fountain!"
    )
    
    // User Stats
    val testUserStats = UserStats(
        totalRatings = 10,
        averageScore = 4.5,
        bestFountain = testFountain
    )
    
    // Leaderboard
    val testLeaderboardEntry = LeaderboardEntry(
        nickname = "TestUser",
        totalRatings = 10,
        averageScore = 4.5,
        rank = 1
    )
    
    val testLeaderboardEntries = listOf(
        testLeaderboardEntry,
        testLeaderboardEntry.copy(
            nickname = "User2",
            totalRatings = 8,
            averageScore = 4.2,
            rank = 2
        ),
        testLeaderboardEntry.copy(
            nickname = "User3",
            totalRatings = 6,
            averageScore = 4.0,
            rank = 3
        )
    )
    
    // Create Fountain Request
    val testCreateFountainRequest = CreateFountainRequest(
        nom = "New Fountain",
        carrer = "New Street",
        numeroCarrer = "456",
        latitude = 41.4,
        longitude = 2.2
    )
}
