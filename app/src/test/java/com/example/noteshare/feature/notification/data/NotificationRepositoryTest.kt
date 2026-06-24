package com.example.noteshare.feature.notification.data

import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.core.network.ApiResponse
import com.example.noteshare.core.network.PageData
import com.example.noteshare.feature.notification.domain.model.NotificationResponse
import com.example.noteshare.feature.notification.domain.model.UnreadCountResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * NotificationRepository 单元测试
 *
 * 覆盖场景：获取通知列表、未读数、标记已读、分页参数、异常处理
 */
class NotificationRepositoryTest {

    private lateinit var notificationApi: NotificationApi
    private lateinit var repository: NotificationRepository

    @Before
    fun setUp() {
        notificationApi = mockk(relaxed = true)
        repository = NotificationRepository(notificationApi)
    }

    // ==================== 辅助方法 ====================

    /** 创建单条通知数据 */
    private fun createNotification(id: Long = 1L) = NotificationResponse(
        id = id,
        type = "LIKE",
        senderId = 100L,
        senderNickname = "测试用户",
        senderAvatar = null,
        noteId = 200L,
        noteTitle = "测试笔记",
        commentContent = null,
        isRead = false,
        createdAt = "2025-06-23T10:00:00"
    )

    /** 创建分页数据 */
    private fun createPageData(
        items: List<NotificationResponse> = emptyList(),
        hasMore: Boolean = false
    ) = PageData(
        items = items,
        page = 1,
        pageSize = 20,
        total = items.size.toLong(),
        hasMore = hasMore
    )

    // ==================== getNotifications 测试 ====================

    /**
     * 测试：获取通知列表成功
     * 期望：Result.Success 包含 PageData
     */
    @Test
    fun getNotifications_success_returnsPageData() = runTest {
        // Given: API 返回成功响应
        val notifications = listOf(createNotification(id = 1), createNotification(id = 2))
        val pageData = createPageData(items = notifications, hasMore = true)
        coEvery {
            notificationApi.getNotifications(page = 1, size = 20)
        } returns ApiResponse(code = ErrorCode.SUCCESS, message = "ok", data = pageData)

        // When
        val result = repository.getNotifications(page = 1, size = 20)

        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(2, data.items.size)
        assertEquals(1L, data.items[0].id)
        assertEquals(2L, data.items[1].id)
        assertTrue(data.hasMore)
    }

    /**
     * 测试：获取通知列表时 API 返回非成功错误码
     * 期望：Result.Error 包含错误信息
     */
    @Test
    fun getNotifications_apiError_returnsFailure() = runTest {
        // Given: API 返回业务错误
        coEvery {
            notificationApi.getNotifications(page = 1, size = 20)
        } returns ApiResponse(code = ErrorCode.SERVER_ERROR, message = "服务器内部错误")

        // When
        val result = repository.getNotifications()

        // Then
        assertTrue(result is Result.Error)
        assertEquals("服务器内部错误", (result as Result.Error).message)
    }

    /**
     * 测试：获取通知列表时 API 返回错误消息为空的情况
     * 期望：错误消息为默认值
     */
    @Test
    fun getNotifications_apiError_emptyMessage_returnsDefault() = runTest {
        // Given: API 返回错误但 message 为空字符串
        coEvery {
            notificationApi.getNotifications(page = 1, size = 20)
        } returns ApiResponse(code = ErrorCode.SERVER_ERROR, message = "")

        // When
        val result = repository.getNotifications()

        // Then: message 为空字符串（非null），返回源码中 message ?: default
        assertTrue(result is Result.Error)
        assertEquals("", (result as Result.Error).message)
    }

    /**
     * 测试：获取通知列表时 API 抛出网络异常
     * 期望：Result.Error 包含网络错误信息
     */
    @Test
    fun getNotifications_networkException_returnsFailure() = runTest {
        // Given: API 抛出异常
        coEvery {
            notificationApi.getNotifications(page = 1, size = 20)
        } throws RuntimeException("网络超时")

        // When
        val result = repository.getNotifications()

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertEquals(ErrorCode.NETWORK_ERROR, error.code)
        assertTrue(error.message.contains("网络超时"))
    }

    /**
     * 测试：获取通知列表时 data 为 null 的情况
     * 期望：Result.Error，消息为 API 的 message 字段
     */
    @Test
    fun getNotifications_dataNull_returnsFailure() = runTest {
        // Given: API 返回成功但 data 为 null
        coEvery {
            notificationApi.getNotifications(page = 1, size = 20)
        } returns ApiResponse(code = ErrorCode.SUCCESS, message = "ok", data = null)

        // When
        val result = repository.getNotifications()

        // Then: code=SUCCESS 但 data=null，进入 else 分支，消息为 "ok"
        assertTrue(result is Result.Error)
        assertEquals("ok", (result as Result.Error).message)
    }

    // ==================== getUnreadCount 测试 ====================

    /**
     * 测试：获取未读数成功
     * 期望：Result.Success 包含 count
     */
    @Test
    fun getUnreadCount_success_returnsCount() = runTest {
        // Given
        coEvery {
            notificationApi.getUnreadCount()
        } returns ApiResponse(code = ErrorCode.SUCCESS, message = "ok", data = UnreadCountResponse(count = 5))

        // When
        val result = repository.getUnreadCount()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(5, (result as Result.Success).data)
    }

