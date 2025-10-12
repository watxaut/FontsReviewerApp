package com.watxaut.fontsreviewer.data.mapper

import com.watxaut.fontsreviewer.data.remote.dto.FountainWithStatsDto
import com.watxaut.fontsreviewer.domain.model.Fountain

fun FountainWithStatsDto.toDomain(): Fountain {
    return Fountain(
        codi = codi,
        nom = nom,
        carrer = carrer,
        numeroCarrer = numeroCarrer,
        latitude = latitude,
        longitude = longitude,
        averageRating = averageRating,
        totalReviews = totalReviews,
        isDeleted = isDeleted
    )
}
