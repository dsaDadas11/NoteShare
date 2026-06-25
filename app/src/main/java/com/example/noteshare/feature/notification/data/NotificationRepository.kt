package com.example.noteshare.feature.notification.data

import com.example.noteshare.core.common.Result
import com.example.noteshare.core.common.safeApiCall
import com.example.noteshare.core.common.safeApiCallUnit
import com.example.noteshare.core.network.PageData
import com.example.noteshare.feature.notification.domain.model.NotificationResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationApi: NotificationApi
) {
    suspend fun getNotifications(page: Int = 1, size: Int = 20): Result<PageData<NotificationResponse>> =
        safeApiCall("获取通知失败") { notificationApi.getNotifications(page, size) }

    suspend fun getUnreadCount(): Result<Int> {
        return when (val result = safeApiCall("获取未读数失败") { notificationApi.getUnreadCount() }) {
            is Result.Success -> Result.Success(result.data.count)
            is Result.Error -> result
        }
    }

    suspend fun markAllAsRead(): Result<Unit> =
        safeApiCallUnit("标记已读失败") { notificationApi.markAllAsRead() }
}
