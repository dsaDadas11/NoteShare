package com.example.noteshare.feature.notification.presentation

import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.network.PageData
import com.example.noteshare.feature.notification.data.NotificationRepository
import com.example.noteshare.feature.notification.domain.model.NotificationResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * NotificationViewModel 单元测试
 *
 * 覆盖场景：初始加载、下拉刷新、加载更多、空列表、标记已读、通知类型、未读数、分页边界
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: NotificationRepository
    private lateinit var viewModel: NotificationViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== 辅助方法 ====================

    /** 创建一条通知数据 */
    private fun createNotification(
        id: Long = 1L,
        type: String = "LIKE",
        senderId: Long = 100L,
        noteId: Long = 200L,
        isRead: Boolean = false
    ) = NotificationResponse(
        id = id,
        type = type,
        senderId = senderId,
        senderNickname = "测试用户",
        senderAvatar = null,
        noteId = noteId,
        noteTitle = "测试笔记",
        commentContent = null,
        isRead = isRead,
        createdAt = "2025-06-23T10:00:00"
    )

    /** 创建分页数据 */
    private fun createPageData(
        items: List<NotificationResponse> = emptyList(),
        page: Int = 1,
        hasMore: Boolean = false
    ) = PageData(
        items = items,
        page = page,
        pageSize = 20,
        total = items.size.toLong(),
        hasMore = hasMore
    )

    // ==================== 初始加载测试 ====================

    /**
     * 测试：ViewModel 初始化时自动加载通知列表
     * 期望：isLoading 为 false，notifications 有数据，currentPage 为 1
     */
    @Test
    fun init_loadsNotifications_success() = runTest {
        // Given: repository 返回成功数据
        val notifications = listOf(createNotification(id = 1), createNotification(id = 2))
        coEvery {
            repository.getNotifications(page = 1)
        } returns Result.success(createPageData(items = notifications, hasMore = false))
        coEvery { repository.markAllAsRead() } returns Result.success(Unit)

        // When: 创建 ViewModel（触发 init）
        viewModel = NotificationViewModel(repository)
        advanceUntilIdle()

        // Then: UI 状态包含通知列表
        val state = viewModel.uiState.value
        assertEquals(2, state.notifications.size)
        assertEquals(1, state.currentPage)
        assertFalse(state.isLoading)
        assertFalse(state.hasMore)
        assertNull(state.error)
    }

    /**
     * 测试：初始加载失败时显示错误信息
     * 期望：error 字段有值，isLoading 为 false
     */
    @Test
    fun init_loadsNotifications_failure_showsError() = runTest {
        // Given: repository 返回失败
        coEvery {
            repository.getNotifications(page = 1)
        } returns Result.failure(Exception("网络连接失败"))
        coEvery { repository.markAllAsRead() } returns Result.success(Unit)

        // When: 创建 ViewModel
        viewModel = NotificationViewModel(repository)
        advanceUntilIdle()

        // Then: UI 状态有错误信息
        val state = viewModel.uiState.value
        assertEquals("网络连接失败", state.error)
        assertTrue(state.notifications.isEmpty())
        assertFalse(state.isLoading)
    }

    // ==================== 下拉刷新测试 ====================

    /**
     * 测试：下拉刷新成功
     * 期望：通知列表被替换为新数据
     */
    @Test
    fun loadNotifications_refresh_success_replacesNotifications() = runTest {
        // Given: 初始加载返回旧数据
        val oldNotifications = listOf(createNotification(id = 1))
        coEvery {
            repository.getNotifications(page = 1)
        } returnsMany listOf(
            Result.success(createPageData(items = oldNotifications, hasMore = false)),
            // 第二次调用（刷新）
            Result.success(createPageData(items = listOf(createNotification(id = 3)), hasMore = false))
        )
        coEvery { repository.markAllAsRead() } returns Result.success(Unit)

        // When: 创建 ViewModel 并触发刷新
        viewModel = NotificationViewModel(repository)
        advanceUntilIdle()
        viewModel.loadNotifications()
        advanceUntilIdle()

        // Then: 通知列表更新为新数据
        val state = viewModel.uiState.value
        assertEquals(1, state.notifications.size)
        assertEquals(3L, state.notifications[0].id)
        assertFalse(state.isLoading)
    }

    /**
     * 测试：下拉刷新失败时保留旧数据
     * 期望：error 有值，旧通知不被清除
     */
    @Test
    fun loadNotifications_refresh_failure_keepsOldData() = runTest {
        // Given: 初始加载成功
        val oldNotifications = listOf(createNotification(id = 1))
        coEvery {
            repository.getNotifications(page = 1)
        } returnsMany listOf(
            Result.success(createPageData(items = oldNotifications, hasMore = false)),
            // 第二次调用（刷新）失败
            Result.failure(Exception("服务器异常"))
        )
        coEvery { repository.markAllAsRead() } returns Result.success(Unit)

        // When: 创建 ViewModel 并触发刷新
        viewModel = NotificationViewModel(repository)
        advanceUntilIdle()
        viewModel.loadNotifications()
        advanceUntilIdle()

        // Then: 旧数据保留，error 有值
        val state = viewModel.uiState.value
        assertEquals(1, state.notifications.size)
        assertEquals(1L, state.notifications[0].id)
        assertEquals("服务器异常", state.error)
    }

    /**
     * 测试：正在加载时重复调用 loadNotifications 不会重复请求
     * 期望：repository 只被调用一次
     */
    @Test
    fun loadNotifications_alreadyLoading_doesNotRetry() = runTest {
        // Given: repository 慢返回（不会完成）
        coEvery {
            repository.getNotifications(page = 1)
        } coAnswers {
            kotlinx.coroutines.delay(10_000)
            Result.success(createPageData(items = listOf(createNotification()), hasMore = false))
        }
        coEvery { repository.markAllAsRead() } returns Result.success(Unit)

        // When: 创建 ViewModel（init 调用 loadNotifications）
        viewModel = NotificationViewModel(repository)
        // 推进调度器让 init 中的 launch 协程开始执行（isLoading 变为 true）
        testDispatcher.scheduler.advanceTimeBy(1)
        // 此时 isLoading = true，再次调用应被跳过
        viewModel.loadNotifications()

        // Then: repository 只被调用 1 次（init 的调用）
        coVerify(exactly = 1) { repository.getNotifications(page = 1) }
    }

    // ==================== 加载更多测试 ====================

    /**
     * 测试：加载更多成功
     * 期望：新通知追加到列表后面，currentPage 递增
     */
    @Test
    fun loadMore_success_appendsNotifications() = runTest {
        // Given: 初始加载有下一页，加载更多也有下一页
        coEvery {
            repository.getNotifications(page = 1)
        } returns Result.success(createPageData(
            items = listOf(createNotification(id = 1)),
            hasMore = true
        ))
        coEvery {
            repository.getNotifications(page = 2)
        } returns Result.success(createPageData(
            items = listOf(createNotification(id = 10)),
            page = 2,
            hasMore = false
        ))
        coEvery { repository.markAllAsRead() } returns Result.success(Unit)

        // When: 初始化并加载更多
        viewModel = NotificationViewModel(repository)
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        // Then: 两条通知都在列表中
        val state = viewModel.uiState.value
        assertEquals(2, state.notifications.size)
        assertEquals(1L, state.notifications[0].id)
        assertEquals(10L, state.notifications[1].id)
        assertEquals(2, state.currentPage)
        assertFalse(state.hasMore)
    }

    /**
     * 测试：加载更多失败时 isLoadingMore 恢复为 false
     * 期望：旧列表不变，isLoadingMore 为 false
     */
    @Test
    fun loadMore_failure_resetsLoadingMore() = runTest {
        // Given: 初始加载成功（hasMore = true）
        coEvery {
            repository.getNotifications(page = 1)
        } returns Result.success(createPageData(
            items = listOf(createNotification(id = 1)),
            hasMore = true
        ))
        coEvery {
            repository.getNotifications(page = 2)
        } returns Result.failure(Exception("加载失败"))
        coEvery { repository.markAllAsRead() } returns Result.success(Unit)

        // When: 初始化并加载更多
        viewModel = NotificationViewModel(repository)
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        // Then: 旧数据保留，isLoadingMore 恢复
        val state = viewModel.uiState.value
        assertEquals(1, state.notifications.size)
        assertFalse(state.isLoadingMore)
    }

    /**
     * 测试：hasMore 为 false 时 loadMore 不会触发请求
     * 期望：repository 不会被调用第二次
     */
    @Test
    fun loadMore_noMoreData_doesNotRequest() = runTest {
        // Given: 初始加载 hasMore = false
        coEvery {
            repository.getNotifications(page = 1)
        } returns Result.success(createPageData(
            items = listOf(createNotification(id = 1)),
            hasMore = false
        ))
        coEvery { repository.markAllAsRead() } returns Result.success(Unit)

        // When: 初始化后调用 loadMore
        viewModel = NotificationViewModel(repository)
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        // Then: loadMore 被跳过，只调用 1 次
        coVerify(exactly = 1) { repository.getNotifications(page = 1) }
    }

    // ==================== 空列表测试 ====================

    /**
     * 测试：服务端返回空通知列表
     * 期望：notifications 为空，hasMore 为 false
     */
    @Test
    fun loadNotifications_emptyList_returnsEmptyState() = runTest {
        // Given: 服务端返回空列表
        coEvery {
            repository.getNotifications(page = 1)
        } returns Result.success(createPageData(items = emptyList(), hasMore = false))
        coEvery { repository.markAllAsRead() } returns Result.success(Unit)

        // When: 创建 ViewModel
        viewModel = NotificationViewModel(repository)
        advanceUntilIdle()

        // Then: UI 状态为空列表
        val state = viewModel.uiState.value
        assertTrue(state.notifications.isEmpty())
        assertFalse(state.hasMore)
        assertFalse(state.isLoading)
    }

    // ==================== 标记所有已读测试 ====================

    /**
     * 测试：初始化时自动调用 markAllAsRead
     * 期望：markAllAsRead 被调用一次
     */
    @Test
    fun init_markAllAsRead_calledOnce() = runTest {
        // Given
        coEvery {
            repository.getNotifications(page = 1)
        } returns Result.success(createPageData(hasMore = false))
        coEvery { repository.markAllAsRead() } returns Result.success(Unit)

        // When
        viewModel = NotificationViewModel(repository)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.markAllAsRead() }
    }

    /**
     * 测试：markAllAsRead 失败不影响 UI 状态
     * 期望：即使标记已读失败，UI 不受影响
     */
    @Test
    fun init_markAllAsRead_failure_doesNotAffectUi() = runTest {
        // Given: 标记已读失败
        coEvery {
            repository.getNotifications(page = 1)
        } returns Result.success(createPageData(items = listOf(createNotification()), hasMore = false))
        coEvery { repository.markAllAsRead() } returns Result.failure(Exception("服务器错误"))

        // When: 创建 ViewModel
        viewModel = NotificationViewModel(repository)
        advanceUntilIdle()

        // Then: UI 正常（markAllAsRead 的错误被静默处理）
        val state = viewModel.uiState.value
        assertEquals(1, state.notifications.size)
        assertNull(state.error)
    }

    // ==================== 通知类型处理测试 ====================

    /**
     * 测试：LIKE 类型通知正确显示
     * 期望：type 字段正确保留
     */
    @Test
    fun loadNotifications_likeType_correctlyParsed() = runTest {
        // Given
        coEvery {
            repository.getNotifications(page = 1)
        } returns Result.success(createPageData(
            items = listOf(createNotification(type = "LIKE")),
            hasMore = false
        ))
        coEvery { repository.markAllAsRead() } returns Result.success(Unit)

        // When
        viewModel = NotificationViewModel(repository)
        advanceUntilIdle()

        // Then
        assertEquals("LIKE", viewModel.uiState.value.notifications[0].type)
    }

    /**
     * 测试：COMMENT 类型通知包含评论内容
     * 期望：commentContent 字段有值
     */
    @Test
    fun loadNotifications_commentType_hasContent() = runTest {
        // Given
        val commentNotification = createNotification(type = "COMMENT").copy(
            commentContent = "这是一条评论内容"
        )
        coEvery {
            repository.getNotifications(page = 1)
        } returns Result.success(createPageData(
            items = listOf(commentNotification),
            hasMore = false
        ))
        coEvery { repository.markAllAsRead() } returns Result.success(Unit)

        // When
        viewModel = NotificationViewModel(repository)
        advanceUntilIdle()

        // Then
        assertEquals("COMMENT", viewModel.uiState.value.notifications[0].type)
        assertEquals("这是一条评论内容", viewModel.uiState.value.notifications[0].commentContent)
    }

    /**
     * 测试：FOLLOW 类型通知正确处理
     * 期望：type 为 FOLLOW
     */
    @Test
    fun loadNotifications_followType_correctlyParsed() = runTest {
        // Given
        coEvery {
            repository.getNotifications(page = 1)
        } returns Result.success(createPageData(
            items = listOf(createNotification(type = "FOLLOW")),
            hasMore = false
        ))
        coEvery { repository.markAllAsRead() } returns Result.success(Unit)

        // When
        viewModel = NotificationViewModel(repository)
        advanceUntilIdle()

        // Then
        assertEquals("FOLLOW", viewModel.uiState.value.notifications[0].type)
    }

    // ==================== errorShown 测试 ====================

    /**
     * 测试：errorShown 清除错误信息
     * 期望：error 变为 null
     */
    @Test
    fun errorShown_clearsError() = runTest {
        // Given: 先产生一个错误
        coEvery {
            repository.getNotifications(page = 1)
        } returns Result.failure(Exception("测试错误"))
        coEvery { repository.markAllAsRead() } returns Result.success(Unit)

        viewModel = NotificationViewModel(repository)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)

        // When: 调用 errorShown
        viewModel.errorShown()

        // Then: error 为 null
        assertNull(viewModel.uiState.value.error)
    }

    // ==================== 分页边界测试 ====================

    /**
     * 测试：第一页数据后连续加载多页
     * 期望：每一页数据正确追加
     */
    @Test
    fun loadMore_multiplePages_correctlyAppended() = runTest {
        // Given: 3 页数据
        coEvery {
            repository.getNotifications(page = 1)
        } returns Result.success(createPageData(
            items = listOf(createNotification(id = 1)),
            hasMore = true
        ))
        coEvery {
            repository.getNotifications(page = 2)
        } returns Result.success(createPageData(
            items = listOf(createNotification(id = 2)),
            page = 2,
            hasMore = true
        ))
        coEvery {
            repository.getNotifications(page = 3)
        } returns Result.success(createPageData(
            items = listOf(createNotification(id = 3)),
            page = 3,
            hasMore = false
        ))
        coEvery { repository.markAllAsRead() } returns Result.success(Unit)

        // When: 初始化并连续加载更多
        viewModel = NotificationViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        // Then: 3 条通知
        val state = viewModel.uiState.value
        assertEquals(3, state.notifications.size)
        assertEquals(1L, state.notifications[0].id)
        assertEquals(2L, state.notifications[1].id)
        assertEquals(3L, state.notifications[2].id)
        assertEquals(3, state.currentPage)
        assertFalse(state.hasMore)
    }

    /**
     * 测试：isLoadingMore 期间不会重复发起 loadMore
     * 期望：只调用一次 repository.getNotifications(page=2)
     */
    @Test
    fun loadMore_whileLoading_doesNotRetry() = runTest {
        // Given
        coEvery {
            repository.getNotifications(page = 1)
        } returns Result.success(createPageData(
            items = listOf(createNotification()),
            hasMore = true
        ))
        coEvery {
            repository.getNotifications(page = 2)
        } coAnswers {
            kotlinx.coroutines.delay(10_000)
            Result.success(createPageData(items = listOf(createNotification(id = 2)), page = 2, hasMore = false))
        }
        coEvery { repository.markAllAsRead() } returns Result.success(Unit)

        // When: 初始化后调用 loadMore
        viewModel = NotificationViewModel(repository)
        advanceUntilIdle()
        viewModel.loadMore()
        // 推进调度器让 loadMore 的 launch 协程开始执行（isLoadingMore 变为 true）
        testDispatcher.scheduler.advanceTimeBy(1)
        // 此时 isLoadingMore = true，再次调用应被跳过
        viewModel.loadMore()

        // Then: getNotifications(page=2) 只被调用 1 次
        coVerify(exactly = 1) { repository.getNotifications(page = 2) }
    }

    // ==================== 初始 UI 状态测试 ====================

    /**
     * 测试：ViewModel 默认 UI 状态正确
     * 期望：空列表、不加载、无错误
     */
    @Test
    fun initialState_isCorrect() = runTest {
        // Given
        coEvery {
            repository.getNotifications(page = 1)
        } returns Result.success(createPageData(hasMore = false))
        coEvery { repository.markAllAsRead() } returns Result.success(Unit)

        // When: 创建 ViewModel（init 已执行）
        viewModel = NotificationViewModel(repository)
        advanceUntilIdle()

        // Then: 最终状态正确
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isLoadingMore)
        assertNull(state.error)
        assertEquals(1, state.currentPage)
    }
}
