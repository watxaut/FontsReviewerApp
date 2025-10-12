package com.watxaut.fontsreviewer.domain.usecase

import com.watxaut.fontsreviewer.domain.repository.FountainRepository
import javax.inject.Inject

class CreateFountainUseCase @Inject constructor(
    private val repository: FountainRepository
) {
    suspend operator fun invoke(
        nom: String,
        carrer: String,
        numeroCarrer: String,
        latitude: Double,
        longitude: Double
    ): Result<String> {
        return repository.createFountain(
            nom = nom,
            carrer = carrer,
            numeroCarrer = numeroCarrer,
            latitude = latitude,
            longitude = longitude
        )
    }
}
