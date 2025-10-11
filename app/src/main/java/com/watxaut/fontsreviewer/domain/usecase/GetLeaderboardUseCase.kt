package com.watxaut.fontsreviewer.domain.usecase

import com.watxaut.fontsreviewer.domain.model.LeaderboardEntry
import com.watxaut.fontsreviewer.domain.repository.ReviewRepository
import javax.inject.Inject

class GetLeaderboardUseCase @Inject constructor(
    private val repository: ReviewRepository
) {
    suspend operator fun invoke(): Result<List<LeaderboardEntry>> {
        return repository.getLeaderboard()
    }
}
