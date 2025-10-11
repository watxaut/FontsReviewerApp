package com.watxaut.fontsreviewer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FountainStatsDto(
    @SerialName("fountain_id")
    val fountainId: String,
    
    @SerialName("total_reviews")
    val totalReviews: Int,
    
    @SerialName("average_rating")
    val averageRating: Double
)
