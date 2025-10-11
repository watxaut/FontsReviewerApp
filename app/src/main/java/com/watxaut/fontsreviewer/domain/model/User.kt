package com.watxaut.fontsreviewer.domain.model

data class User(
    val id: String,
    val nickname: String,
    val totalRatings: Int = 0,
    val averageScore: Double = 0.0,
    val bestFountainId: String? = null
)

data class UserStats(
    val totalRatings: Int,
    val averageScore: Double,
    val bestFountain: Fountain?
)

data class LeaderboardEntry(
    val nickname: String,
    val totalRatings: Int,
    val averageScore: Double,
    val rank: Int
)
