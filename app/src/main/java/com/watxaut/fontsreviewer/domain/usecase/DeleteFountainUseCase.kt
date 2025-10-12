package com.watxaut.fontsreviewer.domain.usecase

import com.watxaut.fontsreviewer.domain.repository.FountainRepository
import javax.inject.Inject

class DeleteFountainUseCase @Inject constructor(
    private val repository: FountainRepository
) {
    suspend operator fun invoke(fountainId: String): Result<Unit> {
        return repository.softDeleteFountain(fountainId)
    }
}
