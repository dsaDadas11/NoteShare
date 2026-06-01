package com.example.noteshare.feature.profile.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UserProfileResponse(
    val id: Long,
    val username: String,
    val nickname: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val noteCount: Int = 0,
    @SerialName("followed") val isFollowing: Boolean? = null
)

@Serializable
data class UpdateProfileRequest(
    val nickname: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null
)
