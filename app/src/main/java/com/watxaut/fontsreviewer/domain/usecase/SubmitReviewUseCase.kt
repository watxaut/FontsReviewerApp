package com.watxaut.fontsreviewer.domain.usecase

import com.watxaut.fontsreviewer.domain.model.CreateReviewRequest
import com.watxaut.fontsreviewer.domain.model.Review
import com.watxaut.fontsreviewer.domain.repository.ReviewRepository
import javax.inject.Inject

class SubmitReviewUseCase @Inject constructor(
    private val repository: ReviewRepository
) {
    suspend operator fun invoke(request: CreateReviewRequest): Result<Review> {
        return repository.submitReview(request)
    }
}
