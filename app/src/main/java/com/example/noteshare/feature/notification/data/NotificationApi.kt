package com.example.noteshare.feature.notification.data

import com.example.noteshare.core.network.ApiResponse
import com.example.noteshare.core.network.PageData
import com.example.noteshare.feature.notification.domain.model.NotificationResponse
import com.example.noteshare.feature.notification.domain.model.UnreadCountResponse
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

interface NotificationApi {

    @GET("/api/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResponse<PageData<NotificationResponse>>

    @GET("/api/notifications/unread-count")
    suspend fun getUnreadCount(): ApiResponse<UnreadCountResponse>

    @PUT("/api/notifications/read-all")
    suspend fun markAllAsRead(): ApiResponse<Unit>
}
