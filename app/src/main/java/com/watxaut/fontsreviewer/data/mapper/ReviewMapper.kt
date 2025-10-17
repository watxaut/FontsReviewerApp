package com.watxaut.fontsreviewer.data.mapper

import com.watxaut.fontsreviewer.data.remote.dto.CreateReviewDto
import com.watxaut.fontsreviewer.data.remote.dto.LeaderboardDto
import com.watxaut.fontsreviewer.data.remote.dto.ReviewDto
import com.watxaut.fontsreviewer.domain.model.CreateReviewRequest
import com.watxaut.fontsreviewer.domain.model.LeaderboardEntry
import com.watxaut.fontsreviewer.domain.model.Review
import com.watxaut.fontsreviewer.util.SecureLog
import java.time.Instant

fun ReviewDto.toDomain(): Review {
    return Review(
        id = id,
        fountainId = fountainId,
        userId = userId,
        userNickname = userNickname,
        taste = taste,
        freshness = freshness,
        locationRating = locationRating,
        aesthetics = aesthetics,
        splash = splash,
        jet = jet,
        overall = overall,
        comment = comment,
        createdAt = parseTimestamp(createdAt),
        updatedAt = parseTimestamp(updatedAt)
    )
}

fun CreateReviewRequest.toDto(userId: String, userNickname: String): CreateReviewDto {
    return CreateReviewDto(
        fountainId = fountainId,
        userId = userId,
        userNickname = userNickname,
        taste = taste,
        freshness = freshness,
        locationRating = locationRating,
        aesthetics = aesthetics,
        splash = splash,
        jet = jet,
        comment = comment
    )
}

fun LeaderboardDto.toDomain(): LeaderboardEntry {
    return LeaderboardEntry(
        nickname = nickname,
        totalRatings = totalRatings,
        averageScore = averageScore,
        rank = rank
    )
}

private fun parseTimestamp(timestamp: String): Long {
    return try {
        Instant.parse(timestamp).toEpochMilli()
    } catch (e: Exception) {
        SecureLog.e("ReviewMapper", "Failed to parse timestamp: '$timestamp'. Error: ${e.message}", e)
        // Return 0L instead of current time to avoid data corruption
        // This makes it obvious when timestamps fail to parse (shows as Jan 1, 1970)
        0L
    }
}
