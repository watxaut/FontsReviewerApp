package com.watxaut.fontsreviewer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReviewDto(
    @SerialName("id")
    val id: String,

    @SerialName("fountain_id")
    val fountainId: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("user_nickname")
    val userNickname: String,

    @SerialName("taste")
    val taste: Int,

    @SerialName("freshness")
    val freshness: Int,

    @SerialName("location_rating")
    val locationRating: Int,

    @SerialName("aesthetics")
    val aesthetics: Int,

    @SerialName("splash")
    val splash: Int,

    @SerialName("jet")
    val jet: Int,

    @SerialName("overall")
    val overall: Double,

    @SerialName("comment")
    val comment: String? = null,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String
)

@Serializable
data class CreateReviewDto(
    @SerialName("fountain_id")
    val fountainId: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("user_nickname")
    val userNickname: String,

    @SerialName("taste")
    val taste: Int,

    @SerialName("freshness")
    val freshness: Int,

    @SerialName("location_rating")
    val locationRating: Int,

    @SerialName("aesthetics")
    val aesthetics: Int,

    @SerialName("splash")
    val splash: Int,

    @SerialName("jet")
    val jet: Int,

    @SerialName("comment")
    val comment: String? = null
)

@Serializable
data class LeaderboardDto(
    @SerialName("nickname")
    val nickname: String,

    @SerialName("total_ratings")
    val totalRatings: Int,

    @SerialName("average_score")
    val averageScore: Double,

    @SerialName("rank")
    val rank: Int
)
