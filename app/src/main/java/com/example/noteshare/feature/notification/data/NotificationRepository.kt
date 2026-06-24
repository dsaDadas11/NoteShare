package com.example.noteshare.feature.notification.data

import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.core.network.PageData
import com.example.noteshare.feature.notification.domain.model.NotificationResponse
import com.example.noteshare.feature.notification.domain.model.UnreadCountResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationApi: NotificationApi
) {
    suspend fun getNotifications(page: Int = 1, size: Int = 20): Result<PageData<NotificationResponse>> {
        return try {
            val response = notificationApi.getNotifications(page, size)
            if (response.code == ErrorCode.SUCCESS && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.code, response.message ?: "获取通知失败")
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }

    suspend fun getUnreadCount(): Result<Int> {
        return try {
            val response = notificationApi.getUnreadCount()
            if (response.code == ErrorCode.SUCCESS && response.data != null) {
                Result.Success(response.data.count)
            } else {
                Result.Error(response.code, response.message ?: "获取未读数失败")
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }

    suspend fun markAllAsRead(): Result<Unit> {
        return try {
            val response = notificationApi.markAllAsRead()
            if (response.code == ErrorCode.SUCCESS) {
                Result.Success(Unit)
            } else {
                Result.Error(response.code, response.message ?: "标记已读失败")
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }
}
