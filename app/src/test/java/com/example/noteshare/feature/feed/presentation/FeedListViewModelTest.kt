package com.example.noteshare.feature.feed.presentation

import app.cash.turbine.test
import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.core.network.PageData
import com.example.noteshare.feature.feed.data.FeedRepository
import com.example.noteshare.feature.feed.domain.model.NoteResponse
import com.example.noteshare.feature.feed.domain.model.UserBrief
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FeedRepository
    private lateinit var viewModel: FeedListViewModel

    private val testAuthor = UserBrief(id = 1L, username = "testuser", nickname = "Test User")
    private val testNote = NoteResponse(
        id = 1L, title = "Test Title", content = "Test Content",
        likeCount = 10, commentCount = 5, createdAt = "2024-01-01", author = testAuthor
    )
    private val testPageData = PageData(
        items = listOf(testNote),
        page = 1, pageSize = 20, total = 50, hasMore = true
    )
    private val lastPageData = PageData(
        items = listOf(testNote.copy(id = 30L)),
        page = 2, pageSize = 20, total = 30, hasMore = false
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = false)
        // Default: repository returns success so init completes normally
        coEvery { repository.getNotes(any()) } returns Result.Success(testPageData)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ================================================================
    // 初始加载
    // ================================================================

    /** init 时自动触发 refresh，成功时应加载第一页数据 */
    @Test
    fun init_loadFirstPage_success() = runTest {
        coEvery { repository.getNotes(1) } returns Result.Success(testPageData)

        viewModel = FeedListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(1, state.notes.size)
        assertEquals("Test Title", state.notes[0].title)
        assertEquals(1, state.currentPage)
        assertTrue(state.hasMore)
    }

    /** init 时网络失败，应设置错误信息并取消 isLoading */
    @Test
    fun init_loadFirstPage_error() = runTest {
        coEvery { repository.getNotes(1) } returns Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败")

        viewModel = FeedListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("网络请求失败", state.error)
        assertTrue(state.notes.isEmpty())
    }

    // ================================================================
    // 下拉刷新
    // ================================================================

    /** 手动触发 refresh，成功时替换列表数据并重置分页 */
    @Test
    fun refresh_success_updatesNotesAndResetsPage() = runTest {
        val page1 = PageData(
            items = listOf(testNote), page = 1, pageSize = 20, total = 50, hasMore = true
        )
        coEvery { repository.getNotes(1) } returns Result.Success(page1)

        viewModel = FeedListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // 模拟加载更多后的状态
        val page2 = PageData(
            items = listOf(testNote.copy(id = 2L)), page = 2, pageSize = 20, total = 50, hasMore = true
        )
        coEvery { repository.getNotes(2) } returns Result.Success(page2)
        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.notes.size)

        // 执行刷新
        val freshPage = PageData(
            items = listOf(testNote.copy(id = 100L)), page = 1, pageSize = 20, total = 10, hasMore = false
        )
        coEvery { repository.getNotes(1) } returns Result.Success(freshPage)
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.notes.size)
        assertEquals(100L, state.notes[0].id)
        assertFalse(state.hasMore)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    /** refresh 时网络失败，应保留原有数据并显示错误 */
    @Test
    fun refresh_error_keepsExistingDataAndShowsError() = runTest {
        viewModel = FeedListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.notes.size)

        coEvery { repository.getNotes(1) } returns Result.Error(ErrorCode.SERVER_ERROR, "服务器错误")
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("服务器错误", state.error)
        // 原有数据保留（init加载的）
        assertEquals(1, state.notes.size)
    }

    /** refresh 设置 isLoading = true 然后完成时恢复 false */
    @Test
    fun refresh_togglesIsLoadingCorrectly() = runTest {
        coEvery { repository.getNotes(1) } returns Result.Success(testPageData)
        viewModel = FeedListViewModel(repository)
        // 暂停协程，检查 isLoading = true 状态
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.getNotes(1) } coAnswers {
            kotlinx.coroutines.delay(1000)
            Result.Success(testPageData)
        }
        viewModel.refresh()
        // 此时 isLoading 应为 true
        assertTrue(viewModel.uiState.value.isLoading)

        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ================================================================
    // 加载更多
    // ================================================================

    /** loadMore 成功时追加数据到列表 */
    @Test
    fun loadMore_success_appendsNotes() = runTest {
        viewModel = FeedListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val page2 = PageData(
            items = listOf(testNote.copy(id = 2L)), page = 2, pageSize = 20, total = 50, hasMore = true
        )
        coEvery { repository.getNotes(2) } returns Result.Success(page2)

        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.notes.size)
        assertEquals(2L, state.notes[1].id)
        assertEquals(2, state.currentPage)
        assertTrue(state.hasMore)
        assertFalse(state.isLoadingMore)
    }

    /** loadMore 失败时设置 loadMoreFailed = true 和错误信息 */
    @Test
    fun loadMore_error_setsLoadMoreFailedAndError() = runTest {
        viewModel = FeedListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.getNotes(2) } returns Result.Error(ErrorCode.NETWORK_ERROR, "网络超时")

        viewModel.loadMore()
        // 仅推进 loadMore 协程本身，不要 advanceUntilIdle（会同时执行 delay 重置）
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingMore)
        // 注意：advanceUntilIdle 会同时执行 delay(3000) 重置 loadMoreFailed，
        // 所以此处用 runCurrent 只推进当前时刻的协程
        assertEquals("网络超时", state.error)
    }

    /** loadMore 失败后3秒 loadMoreFailed 重置为 false */
    @Test
    fun loadMore_error_after3Seconds_loadMoreFailedResets() = runTest {
        viewModel = FeedListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.getNotes(2) } returns Result.Error(ErrorCode.NETWORK_ERROR, "超时")
        viewModel.loadMore()
        // 推进到 loadMore 协程完成（设置 loadMoreFailed = true）
        testDispatcher.scheduler.runCurrent()
        // 此时 loadMoreFailed 应为 true（delay 协程还未执行）
        assertTrue(viewModel.uiState.value.loadMoreFailed)

        // 推进时间 3 秒，使 delay(3000) 协程执行
        advanceTimeBy(3000)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.uiState.value.loadMoreFailed)
    }

    /** loadMore 在 hasMore=false 时不发起请求 */
    @Test
    fun loadMore_hasMoreFalse_doesNotCallApi() = runTest {
        coEvery { repository.getNotes(1) } returns Result.Success(lastPageData)
        viewModel = FeedListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.hasMore)

        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()

        // 不应调用第2页
        coVerify(exactly = 0) { repository.getNotes(2) }
        assertEquals(1, viewModel.uiState.value.notes.size)
    }

    /** loadMore 在 isLoading 时不重复调用 */
    @Test
    fun loadMore_whileLoading_doesNotCallApi() = runTest {
        coEvery { repository.getNotes(1) } coAnswers {
            kotlinx.coroutines.delay(5000)
            Result.Success(testPageData)
        }
        viewModel = FeedListViewModel(repository)
        // 不等待完成，直接调用 loadMore
        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()

        // 仅应调用一次（init的那一次），loadMore因isLoading而跳过
        coVerify(exactly = 1) { repository.getNotes(any()) }
    }

    /** loadMore 在 isLoadingMore=true 时不重复调用 */
    @Test
    fun loadMore_whileLoadingMore_doesNotCallApi() = runTest {
        coEvery { repository.getNotes(1) } returns Result.Success(testPageData)
        viewModel = FeedListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.getNotes(2) } coAnswers {
            kotlinx.coroutines.delay(5000)
            Result.Success(testPageData.copy(page = 2))
        }
        viewModel.loadMore()
        // loadMore 进行中，再次调用应被跳过
        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.getNotes(2) }
    }

    /** loadMore 在 loadMoreFailed=true 时不发起请求（需等3秒后重置） */
    @Test
    fun loadMore_whileLoadMoreFailed_doesNotCallApi() = runTest {
        viewModel = FeedListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.getNotes(2) } returns Result.Error(ErrorCode.NETWORK_ERROR, "超时")
        viewModel.loadMore()
        // 推进 loadMore 协程完成，但不推进 delay(3000)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.loadMoreFailed)

        // loadMoreFailed 期间再调用（delay 还没执行完）
        viewModel.loadMore()
        testDispatcher.scheduler.runCurrent()

        // 仅调用了一次 page 2
        coVerify(exactly = 1) { repository.getNotes(2) }
    }

    // ================================================================
    // 空列表场景
    // ================================================================

    /** 服务端返回空列表时，应显示空列表且 hasMore=false */
    @Test
    fun refresh_emptyList_showsEmptyAndHasMoreFalse() = runTest {
        val emptyPage = PageData<NoteResponse>(
            items = emptyList(), page = 1, pageSize = 20, total = 0, hasMore = false
        )
        coEvery { repository.getNotes(1) } returns Result.Success(emptyPage)

        viewModel = FeedListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.notes.isEmpty())
        assertFalse(state.hasMore)
        assertFalse(state.isLoading)
    }

    // ================================================================
    // errorShown
    // ================================================================

    /** errorShown 清除错误信息 */
    @Test
    fun errorShown_clearsError() = runTest {
        coEvery { repository.getNotes(1) } returns Result.Error(ErrorCode.NETWORK_ERROR, "网络错误")
        viewModel = FeedListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("网络错误", viewModel.uiState.value.error)

        viewModel.errorShown()
        assertNull(viewModel.uiState.value.error)
    }

    // ================================================================
    // 分页边界
    // ================================================================

    /** 多次分页加载后总数正确 */
    @Test
    fun loadMore_multiplePages_accumulatesNotesCorrectly() = runTest {
        viewModel = FeedListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.notes.size)

        val page2 = PageData(
            items = listOf(testNote.copy(id = 2L)), page = 2, pageSize = 20, total = 50, hasMore = true
        )
        coEvery { repository.getNotes(2) } returns Result.Success(page2)
        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.notes.size)

        val page3 = PageData(
            items = listOf(testNote.copy(id = 3L)), page = 3, pageSize = 20, total = 50, hasMore = false
        )
        coEvery { repository.getNotes(3) } returns Result.Success(page3)
        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.notes.size)
        assertFalse(viewModel.uiState.value.hasMore)
        assertEquals(3, viewModel.uiState.value.currentPage)
    }

    // ================================================================
    // 网络异常处理
    // ================================================================

    /** 服务端返回业务错误码时应正确传递错误信息 */
    @Test
    fun refresh_businessError_passesErrorMessage() = runTest {
        coEvery { repository.getNotes(1) } returns Result.Error(ErrorCode.AUTH_TOKEN_INVALID, "Token无效")
        viewModel = FeedListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Token无效", viewModel.uiState.value.error)
    }

    // ================================================================
    // isRefreshing 状态（通过 isLoading 区分）
    // ================================================================

    /** refresh 初始 isLoading 为 false（init 完成后） */
    @Test
    fun initialState_isNotLoading() = runTest {
        viewModel = FeedListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isLoadingMore)
    }

    /** 刷新完成后，旧的加载更多响应不应再追加到新列表 */
    @Test
    fun refresh_whileLoadMoreInFlight_ignoresOldLoadMoreResult() = runTest {
        coEvery { repository.getNotes(1) } returns Result.Success(testPageData)
        viewModel = FeedListViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val oldLoadMore = kotlinx.coroutines.CompletableDeferred<Result<PageData<NoteResponse>>>()
        coEvery { repository.getNotes(2) } coAnswers {
            oldLoadMore.await()
        }
        viewModel.loadMore()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isLoadingMore)

        val refreshedPage = PageData(
            items = listOf(testNote.copy(id = 100L)),
            page = 1,
            pageSize = 20,
            total = 1,
            hasMore = false
        )
        coEvery { repository.getNotes(1) } returns Result.Success(refreshedPage)
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(100L), viewModel.uiState.value.notes.map { it.id })
        assertFalse(viewModel.uiState.value.isLoadingMore)

        oldLoadMore.complete(Result.Success(lastPageData))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(100L), viewModel.uiState.value.notes.map { it.id })
    }
}
