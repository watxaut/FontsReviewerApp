package com.watxaut.fontsreviewer.domain.model

data class Fountain(
    val codi: String,
    val nom: String,
    val carrer: String,
    val numeroCarrer: String,
    val latitude: Double,
    val longitude: Double,
    val averageRating: Double = 0.0,
    val totalReviews: Int = 0,
    val isDeleted: Boolean = false
)

data class CreateFountainRequest(
    val nom: String,
    val carrer: String,
    val numeroCarrer: String,
    val latitude: Double,
    val longitude: Double
)
