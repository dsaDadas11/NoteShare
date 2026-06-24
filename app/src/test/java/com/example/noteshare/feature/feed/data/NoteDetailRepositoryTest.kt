package com.example.noteshare.feature.feed.data

import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.core.network.ApiResponse
import com.example.noteshare.core.network.PageData
import com.example.noteshare.feature.feed.domain.model.CommentResponse
import com.example.noteshare.feature.feed.domain.model.CreateCommentRequest
import com.example.noteshare.feature.feed.domain.model.NoteDetailResponse
import com.example.noteshare.feature.feed.domain.model.UserBrief
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NoteDetailRepositoryTest {

    private lateinit var noteApi: NoteApi
    private lateinit var repository: NoteDetailRepository

    private val testAuthor = UserBrief(id = 10L, username = "author", nickname = "Author")
    private val testNoteDetail = NoteDetailResponse(
        id = 100L, title = "Note", content = "Content",
        likeCount = 5, commentCount = 3, isLiked = false,
        isAuthorFollowed = false, isAuthorSelf = false,
        createdAt = "2024-01-01", author = testAuthor
    )
    private val testComment = CommentResponse(
        id = 200L, content = "Comment", createdAt = "2024-01-02",
        user = UserBrief(id = 20L, username = "user", nickname = "User"),
        isMine = false, likeCount = 1, liked = false, replyCount = 0
    )
    private val successUnitResponse = ApiResponse<Unit>(code = ErrorCode.SUCCESS, message = "ok")

    @Before
    fun setUp() {
        noteApi = mockk(relaxed = false)
        repository = NoteDetailRepository(noteApi)
    }

    // ================================================================
    // getNoteDetail
    // ================================================================

    /** 获取笔记详情成功 */
    @Test
    fun getNoteDetail_success_returnsDetail() = runTest {
        coEvery { noteApi.getNoteDetail(100L) } returns ApiResponse(
            code = ErrorCode.SUCCESS, message = "ok", data = testNoteDetail
        )

        val result = repository.getNoteDetail(100L)

        assertTrue(result is Result.Success)
        assertEquals("Note", (result as Result.Success).data.title)
    }

    /** 获取笔记详情 API 错误 */
    @Test
    fun getNoteDetail_apiError_returnsError() = runTest {
        coEvery { noteApi.getNoteDetail(999L) } returns ApiResponse(
            code = ErrorCode.NOTE_NOT_FOUND, message = "笔记不存在"
        )

        val result = repository.getNoteDetail(999L)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NOTE_NOT_FOUND, (result as Result.Error).code)
    }

    /** 获取笔记详情网络异常 */
    @Test
    fun getNoteDetail_networkException_returnsNetworkError() = runTest {
        coEvery { noteApi.getNoteDetail(any()) } throws RuntimeException("Connection refused")

        val result = repository.getNoteDetail(100L)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
    }

    /** 获取笔记详情 null data */
    @Test
    fun getNoteDetail_nullData_returnsError() = runTest {
        coEvery { noteApi.getNoteDetail(100L) } returns ApiResponse(
            code = ErrorCode.SUCCESS, message = "ok", data = null
        )

        val result = repository.getNoteDetail(100L)

        assertTrue(result is Result.Error)
    }

    // ================================================================
    // likeNote / unlikeNote
    // ================================================================

    /** 点赞成功 */
    @Test
    fun likeNote_success_returnsSuccess() = runTest {
        coEvery { noteApi.likeNote(100L) } returns successUnitResponse

        val result = repository.likeNote(100L)

        assertTrue(result is Result.Success)
        coVerify { noteApi.likeNote(100L) }
    }

    /** 点赞失败（已点赞） */
    @Test
    fun likeNote_alreadyLiked_returnsError() = runTest {
        coEvery { noteApi.likeNote(100L) } returns ApiResponse(
            code = ErrorCode.ALREADY_LIKED, message = "已经点赞过了"
        )

        val result = repository.likeNote(100L)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.ALREADY_LIKED, (result as Result.Error).code)
    }

    /** 点赞网络异常 */
    @Test
    fun likeNote_networkException_returnsNetworkError() = runTest {
        coEvery { noteApi.likeNote(any()) } throws RuntimeException("timeout")

        val result = repository.likeNote(100L)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
    }

    /** 取消点赞成功 */
    @Test
    fun unlikeNote_success_returnsSuccess() = runTest {
        coEvery { noteApi.unlikeNote(100L) } returns successUnitResponse

        val result = repository.unlikeNote(100L)

        assertTrue(result is Result.Success)
    }

    /** 取消点赞失败（未点赞） */
    @Test
    fun unlikeNote_notLiked_returnsError() = runTest {
        coEvery { noteApi.unlikeNote(100L) } returns ApiResponse(
            code = ErrorCode.NOT_LIKED, message = "还没点赞"
        )

        val result = repository.unlikeNote(100L)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NOT_LIKED, (result as Result.Error).code)
    }

    // ================================================================
    // deleteNote
    // ================================================================

    /** 删除笔记成功 */
    @Test
    fun deleteNote_success_returnsSuccess() = runTest {
        coEvery { noteApi.deleteNote(100L) } returns successUnitResponse

        val result = repository.deleteNote(100L)

        assertTrue(result is Result.Success)
    }

    /** 删除笔记无权限 */
    @Test
    fun deleteNote_forbidden_returnsError() = runTest {
        coEvery { noteApi.deleteNote(100L) } returns ApiResponse(
            code = ErrorCode.NOTE_FORBIDDEN, message = "无权删除"
        )

        val result = repository.deleteNote(100L)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NOTE_FORBIDDEN, (result as Result.Error).code)
    }

    /** 删除笔记网络异常 */
    @Test
    fun deleteNote_networkException_returnsNetworkError() = runTest {
        coEvery { noteApi.deleteNote(any()) } throws RuntimeException("IO error")

        val result = repository.deleteNote(100L)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
    }

    // ================================================================
    // getComments
    // ================================================================

    /** 获取评论列表成功 */
    @Test
    fun getComments_success_returnsComments() = runTest {
        val commentsPage = PageData(
            items = listOf(testComment), page = 1, pageSize = 20, total = 10, hasMore = true
        )
        coEvery { noteApi.getComments(100L, 1, 20) } returns ApiResponse(
            code = ErrorCode.SUCCESS, message = "ok", data = commentsPage
        )

        val result = repository.getComments(100L, 1)

        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.items.size)
    }

    /** 获取评论列表分页参数传递 */
    @Test
    fun getComments_page2_passesCorrectParams() = runTest {
        coEvery { noteApi.getComments(100L, 2, 10) } returns ApiResponse(
            code = ErrorCode.SUCCESS, message = "ok",
            data = PageData(items = emptyList(), page = 2, pageSize = 10, total = 20, hasMore = false)
        )

        repository.getComments(100L, 2, 10)

        coVerify { noteApi.getComments(100L, 2, 10) }
    }

    /** 获取评论列表失败 */
    @Test
    fun getComments_apiError_returnsError() = runTest {
        coEvery { noteApi.getComments(100L, 1, 20) } returns ApiResponse(
            code = ErrorCode.SERVER_ERROR, message = "服务错误"
        )

        val result = repository.getComments(100L, 1)

        assertTrue(result is Result.Error)
    }

    /** 获取评论网络异常 */
    @Test
    fun getComments_networkException_returnsNetworkError() = runTest {
        coEvery { noteApi.getComments(any(), any(), any()) } throws RuntimeException("timeout")

        val result = repository.getComments(100L, 1)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
    }

    // ================================================================
    // createComment
    // ================================================================

    /** 创建顶级评论成功 */
    @Test
    fun createComment_topLevel_success() = runTest {
        coEvery {
            noteApi.createComment(100L, CreateCommentRequest("Nice!", null, null))
        } returns ApiResponse(code = ErrorCode.SUCCESS, message = "ok", data = testComment)

        val result = repository.createComment(100L, "Nice!")

        assertTrue(result is Result.Success)
        assertEquals("Comment", (result as Result.Success).data.content)
    }

    /** 创建回复评论成功 */
    @Test
    fun createComment_reply_success() = runTest {
        val replyRequest = CreateCommentRequest("Reply!", 200L, "Author")
        coEvery {
            noteApi.createComment(100L, replyRequest)
        } returns ApiResponse(code = ErrorCode.SUCCESS, message = "ok", data = testComment)

        val result = repository.createComment(100L, "Reply!", 200L, "Author")

        assertTrue(result is Result.Success)
        coVerify { noteApi.createComment(100L, replyRequest) }
    }

    /** 创建评论失败（空评论） */
    @Test
    fun createComment_emptyContent_returnsError() = runTest {
        coEvery {
            noteApi.createComment(100L, CreateCommentRequest("", null, null))
        } returns ApiResponse(code = ErrorCode.COMMENT_EMPTY, message = "评论不能为空")

        val result = repository.createComment(100L, "")

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.COMMENT_EMPTY, (result as Result.Error).code)
    }

    /** 创建评论网络异常 */
    @Test
    fun createComment_networkException_returnsNetworkError() = runTest {
        coEvery { noteApi.createComment(any(), any()) } throws RuntimeException("timeout")

        val result = repository.createComment(100L, "test")

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
    }

    // ================================================================
    // deleteComment
    // ================================================================

    /** 删除评论成功 */
    @Test
    fun deleteComment_success_returnsSuccess() = runTest {
        coEvery { noteApi.deleteComment(100L, 200L) } returns successUnitResponse

        val result = repository.deleteComment(100L, 200L)

        assertTrue(result is Result.Success)
    }

    /** 删除评论无权限 */
    @Test
    fun deleteComment_forbidden_returnsError() = runTest {
        coEvery { noteApi.deleteComment(100L, 200L) } returns ApiResponse(
            code = ErrorCode.COMMENT_FORBIDDEN, message = "无权删除此评论"
        )

        val result = repository.deleteComment(100L, 200L)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.COMMENT_FORBIDDEN, (result as Result.Error).code)
    }

    /** 删除评论网络异常 */
    @Test
    fun deleteComment_networkException_returnsNetworkError() = runTest {
        coEvery { noteApi.deleteComment(any(), any()) } throws RuntimeException("IO error")

        val result = repository.deleteComment(100L, 200L)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
    }

    // ================================================================
    // getCommentReplies
    // ================================================================

    /** 获取回复列表成功 */
    @Test
    fun getCommentReplies_success_returnsReplies() = runTest {
        val replies = listOf(testComment.copy(id = 300L, parentId = 200L))
        coEvery { noteApi.getCommentReplies(100L, 200L) } returns ApiResponse(
            code = ErrorCode.SUCCESS, message = "ok", data = replies
        )

        val result = repository.getCommentReplies(100L, 200L)

        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.size)
    }

    /** 获取回复列表失败 */
    @Test
    fun getCommentReplies_apiError_returnsError() = runTest {
        coEvery { noteApi.getCommentReplies(100L, 200L) } returns ApiResponse(
            code = ErrorCode.COMMENT_NOT_FOUND, message = "评论不存在"
        )

        val result = repository.getCommentReplies(100L, 200L)

        assertTrue(result is Result.Error)
    }

    /** 获取回复列表网络异常 */
    @Test
    fun getCommentReplies_networkException_returnsNetworkError() = runTest {
        coEvery { noteApi.getCommentReplies(any(), any()) } throws RuntimeException("timeout")

        val result = repository.getCommentReplies(100L, 200L)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
    }

    // ================================================================
    // likeComment / unlikeComment
    // ================================================================

    /** 评论点赞成功 */
    @Test
    fun likeComment_success_returnsSuccess() = runTest {
        coEvery { noteApi.likeComment(100L, 200L) } returns successUnitResponse

        val result = repository.likeComment(100L, 200L)

        assertTrue(result is Result.Success)
    }

    /** 评论点赞失败（已点赞） */
    @Test
    fun likeComment_alreadyLiked_returnsError() = runTest {
        coEvery { noteApi.likeComment(100L, 200L) } returns ApiResponse(
            code = ErrorCode.COMMENT_LIKE_ALREADY, message = "已经点赞"
        )

        val result = repository.likeComment(100L, 200L)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.COMMENT_LIKE_ALREADY, (result as Result.Error).code)
    }

    /** 评论取消点赞成功 */
    @Test
    fun unlikeComment_success_returnsSuccess() = runTest {
        coEvery { noteApi.unlikeComment(100L, 200L) } returns successUnitResponse

        val result = repository.unlikeComment(100L, 200L)

        assertTrue(result is Result.Success)
    }

    /** 评论取消点赞失败（未点赞） */
    @Test
    fun unlikeComment_notLiked_returnsError() = runTest {
        coEvery { noteApi.unlikeComment(100L, 200L) } returns ApiResponse(
            code = ErrorCode.COMMENT_LIKE_NOT_FOUND, message = "还没点赞"
        )

        val result = repository.unlikeComment(100L, 200L)

        assertTrue(result is Result.Error)
    }

    /** 评论点赞网络异常 */
    @Test
    fun likeComment_networkException_returnsNetworkError() = runTest {
        coEvery { noteApi.likeComment(any(), any()) } throws RuntimeException("timeout")

        val result = repository.likeComment(100L, 200L)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
    }
}
