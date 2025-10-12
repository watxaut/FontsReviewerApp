package com.watxaut.fontsreviewer.data.repository

import com.watxaut.fontsreviewer.data.mapper.toDomain
import com.watxaut.fontsreviewer.data.mapper.toDto
import com.watxaut.fontsreviewer.data.remote.service.SupabaseService
import com.watxaut.fontsreviewer.domain.model.CreateReviewRequest
import com.watxaut.fontsreviewer.domain.model.LeaderboardEntry
import com.watxaut.fontsreviewer.domain.model.Review
import com.watxaut.fontsreviewer.domain.model.UserStats
import com.watxaut.fontsreviewer.domain.repository.FountainRepository
import com.watxaut.fontsreviewer.domain.repository.ReviewRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val supabaseService: SupabaseService,
    private val fountainRepository: FountainRepository
) : ReviewRepository {

    override suspend fun submitReview(request: CreateReviewRequest): Result<Review> {
        return try {
            // Get current user ID
            val userId = supabaseService.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            // Get user profile to get nickname
            val profileResult = supabaseService.getProfile(userId)
            if (profileResult.isFailure) {
                return Result.failure(Exception("Failed to get user profile"))
            }

            val profile = profileResult.getOrNull()
                ?: return Result.failure(Exception("User profile not found"))

            // Create review DTO
            val reviewDto = request.toDto(userId, profile.nickname)

            // Submit to Supabase
            val result = supabaseService.createReview(reviewDto)

            if (result.isSuccess) {
                val reviewDto = result.getOrNull()!!
                Result.success(reviewDto.toDomain())
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to submit review"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReviewsForFountain(fountainId: String): Result<List<Review>> {
        return try {
            val result = supabaseService.getReviewsForFountain(fountainId)

            if (result.isSuccess) {
                val reviews = result.getOrNull()?.map { it.toDomain() } ?: emptyList()
                Result.success(reviews)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to get reviews"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserReview(userId: String, fountainId: String): Result<Review?> {
        return try {
            val result = supabaseService.getUserReview(userId, fountainId)

            if (result.isSuccess) {
                val review = result.getOrNull()?.toDomain()
                Result.success(review)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to get user review"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateReview(reviewId: String, request: CreateReviewRequest): Result<Review> {
        return try {
            // Get current user ID
            val userId = supabaseService.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            // Get user profile to get nickname
            val profileResult = supabaseService.getProfile(userId)
            if (profileResult.isFailure) {
                return Result.failure(Exception("Failed to get user profile"))
            }

            val profile = profileResult.getOrNull()
                ?: return Result.failure(Exception("User profile not found"))

            // Create review DTO
            val reviewDto = request.toDto(userId, profile.nickname)

            // Update in Supabase
            val result = supabaseService.updateReview(reviewId, reviewDto)

            if (result.isSuccess) {
                val reviewDto = result.getOrNull()!!
                Result.success(reviewDto.toDomain())
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to update review"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteReview(reviewId: String): Result<Unit> {
        return try {
            supabaseService.deleteReview(reviewId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserStats(userId: String): Result<UserStats> {
        return try {
            val profileResult = supabaseService.getProfile(userId)
            if (profileResult.isFailure) {
                return Result.failure(Exception("Failed to get user profile"))
            }

            val profile = profileResult.getOrNull()
                ?: return Result.failure(Exception("User profile not found"))

            // Get best fountain if exists
            val bestFountain = profile.bestFountainId?.let {
                fountainRepository.getFountainByCodi(it)
            }

            val stats = UserStats(
                totalRatings = profile.totalRatings,
                averageScore = profile.averageScore,
                bestFountain = bestFountain
            )

            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLeaderboard(limit: Int): Result<List<LeaderboardEntry>> {
        return try {
            val result = supabaseService.getLeaderboard(limit)

            if (result.isSuccess) {
                val leaderboard = result.getOrNull()?.map { it.toDomain() } ?: emptyList()
                Result.success(leaderboard)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to get leaderboard"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserReviewedFountainIds(userId: String): Result<Set<String>> {
        return try {
            val result = supabaseService.getUserReviewedFountainIds(userId)
            
            if (result.isSuccess) {
                val fountainIds = result.getOrNull()?.toSet() ?: emptySet()
                Result.success(fountainIds)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to get reviewed fountain IDs"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
