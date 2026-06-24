package com.example.noteshare.feature.feed.data

import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.core.network.ApiResponse
import com.example.noteshare.core.network.PageData
import com.example.noteshare.feature.feed.domain.model.NoteResponse
import com.example.noteshare.feature.feed.domain.model.UserBrief
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FeedRepositoryTest {

    private lateinit var noteApi: NoteApi
    private lateinit var repository: FeedRepository

    private val testAuthor = UserBrief(id = 1L, username = "user", nickname = "User")
    private val testNote = NoteResponse(
        id = 1L, title = "Title", content = "Content",
        likeCount = 5, commentCount = 2, createdAt = "2024-01-01", author = testAuthor
    )
    private val testPageData = PageData(
        items = listOf(testNote), page = 1, pageSize = 20, total = 50, hasMore = true
    )
    private val successPageResponse = ApiResponse(
        code = ErrorCode.SUCCESS, message = "ok", data = testPageData
    )

    @Before
    fun setUp() {
        noteApi = mockk(relaxed = false)
        repository = FeedRepository(noteApi)
    }

    // ================================================================
    // getNotes
    // ================================================================

    /** getNotes 调用 API 成功返回数据 */
    @Test
    fun getNotes_success_returnsSuccess() = runTest {
        coEvery { noteApi.getNotes(1, 20) } returns successPageResponse

        val result = repository.getNotes(1)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.items.size)
        assertEquals("Title", data.items[0].title)
        assertEquals(1, data.page)
        assertTrue(data.hasMore)
    }

    /** getNotes 传入自定义分页参数 */
    @Test
    fun getNotes_customParams_passesToApi() = runTest {
        coEvery { noteApi.getNotes(3, 10) } returns successPageResponse.copy(
            data = testPageData.copy(page = 3, pageSize = 10)
        )

        val result = repository.getNotes(3, 10)

        coVerify { noteApi.getNotes(3, 10) }
        assertTrue(result is Result.Success)
    }

    /** getNotes API 返回错误码时返回 Error */
    @Test
    fun getNotes_apiError_returnsError() = runTest {
        coEvery { noteApi.getNotes(1, 20) } returns ApiResponse(
            code = ErrorCode.AUTH_TOKEN_INVALID, message = "Token无效"
        )

        val result = repository.getNotes(1)

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertEquals(ErrorCode.AUTH_TOKEN_INVALID, error.code)
        assertEquals("Token无效", error.message)
    }

    /** getNotes API 返回 null data 时视为错误 */
    @Test
    fun getNotes_nullData_returnsError() = runTest {
        coEvery { noteApi.getNotes(1, 20) } returns ApiResponse(
            code = ErrorCode.SUCCESS, message = "ok", data = null
        )

        val result = repository.getNotes(1)

        assertTrue(result is Result.Error)
    }

    /** getNotes 网络异常时返回网络错误 */
    @Test
    fun getNotes_networkException_returnsNetworkError() = runTest {
        coEvery { noteApi.getNotes(1, 20) } throws RuntimeException("连接超时")

        val result = repository.getNotes(1)

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertEquals(ErrorCode.NETWORK_ERROR, error.code)
        assertTrue(error.message.contains("连接超时"))
    }

    // ================================================================
    // searchNotes
    // ================================================================

    /** searchNotes 成功返回搜索结果 */
    @Test
    fun searchNotes_success_returnsResults() = runTest {
        coEvery { noteApi.searchNotes("kotlin", 1, 20) } returns successPageResponse

        val result = repository.searchNotes("kotlin", 1)

        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.items.size)
    }

    /** searchNotes 传入分页参数 */
    @Test
    fun searchNotes_customPage_passesToApi() = runTest {
        coEvery { noteApi.searchNotes("kotlin", 2, 10) } returns successPageResponse

        repository.searchNotes("kotlin", 2, 10)

        coVerify { noteApi.searchNotes("kotlin", 2, 10) }
    }

    /** searchNotes API 返回错误码 */
    @Test
    fun searchNotes_apiError_returnsError() = runTest {
        coEvery { noteApi.searchNotes("error", 1, 20) } returns ApiResponse(
            code = ErrorCode.SERVER_ERROR, message = "服务器内部错误"
        )

        val result = repository.searchNotes("error", 1)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.SERVER_ERROR, (result as Result.Error).code)
    }

    /** searchNotes 网络异常 */
    @Test
    fun searchNotes_networkException_returnsNetworkError() = runTest {
        coEvery { noteApi.searchNotes(any(), any(), any()) } throws RuntimeException("DNS解析失败")

        val result = repository.searchNotes("test", 1)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
    }

    /** searchNotes 返回空数据时也视为 Success */
    @Test
    fun searchNotes_emptyData_returnsSuccess() = runTest {
        val emptyPage = PageData<NoteResponse>(
            items = emptyList(), page = 1, pageSize = 20, total = 0, hasMore = false
        )
        coEvery { noteApi.searchNotes("empty", 1, 20) } returns ApiResponse(
            code = ErrorCode.SUCCESS, message = "ok", data = emptyPage
        )

        val result = repository.searchNotes("empty", 1)

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.items.isEmpty())
    }
}
