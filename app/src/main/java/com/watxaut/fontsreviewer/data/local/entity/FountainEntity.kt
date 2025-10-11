package com.watxaut.fontsreviewer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fountains")
data class FountainEntity(
    @PrimaryKey
    val codi: String,
    val nom: String,
    val carrer: String,
    val numeroCarrer: String,
    val latitude: Double,
    val longitude: Double
)
