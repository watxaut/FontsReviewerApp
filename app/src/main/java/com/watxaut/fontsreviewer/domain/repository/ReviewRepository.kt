package com.watxaut.fontsreviewer.domain.repository

import com.watxaut.fontsreviewer.domain.model.CreateReviewRequest
import com.watxaut.fontsreviewer.domain.model.LeaderboardEntry
import com.watxaut.fontsreviewer.domain.model.Review
import com.watxaut.fontsreviewer.domain.model.UserStats

interface ReviewRepository {
    suspend fun submitReview(request: CreateReviewRequest): Result<Review>
    suspend fun getReviewsForFountain(fountainId: String): Result<List<Review>>
    suspend fun getUserReview(userId: String, fountainId: String): Result<Review?>
    suspend fun updateReview(reviewId: String, request: CreateReviewRequest): Result<Review>
    suspend fun deleteReview(reviewId: String): Result<Unit>
    suspend fun getUserStats(userId: String): Result<UserStats>
    suspend fun getLeaderboard(limit: Int = 100): Result<List<LeaderboardEntry>>
}
