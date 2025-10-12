package com.watxaut.fontsreviewer.domain.usecase

import com.watxaut.fontsreviewer.domain.repository.FountainRepository
import javax.inject.Inject

class SoftDeleteFountainUseCase @Inject constructor(
    private val fountainRepository: FountainRepository
) {
    suspend operator fun invoke(fountainId: String): Result<Unit> {
        return fountainRepository.softDeleteFountain(fountainId)
    }
}
