package com.watxaut.fontsreviewer.domain.model

data class Review(
    val id: String,
    val fountainId: String,
    val userId: String,
    val userNickname: String,
    val taste: Int,           // 1-5
    val freshness: Int,       // 1-5
    val locationRating: Int,  // 1-5
    val aesthetics: Int,      // 1-5
    val splash: Int,          // 1-5
    val jet: Int,             // 1-5
    val overall: Double,      // Average of 6 ratings
    val comment: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

data class CreateReviewRequest(
    val fountainId: String,
    val taste: Int,
    val freshness: Int,
    val locationRating: Int,
    val aesthetics: Int,
    val splash: Int,
    val jet: Int,
    val comment: String? = null
) {
    init {
        require(taste in 1..5) { "Taste must be between 1 and 5" }
        require(freshness in 1..5) { "Freshness must be between 1 and 5" }
        require(locationRating in 1..5) { "Location rating must be between 1 and 5" }
        require(aesthetics in 1..5) { "Aesthetics must be between 1 and 5" }
        require(splash in 1..5) { "Splash must be between 1 and 5" }
        require(jet in 1..5) { "Jet must be between 1 and 5" }
    }

    val overall: Double
        get() = (taste + freshness + locationRating + aesthetics + splash + jet) / 6.0
}
