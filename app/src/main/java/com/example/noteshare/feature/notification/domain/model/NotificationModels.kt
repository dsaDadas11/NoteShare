package com.example.noteshare.feature.notification.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponse(
    val id: Long,
    val type: String,
    val senderId: Long,
    val senderNickname: String? = null,
    val senderAvatar: String? = null,
    val noteId: Long,
    val noteTitle: String? = null,
    val commentContent: String? = null,
    val isRead: Boolean = false,
    val createdAt: String
)

@Serializable
data class UnreadCountResponse(val count: Int = 0)

@Serializable
data class NotificationPush(
    val notificationId: Long? = null,
    val type: String,
    val senderId: Long,
    val senderNickname: String? = null,
    val senderAvatar: String? = null,
    val noteId: Long,
    val noteTitle: String? = null,
    val commentContent: String? = null,
    val createdAt: String
)
