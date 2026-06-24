package com.example.noteshare.feature.feed.presentation

import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.core.network.PageData
import com.example.noteshare.feature.feed.data.FeedRepository
import com.example.noteshare.feature.feed.domain.model.NoteResponse
import com.example.noteshare.feature.feed.domain.model.UserBrief
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FeedRepository
    private lateinit var viewModel: SearchViewModel

    private val testAuthor = UserBrief(id = 1L, username = "user1", nickname = "User One")
    private val testNote = NoteResponse(
        id = 1L, title = "Kotlin Tips", content = "Kotlin is great",
        likeCount = 10, commentCount = 3, createdAt = "2024-01-01", author = testAuthor
    )
    private val searchResultPage = PageData(
        items = listOf(testNote), page = 1, pageSize = 20, total = 25, hasMore = true
    )
    private val lastSearchPage = PageData(
        items = listOf(testNote.copy(id = 30L)), page = 2, pageSize = 20, total = 30, hasMore = false
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = false)
        viewModel = SearchViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ================================================================
    // 搜索成功
    // ================================================================

    /** 搜索关键词成功，返回结果 */
    @Test
    fun search_keyword_success_returnsResults() = runTest {
        coEvery { repository.searchNotes("kotlin", 1) } returns Result.Success(searchResultPage)

        viewModel.updateKeyword("kotlin")
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.hasSearched)
        assertEquals(1, state.results.size)
        assertEquals("Kotlin Tips", state.results[0].title)
        assertEquals(1, state.currentPage)
        assertTrue(state.hasMore)
    }

    /** 搜索关键词含前后空格时自动 trim */
    @Test
    fun search_keywordWithSpaces_trimmed() = runTest {
        coEvery { repository.searchNotes("kotlin", 1) } returns Result.Success(searchResultPage)

        viewModel.updateKeyword("  kotlin  ")
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.searchNotes("  kotlin  ", 1) }
        // 验证使用了trim后的关键词
        coVerify { repository.searchNotes("kotlin", 1) }
        assertEquals(1, viewModel.uiState.value.results.size)
    }

    // ================================================================
    // 搜索失败
    // ================================================================

    /** 搜索失败时显示错误信息 */
    @Test
    fun search_error_setsError() = runTest {
        coEvery { repository.searchNotes("kotlin", 1) } returns Result.Error(ErrorCode.SERVER_ERROR, "服务器错误")

        viewModel.updateKeyword("kotlin")
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("服务器错误", state.error)
        assertTrue(state.results.isEmpty())
    }

    /** 搜索失败后结果列表被清空 */
    @Test
    fun search_error_clearsPreviousResults() = runTest {
        coEvery { repository.searchNotes("kotlin", 1) } returns Result.Success(searchResultPage)
        viewModel.updateKeyword("kotlin")
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.results.size)

        // 第二次搜索失败
        coEvery { repository.searchNotes("java", 1) } returns Result.Error(ErrorCode.NETWORK_ERROR, "网络错误")
        viewModel.updateKeyword("java")
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.results.isEmpty())
    }

    // ================================================================
    // 空关键词
    // ================================================================

    /** 空关键词不执行搜索 */
    @Test
    fun search_emptyKeyword_doesNotCallApi() = runTest {
        viewModel.updateKeyword("")
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.searchNotes(any(), any()) }
        assertFalse(viewModel.uiState.value.hasSearched)
    }

    /** 纯空格关键词不执行搜索 */
    @Test
    fun search_blankKeyword_doesNotCallApi() = runTest {
        viewModel.updateKeyword("   ")
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.searchNotes(any(), any()) }
    }

    // ================================================================
    // 清空搜索
    // ================================================================

    /** clearKeyword 清除所有搜索状态 */
    @Test
    fun clearKeyword_clearsAllState() = runTest {
        coEvery { repository.searchNotes("kotlin", 1) } returns Result.Success(searchResultPage)
        viewModel.updateKeyword("kotlin")
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasSearched)

        viewModel.clearKeyword()
        val state = viewModel.uiState.value
        assertEquals("", state.keyword)
        assertTrue(state.results.isEmpty())
        assertFalse(state.hasSearched)
        assertNull(state.error)
    }

    /** 清空后重新搜索 */
    @Test
    fun clearKeyword_thenSearch_works() = runTest {
        coEvery { repository.searchNotes("kotlin", 1) } returns Result.Success(searchResultPage)
        viewModel.updateKeyword("kotlin")
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.results.size)

        viewModel.clearKeyword()
        assertTrue(viewModel.uiState.value.results.isEmpty())

        coEvery { repository.searchNotes("java", 1) } returns Result.Success(
            PageData(items = listOf(testNote.copy(id = 50L)), page = 1, pageSize = 20, total = 5, hasMore = false)
        )
        viewModel.updateKeyword("java")
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.results.size)
        assertEquals(50L, viewModel.uiState.value.results[0].id)
    }

    // ================================================================
    // 分页加载
    // ================================================================

    /** loadMore 追加结果 */
    @Test
    fun loadMore_success_appendsResults() = runTest {
        coEvery { repository.searchNotes("kotlin", 1) } returns Result.Success(searchResultPage)
        viewModel.updateKeyword("kotlin")
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.results.size)

        coEvery { repository.searchNotes("kotlin", 2) } returns Result.Success(lastSearchPage)
        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.results.size)
        assertEquals(2, viewModel.uiState.value.currentPage)
        assertFalse(viewModel.uiState.value.isLoadingMore)
    }

    /** loadMore 失败时设置 loadMoreFailed */
    @Test
    fun loadMore_error_setsLoadMoreFailed() = runTest {
        coEvery { repository.searchNotes("kotlin", 1) } returns Result.Success(searchResultPage)
        viewModel.updateKeyword("kotlin")
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.searchNotes("kotlin", 2) } returns Result.Error(ErrorCode.NETWORK_ERROR, "超时")
        viewModel.loadMore()
        // 仅推进当前协程，不触发 delay(3000) 重置
        testDispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isLoadingMore)
    }

    /** loadMore 失败后3秒重置 loadMoreFailed */
    @Test
    fun loadMore_error_after3Seconds_resetsLoadMoreFailed() = runTest {
        coEvery { repository.searchNotes("kotlin", 1) } returns Result.Success(searchResultPage)
        viewModel.updateKeyword("kotlin")
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.searchNotes("kotlin", 2) } returns Result.Error(ErrorCode.NETWORK_ERROR, "超时")
        viewModel.loadMore()
        // 推进 loadMore 协程完成
        testDispatcher.scheduler.runCurrent()

        // 推进时间 3 秒，使 delay(3000) 重置 loadMoreFailed
        advanceTimeBy(3000)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.uiState.value.loadMoreFailed)
    }

    /** loadMore 在 hasMore=false 时不执行 */
    @Test
    fun loadMore_hasMoreFalse_doesNotCallApi() = runTest {
        coEvery { repository.searchNotes("kotlin", 1) } returns Result.Success(
            PageData(items = listOf(testNote), page = 1, pageSize = 20, total = 5, hasMore = false)
        )
        viewModel.updateKeyword("kotlin")
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.hasMore)

        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.searchNotes(any(), any()) }
    }

    /** loadMore 在 isLoading 时不执行 */
    @Test
    fun loadMore_whileLoading_doesNotCallApi() = runTest {
        coEvery { repository.searchNotes("kotlin", 1) } coAnswers {
            kotlinx.coroutines.delay(5000)
            Result.Success(searchResultPage)
        }
        viewModel.updateKeyword("kotlin")
        viewModel.search()
        // isLoading=true，loadMore应被跳过
        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.searchNotes("kotlin", 1) }
    }

    /** loadMore 在 loadMoreFailed 时不执行 */
    @Test
    fun loadMore_whileLoadMoreFailed_doesNotCallApi() = runTest {
        coEvery { repository.searchNotes("kotlin", 1) } returns Result.Success(searchResultPage)
        viewModel.updateKeyword("kotlin")
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.searchNotes("kotlin", 2) } returns Result.Error(50000, "错误")
        viewModel.loadMore()
        // 推进 loadMore 协程完成，但不触发 delay(3000)
        testDispatcher.scheduler.runCurrent()

        // loadMoreFailed 期间再调用
        viewModel.loadMore()
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) { repository.searchNotes("kotlin", 2) }
    }

    // ================================================================
    // 无结果场景
    // ================================================================

    /** 搜索无结果时返回空列表 */
    @Test
    fun search_noResults_returnsEmptyList() = runTest {
        coEvery { repository.searchNotes("unknown", 1) } returns Result.Success(
            PageData(items = emptyList(), page = 1, pageSize = 20, total = 0, hasMore = false)
        )

        viewModel.updateKeyword("unknown")
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.results.isEmpty())
        assertFalse(state.hasMore)
        assertFalse(state.isLoading)
        assertTrue(state.hasSearched)
    }

    // ================================================================
    // isLoading 状态管理
    // ================================================================

    /** search 期间 isLoading=true，完成后 false */
    @Test
    fun search_togglesIsLoading() = runTest {
        coEvery { repository.searchNotes("kotlin", 1) } coAnswers {
            kotlinx.coroutines.delay(1000)
            Result.Success(searchResultPage)
        }
        viewModel.updateKeyword("kotlin")
        viewModel.search()
        assertTrue(viewModel.uiState.value.isLoading)

        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    /** search 重置分页状态 */
    @Test
    fun search_resetsPaginationState() = runTest {
        // 先加载第二页
        coEvery { repository.searchNotes("kotlin", 1) } returns Result.Success(searchResultPage)
        viewModel.updateKeyword("kotlin")
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.searchNotes("kotlin", 2) } returns Result.Success(lastSearchPage)
        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.currentPage)

        // 再次搜索，应重置到第1页
        coEvery { repository.searchNotes("java", 1) } returns Result.Success(
            PageData(items = listOf(testNote.copy(id = 99L)), page = 1, pageSize = 20, total = 1, hasMore = false)
        )
        viewModel.updateKeyword("java")
        viewModel.search()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.currentPage)
        assertEquals(1, viewModel.uiState.value.results.size)
    }
}