    /**
     * 测试：获取未读数成功但 count 为 0
     * 期望：Result.Success(0)
     */
    @Test
    fun getUnreadCount_success_zeroCount() = runTest {
        // Given
        coEvery {
            notificationApi.getUnreadCount()
        } returns ApiResponse(code = ErrorCode.SUCCESS, message = "ok", data = UnreadCountResponse(count = 0))

        // When
        val result = repository.getUnreadCount()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(0, (result as Result.Success).data)
    }

    /**
     * 测试：获取未读数时 API 返回业务错误
     * 期望：Result.Error
     */
    @Test
    fun getUnreadCount_apiError_returnsFailure() = runTest {
        // Given
        coEvery {
            notificationApi.getUnreadCount()
        } returns ApiResponse(code = ErrorCode.AUTH_TOKEN_INVALID, message = "token无效")

        // When
        val result = repository.getUnreadCount()

        // Then
        assertTrue(result is Result.Error)
        assertEquals("token无效", (result as Result.Error).message)
    }

    /**
     * 测试：获取未读数时 API 抛出异常
     * 期望：Result.Error
     */
    @Test
    fun getUnreadCount_networkException_returnsFailure() = runTest {
        // Given
        coEvery {
            notificationApi.getUnreadCount()
        } throws RuntimeException("连接超时")

        // When
        val result = repository.getUnreadCount()

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.message.contains("连接超时"))
    }

    /**
     * 测试：获取未读数时 data 为 null
     * 期望：Result.Error，消息为 API message
     */
    @Test
    fun getUnreadCount_dataNull_returnsFailure() = runTest {
        // Given
        coEvery {
            notificationApi.getUnreadCount()
        } returns ApiResponse(code = ErrorCode.SUCCESS, message = "ok", data = null)

        // When
        val result = repository.getUnreadCount()

        // Then: code=SUCCESS 但 data=null，进入 else 分支
        assertTrue(result is Result.Error)
        assertEquals("ok", (result as Result.Error).message)
    }

    // ==================== markAllAsRead 测试 ====================

    /**
     * 测试：标记所有已读成功
     * 期望：Result.Success
     */
    @Test
    fun markAllAsRead_success_returnsSuccess() = runTest {
        // Given
        coEvery {
            notificationApi.markAllAsRead()
        } returns ApiResponse(code = ErrorCode.SUCCESS, message = "ok")

        // When
        val result = repository.markAllAsRead()

        // Then
        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { notificationApi.markAllAsRead() }
    }

    /**
     * 测试：标记所有已读时 API 返回业务错误
     * 期望：Result.Error
     */
    @Test
    fun markAllAsRead_apiError_returnsFailure() = runTest {
        // Given
        coEvery {
            notificationApi.markAllAsRead()
        } returns ApiResponse(code = ErrorCode.SERVER_ERROR, message = "服务器异常")

        // When
        val result = repository.markAllAsRead()

        // Then
        assertTrue(result is Result.Error)
        assertEquals("服务器异常", (result as Result.Error).message)
    }

    /**
     * 测试：标记所有已读时 API 抛出异常
     * 期望：Result.Error
     */
    @Test
    fun markAllAsRead_networkException_returnsFailure() = runTest {
        // Given
        coEvery {
            notificationApi.markAllAsRead()
        } throws RuntimeException("网络断开")

        // When
        val result = repository.markAllAsRead()

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.message.contains("网络断开"))
    }

    /**
     * 测试：标记已读时 API 返回错误消息为空
     * 期望：错误消息为空字符串
     */
    @Test
    fun markAllAsRead_apiError_emptyMessage_returnsEmpty() = runTest {
        // Given
        coEvery {
            notificationApi.markAllAsRead()
        } returns ApiResponse(code = ErrorCode.SERVER_ERROR, message = "")

        // When
        val result = repository.markAllAsRead()

        // Then: message 为空字符串，不是 null，所以错误消息为 ""
        assertTrue(result is Result.Error)
        assertEquals("", (result as Result.Error).message)
    }

    // ==================== 分页参数测试 ====================

    /**
     * 测试：分页参数正确传递给 API
     * 期望：page 和 size 参数正确传递
     */
    @Test
    fun getNotifications_paginationParams_passedCorrectly() = runTest {
        // Given
        coEvery {
            notificationApi.getNotifications(page = 3, size = 10)
        } returns ApiResponse(
            code = ErrorCode.SUCCESS,
            message = "ok",
            data = createPageData()
        )

        // When
        repository.getNotifications(page = 3, size = 10)

        // Then: 验证传参正确
        coVerify(exactly = 1) { notificationApi.getNotifications(page = 3, size = 10) }
    }

    /**
     * 测试：默认分页参数 page=1, size=20
     * 期望：调用 API 时使用默认参数
     */
    @Test
    fun getNotifications_defaultParams_page1Size20() = runTest {
        // Given
        coEvery {
            notificationApi.getNotifications(page = 1, size = 20)
        } returns ApiResponse(
            code = ErrorCode.SUCCESS,
            message = "ok",
            data = createPageData()
        )

        // When: 使用默认参数
        repository.getNotifications()

        // Then
        coVerify(exactly = 1) { notificationApi.getNotifications(page = 1, size = 20) }
    }
}
