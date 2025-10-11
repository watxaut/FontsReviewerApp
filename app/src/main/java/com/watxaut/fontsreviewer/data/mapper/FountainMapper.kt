package com.watxaut.fontsreviewer.data.mapper

import com.watxaut.fontsreviewer.data.local.entity.FountainEntity
import com.watxaut.fontsreviewer.domain.model.Fountain

fun FountainEntity.toDomain(): Fountain {
    return Fountain(
        codi = codi,
        nom = nom,
        carrer = carrer,
        numeroCarrer = numeroCarrer,
        latitude = latitude,
        longitude = longitude
    )
}

fun Fountain.toEntity(): FountainEntity {
    return FountainEntity(
        codi = codi,
        nom = nom,
        carrer = carrer,
        numeroCarrer = numeroCarrer,
        latitude = latitude,
        longitude = longitude
    )
}
