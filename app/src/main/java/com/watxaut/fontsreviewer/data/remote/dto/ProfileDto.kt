package com.watxaut.fontsreviewer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    @SerialName("id")
    val id: String,

    @SerialName("nickname")
    val nickname: String,

    @SerialName("total_ratings")
    val totalRatings: Int = 0,

    @SerialName("average_score")
    val averageScore: Double = 0.0,

    @SerialName("best_fountain_id")
    val bestFountainId: String? = null
)

@Serializable
data class CreateProfileDto(
    @SerialName("id")
    val id: String,

    @SerialName("nickname")
    val nickname: String
)
