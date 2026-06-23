package com.example.noteshare.feature.feed.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UserBrief(
    val id: Long,
    val username: String,
    val nickname: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class ImageInfo(
    val id: Long,
    val url: String,
    val sort: Int
)

@Serializable
data class NoteResponse(
    val id: Long,
    val title: String,
    val content: String,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val createdAt: String,
    val author: UserBrief,
    val images: List<ImageInfo> = emptyList(),
    val videoUrl: String? = null
)

@Serializable
data class NoteDetailResponse(
    val id: Long,
    val title: String,
    val content: String,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    @SerialName("liked") val isLiked: Boolean = false,
    @SerialName("authorFollowed") val isAuthorFollowed: Boolean = false,
    @SerialName("authorSelf") val isAuthorSelf: Boolean = false,
    val createdAt: String,
    val author: UserBrief,
    val images: List<ImageInfo> = emptyList(),
    val videoUrl: String? = null
)

@Serializable
data class CommentResponse(
    val id: Long,
    val content: String,
    val createdAt: String,
    @SerialName("author") val user: UserBrief,
    @SerialName("mine") val isMine: Boolean = false,
    val parentId: Long? = null,
    val likeCount: Int = 0,
    val liked: Boolean = false,
    val replyCount: Int = 0,
    val replies: List<CommentResponse> = emptyList(),
    /** 回复目标作者昵称（楼中楼展示用） */
    val replyToAuthor: String? = null
)

@Serializable
data class CreateCommentRequest(
    val content: String,
    val parentId: Long? = null,
    /** 回复目标作者昵称（楼中楼展示用） */
    val replyToAuthor: String? = null
)


@Serializable
data class CreateNoteRequest(
    val title: String,
    val content: String,
    val imageUrls: List<String>,
    val videoUrl: String? = null
)
