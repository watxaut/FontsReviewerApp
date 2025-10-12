package com.watxaut.fontsreviewer.domain.usecase

import com.watxaut.fontsreviewer.domain.model.CreateFountainRequest
import com.watxaut.fontsreviewer.domain.repository.FountainRepository
import javax.inject.Inject

class CreateFountainUseCase @Inject constructor(
    private val fountainRepository: FountainRepository
) {
    suspend operator fun invoke(fountain: CreateFountainRequest): Result<String> {
        return fountainRepository.createFountain(fountain)
    }
}
