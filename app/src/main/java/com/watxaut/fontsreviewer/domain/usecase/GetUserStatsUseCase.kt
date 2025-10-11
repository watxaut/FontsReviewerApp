package com.watxaut.fontsreviewer.domain.usecase

import com.watxaut.fontsreviewer.domain.model.UserStats
import com.watxaut.fontsreviewer.domain.repository.ReviewRepository
import javax.inject.Inject

class GetUserStatsUseCase @Inject constructor(
    private val repository: ReviewRepository
) {
    suspend operator fun invoke(userId: String): Result<UserStats> {
        return repository.getUserStats(userId)
    }
}
