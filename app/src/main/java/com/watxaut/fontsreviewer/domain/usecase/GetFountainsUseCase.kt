package com.watxaut.fontsreviewer.domain.usecase

import com.watxaut.fontsreviewer.domain.model.Fountain
import com.watxaut.fontsreviewer.domain.repository.FountainRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFountainsUseCase @Inject constructor(
    private val repository: FountainRepository
) {
    operator fun invoke(includeDeleted: Boolean = false): Flow<List<Fountain>> {
        return repository.getAllFountains(includeDeleted)
    }
}
