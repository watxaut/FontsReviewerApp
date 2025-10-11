package com.watxaut.fontsreviewer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FountainDto(
    @SerialName("codi")
    val codi: String,
    
    @SerialName("nom")
    val nom: String,
    
    @SerialName("carrer")
    val carrer: String,
    
    @SerialName("numero_carrer")
    val numeroCarrer: String,
    
    @SerialName("latitude")
    val latitude: Double,
    
    @SerialName("longitude")
    val longitude: Double
)

@Serializable
data class FountainWithStatsDto(
    @SerialName("codi")
    val codi: String,
    
    @SerialName("nom")
    val nom: String,
    
    @SerialName("carrer")
    val carrer: String,
    
    @SerialName("numero_carrer")
    val numeroCarrer: String,
    
    @SerialName("latitude")
    val latitude: Double,
    
    @SerialName("longitude")
    val longitude: Double,
    
    @SerialName("total_reviews")
    val totalReviews: Int = 0,
    
    @SerialName("average_rating")
    val averageRating: Double = 0.0
)
