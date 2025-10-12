package com.watxaut.fontsreviewer.domain.usecase

import com.watxaut.fontsreviewer.domain.repository.ReviewRepository
import javax.inject.Inject

/**
 * Use case to get all fountain IDs that a user has reviewed.
 * 
 * This follows Clean Architecture principles by abstracting the
 * data layer from the presentation layer through the repository pattern.
 */
class GetUserReviewedFountainsUseCase @Inject constructor(
    private val reviewRepository: ReviewRepository
) {
    /**
     * Get the set of fountain IDs that the user has reviewed.
     * 
     * @param userId The ID of the user
     * @return Result containing a Set of fountain IDs, or an error
     */
    suspend operator fun invoke(userId: String): Result<Set<String>> {
        return reviewRepository.getUserReviewedFountainIds(userId)
    }
}
