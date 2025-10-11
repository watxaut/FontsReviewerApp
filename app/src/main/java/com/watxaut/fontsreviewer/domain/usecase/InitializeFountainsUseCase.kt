package com.watxaut.fontsreviewer.domain.usecase

import com.watxaut.fontsreviewer.domain.repository.FountainRepository
import javax.inject.Inject

class InitializeFountainsUseCase @Inject constructor(
    private val repository: FountainRepository
) {
    suspend operator fun invoke() {
        repository.initializeFountains()
    }
}
