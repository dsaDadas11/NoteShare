package com.example.noteshare.feature.feed.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.core.network.PageData
import com.example.noteshare.feature.feed.data.NoteDetailRepository
import com.example.noteshare.feature.feed.domain.model.CommentResponse
import com.example.noteshare.feature.feed.domain.model.NoteDetailResponse
import com.example.noteshare.feature.feed.domain.model.UserBrief
import com.example.noteshare.feature.profile.data.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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

@OptIn(ExperimentalCoroutinesApi::class)
class NoteDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: NoteDetailRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var savedStateHandle: SavedStateHandle

    private val testAuthor = UserBrief(id = 10L, username = "author", nickname = "Author")
    private val testNoteDetail = NoteDetailResponse(
        id = 100L, title = "Test Note", content = "Content",
        likeCount = 5, commentCount = 3, isLiked = false,
        isAuthorFollowed = false, isAuthorSelf = false,
        createdAt = "2024-01-01", author = testAuthor
    )
    private val testComment = CommentResponse(
        id = 200L, content = "Nice post!", createdAt = "2024-01-02",
        user = UserBrief(id = 20L, username = "commenter", nickname = "Commenter"),
        isMine = false, likeCount = 2, liked = false, replyCount = 0
    )
    private val testCommentsPage = PageData(
        items = listOf(testComment), page = 1, pageSize = 20, total = 10, hasMore = true
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = false)
        profileRepository = mockk(relaxed = false)
        savedStateHandle = SavedStateHandle(mapOf("noteId" to "100"))

        coEvery { repository.getNoteDetail(100L) } returns Result.Success(testNoteDetail)
        coEvery { repository.getComments(100L, 1) } returns Result.Success(testCommentsPage)
        coEvery { repository.likeNote(100L) } returns Result.Success(Unit)
        coEvery { repository.unlikeNote(100L) } returns Result.Success(Unit)
        coEvery { repository.deleteNote(100L) } returns Result.Success(Unit)
        coEvery { repository.deleteComment(100L, any()) } returns Result.Success(Unit)
        coEvery { repository.getCommentReplies(100L, any()) } returns Result.Success(emptyList())
        coEvery { repository.likeComment(100L, any()) } returns Result.Success(Unit)
        coEvery { repository.unlikeComment(100L, any()) } returns Result.Success(Unit)
        coEvery { repository.createComment(100L, any(), any(), any()) } returns Result.Success(testComment)
        coEvery { profileRepository.followUser(10L) } returns Result.Success(Unit)
        coEvery { profileRepository.unfollowUser(10L) } returns Result.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 创建 ViewModel，自动加载笔记详情和评论列表 */
    private fun createViewModel(): NoteDetailViewModel {
        return NoteDetailViewModel(repository, profileRepository, savedStateHandle).also {
            testDispatcher.scheduler.advanceUntilIdle()
        }
    }

    // ================================================================
    // 初始化与加载
    // ================================================================

    /** init 时自动加载笔记详情和评论 */
    @Test
    fun init_loadsNoteDetailAndComments() = runTest {
        val vm = createViewModel()
        val state = vm.uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.noteDetail)
        assertEquals("Test Note", state.noteDetail?.title)
        assertEquals(1, state.comments.size)
        assertEquals("Nice post!", state.comments[0].content)
    }

    /** 加载笔记详情失败时设置错误信息 */
    @Test
    fun loadNoteDetail_error_setsError() = runTest {
        coEvery { repository.getNoteDetail(100L) } returns Result.Error(ErrorCode.NOTE_NOT_FOUND, "笔记不存在")
        coEvery { repository.getComments(100L, 1) } returns Result.Success(testCommentsPage)

        val vm = createViewModel()
        assertEquals("笔记不存在", vm.uiState.value.error)
        assertNull(vm.uiState.value.noteDetail)
    }

    /** noteId 为 0 时应显示无效 ID 错误 */
    @Test
    fun init_invalidNoteId_showsError() = runTest {
        val badHandle = SavedStateHandle(mapOf("noteId" to "0"))
        val vm = NoteDetailViewModel(repository, profileRepository, badHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("无效的笔记 ID", vm.uiState.value.error)
    }

    /** noteId 为非数字字符串时也视为无效 */
    @Test
    fun init_nonNumericNoteId_showsError() = runTest {
        val badHandle = SavedStateHandle(mapOf("noteId" to "abc"))
        val vm = NoteDetailViewModel(repository, profileRepository, badHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("无效的笔记 ID", vm.uiState.value.error)
    }

    // ================================================================
    // 点赞 / 取消点赞（乐观更新 + 失败回滚）
    // ================================================================

    /** 点赞成功时乐观更新 isLiked 和 likeCount */
    @Test
    fun toggleLike_notLiked_success_updatesIsLikedAndCount() = runTest {
        val syncedLiked = testNoteDetail.copy(isLiked = true, likeCount = 6)
        coEvery { repository.getNoteDetail(100L) } returnsMany listOf(
            Result.Success(testNoteDetail),
            Result.Success(syncedLiked)
        )

        val vm = createViewModel()
        assertEquals(false, vm.uiState.value.noteDetail?.isLiked)
        assertEquals(5, vm.uiState.value.noteDetail?.likeCount)

        vm.toggleLike()
        assertTrue(vm.uiState.value.noteDetail?.isLiked == true)
        assertEquals(6, vm.uiState.value.noteDetail?.likeCount)

        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.noteDetail?.isLiked == true)
        assertEquals(6, vm.uiState.value.noteDetail?.likeCount)
    }

    /** 取消点赞成功时 likeCount 减少 */
    @Test
    fun toggleLike_liked_success_decrementsCount() = runTest {
        val likedNote = testNoteDetail.copy(isLiked = true, likeCount = 5)
        val syncedUnliked = testNoteDetail.copy(isLiked = false, likeCount = 4)
        coEvery { repository.getNoteDetail(100L) } returnsMany listOf(
            Result.Success(likedNote),
            Result.Success(syncedUnliked)
        )

        val vm = createViewModel()
        assertTrue(vm.uiState.value.noteDetail?.isLiked == true)

        vm.toggleLike()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.noteDetail?.isLiked == true)
        assertEquals(4, vm.uiState.value.noteDetail?.likeCount)
    }

    /** 点赞数为 0 时取消点赞不应变成负数 */
    @Test
    fun toggleLike_unlikeWhenCountZero_doesNotGoNegative() = runTest {
        val likedNote = testNoteDetail.copy(isLiked = true, likeCount = 0)
        val syncedUnliked = testNoteDetail.copy(isLiked = false, likeCount = 0)
        coEvery { repository.getNoteDetail(100L) } returnsMany listOf(
            Result.Success(likedNote),
            Result.Success(syncedUnliked)
        )

        val vm = createViewModel()
        vm.toggleLike()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.noteDetail?.isLiked == true)
        assertEquals(0, vm.uiState.value.noteDetail?.likeCount)
    }

    /** 服务端已点赞时同步详情，不展示重复点赞错误 */
    @Test
    fun toggleLike_alreadyLiked_syncsFromServer() = runTest {
        val syncedLiked = testNoteDetail.copy(isLiked = true, likeCount = 6)
        coEvery { repository.getNoteDetail(100L) } returnsMany listOf(
            Result.Success(testNoteDetail),
            Result.Success(syncedLiked)
        )
        coEvery { repository.likeNote(100L) } returns Result.Error(ErrorCode.ALREADY_LIKED, "已经点赞")

        val vm = createViewModel()
        vm.toggleLike()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.noteDetail?.isLiked == true)
        assertEquals(6, vm.uiState.value.noteDetail?.likeCount)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun toggleLike_likeUnlikeLike_success() = runTest {
        val syncedLiked = testNoteDetail.copy(isLiked = true, likeCount = 6)
        val syncedUnliked = testNoteDetail.copy(isLiked = false, likeCount = 5)
        coEvery { repository.getNoteDetail(100L) } returnsMany listOf(
            Result.Success(testNoteDetail),
            Result.Success(syncedLiked),
            Result.Success(syncedUnliked),
            Result.Success(syncedLiked)
        )

        val vm = createViewModel()

        vm.toggleLike()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.noteDetail?.isLiked == true)
        assertEquals(6, vm.uiState.value.noteDetail?.likeCount)

        vm.toggleLike()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.noteDetail?.isLiked == true)
        assertEquals(5, vm.uiState.value.noteDetail?.likeCount)

        vm.toggleLike()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.noteDetail?.isLiked == true)
        assertEquals(6, vm.uiState.value.noteDetail?.likeCount)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun toggleLike_networkError_revertsOptimisticUpdate() = runTest {
        coEvery { repository.likeNote(100L) } returns Result.Error(ErrorCode.NETWORK_ERROR, "网络错误")

        val vm = createViewModel()
        vm.toggleLike()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.noteDetail?.isLiked == true)
        assertEquals(5, vm.uiState.value.noteDetail?.likeCount)
    }

    /** noteDetail 为 null 时 toggleLike 不崩溃 */
    @Test
    fun toggleLike_nullDetail_doesNotCrash() = runTest {
        coEvery { repository.getNoteDetail(100L) } returns Result.Error(ErrorCode.NOTE_NOT_FOUND, "不存在")
        coEvery { repository.getComments(100L, 1) } returns Result.Success(testCommentsPage)
        val vm = createViewModel()
        // noteDetail is null, toggleLike should return early
        vm.toggleLike()
        testDispatcher.scheduler.advanceUntilIdle()
        // No crash
    }

    /** isTogglingLike 防止并发点赞 */
    @Test
    fun toggleLike_whileToggling_doesNotToggleAgain() = runTest {
        coEvery { repository.likeNote(100L) } coAnswers {
            kotlinx.coroutines.delay(5000)
            Result.Success(Unit)
        }
        val vm = createViewModel()
        vm.toggleLike()
        // isTogglingLike = true，再次调用应被阻止
        vm.toggleLike()
        testDispatcher.scheduler.advanceUntilIdle()

        // 仅调用一次
        coVerify(exactly = 1) { repository.likeNote(100L) }
    }

    // ================================================================
    // 关注 / 取关
    // ================================================================

    /** 关注作者成功 */
    @Test
    fun toggleAuthorFollow_notFollowing_success_updatesFollowed() = runTest {
        val vm = createViewModel()
        assertFalse(vm.uiState.value.noteDetail?.isAuthorFollowed == true)

        vm.toggleAuthorFollow()
        // 乐观更新
        assertTrue(vm.uiState.value.noteDetail?.isAuthorFollowed == true)
        assertTrue(vm.uiState.value.isAuthorFollowLoading)

        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isAuthorFollowLoading)
    }

    /** 取关作者成功 */
    @Test
    fun toggleAuthorFollow_following_success_unfollows() = runTest {
        val followedNote = testNoteDetail.copy(isAuthorFollowed = true)
        coEvery { repository.getNoteDetail(100L) } returns Result.Success(followedNote)

        val vm = createViewModel()
        assertTrue(vm.uiState.value.noteDetail?.isAuthorFollowed == true)

        vm.toggleAuthorFollow()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.noteDetail?.isAuthorFollowed == true)
    }

    /** 关注失败回滚 */
    @Test
    fun toggleAuthorFollow_error_revertsOptimisticUpdate() = runTest {
        coEvery { profileRepository.followUser(10L) } returns Result.Error(ErrorCode.USER_NOT_FOUND, "用户不存在")

        val vm = createViewModel()
        vm.toggleAuthorFollow()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.noteDetail?.isAuthorFollowed == true)
        assertNotNull(vm.uiState.value.error)
    }

    /** 自己的笔记不触发关注 */
    @Test
    fun toggleAuthorFollow_selfNote_doesNothing() = runTest {
        val selfNote = testNoteDetail.copy(isAuthorSelf = true)
        coEvery { repository.getNoteDetail(100L) } returns Result.Success(selfNote)

        val vm = createViewModel()
        vm.toggleAuthorFollow()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { profileRepository.followUser(any()) }
    }

    // ================================================================
    // 评论加载
    // ================================================================

    /** 评论加载失败 */
    @Test
    fun loadComments_error_setsError() = runTest {
        coEvery { repository.getComments(100L, 1) } returns Result.Error(ErrorCode.SERVER_ERROR, "服务错误")

        val vm = createViewModel()
        assertEquals("服务错误", vm.uiState.value.error)
        assertTrue(vm.uiState.value.comments.isEmpty())
    }

    /** 分页加载评论 */
    @Test
    fun loadComments_loadMore_appendsComments() = runTest {
        val vm = createViewModel()
        assertEquals(1, vm.uiState.value.comments.size)

        val page2 = PageData(
            items = listOf(testComment.copy(id = 300L)), page = 2, pageSize = 20, total = 10, hasMore = false
        )
        coEvery { repository.getComments(100L, 2) } returns Result.Success(page2)
        vm.loadComments(isRefresh = false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, vm.uiState.value.comments.size)
        assertFalse(vm.uiState.value.commentsHasMore)
    }

    /** 评论分页 hasMore=false 时不加载 */
    @Test
    fun loadComments_hasMoreFalse_doesNotLoad() = runTest {
        val noMorePage = PageData(
            items = listOf(testComment), page = 1, pageSize = 20, total = 5, hasMore = false
        )
        coEvery { repository.getComments(100L, 1) } returns Result.Success(noMorePage)

        val vm = createViewModel()
        assertFalse(vm.uiState.value.commentsHasMore)

        vm.loadComments(isRefresh = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // 不应调用第2页
        coVerify(exactly = 0) { repository.getComments(100L, 2) }
    }

    /** 刷新评论时重置到第1页 */
    @Test
    fun loadComments_refresh_replacesComments() = runTest {
        val vm = createViewModel()

        val refreshPage = PageData(
            items = listOf(testComment.copy(id = 999L)), page = 1, pageSize = 20, total = 20, hasMore = true
        )
        coEvery { repository.getComments(100L, 1) } returns Result.Success(refreshPage)
        vm.loadComments(isRefresh = true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.comments.size)
        assertEquals(999L, vm.uiState.value.comments[0].id)
    }

    // ================================================================
    // 发送评论
    // ================================================================

    /** 发送顶级评论成功时添加到列表头部 */
    @Test
    fun sendComment_topLevel_success_addsToHead() = runTest {
        val newComment = testComment.copy(id = 500L, content = "New comment")
        coEvery { repository.createComment(100L, "New comment", null, null) } returns Result.Success(newComment)

        val vm = createViewModel()
        val initialCount = vm.uiState.value.noteDetail?.commentCount ?: 0
        val initialCommentSize = vm.uiState.value.comments.size

        vm.sendComment("New comment")
        testDispatcher.scheduler.advanceUntilIdle()

        // 新评论被添加到头部，总数应为原有数量 + 1
        assertEquals(initialCommentSize + 1, vm.uiState.value.comments.size)
        assertEquals(500L, vm.uiState.value.comments[0].id) // 新评论在头部
        assertTrue(vm.uiState.value.commentSendSuccess)
        assertNull(vm.uiState.value.replyTarget)
        assertEquals(initialCount + 1, vm.uiState.value.noteDetail?.commentCount)
    }

    /** 发送回复评论成功时添加到对应评论的回复列表 */
    @Test
    fun sendComment_reply_success_addsToReplies() = runTest {
        val vm = createViewModel()
        // 设置回复目标
        vm.setReplyTarget(testComment)
        assertNotNull(vm.uiState.value.replyTarget)

        val replyComment = testComment.copy(id = 600L, content = "Reply!", parentId = 200L)
        coEvery {
            repository.createComment(100L, "Reply!", any(), any())
        } returns Result.Success(replyComment)

        vm.sendComment("Reply!")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.commentSendSuccess)
        assertNull(vm.uiState.value.replyTarget)
        // 检查 expandedReplies 包含该回复
        val replies = vm.uiState.value.expandedReplies[200L]
        assertNotNull(replies)
        assertEquals(1, replies?.size)
    }

    /** 发送空评论不执行 */
    @Test
    fun sendComment_blank_doesNotCallApi() = runTest {
        val vm = createViewModel()
        vm.sendComment("  ")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.createComment(any(), any(), any(), any()) }
    }

    /** 发送评论失败时显示错误 */
    @Test
    fun sendComment_error_setsError() = runTest {
        coEvery {
            repository.createComment(100L, "Bad comment", null, null)
        } returns Result.Error(ErrorCode.COMMENT_EMPTY, "评论不能为空")

        val vm = createViewModel()
        vm.sendComment("Bad comment")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isSendingComment)
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.commentSendSuccess)
    }

    // ================================================================
    // 删除评论
    // ================================================================

    /** 删除顶级评论成功时从列表移除 */
    @Test
    fun deleteComment_topLevel_success_removesFromList() = runTest {
        val vm = createViewModel()
        assertEquals(1, vm.uiState.value.comments.size)

        vm.deleteComment(200L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.comments.isEmpty())
        assertNull(vm.uiState.value.deletingCommentId)
    }

    /** 删除评论失败时显示错误 */
    @Test
    fun deleteComment_error_setsError() = runTest {
        coEvery { repository.deleteComment(100L, 200L) } returns Result.Error(ErrorCode.COMMENT_FORBIDDEN, "无权删除")

        val vm = createViewModel()
        vm.deleteComment(200L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.deletingCommentId)
        assertNotNull(vm.uiState.value.error)
    }

    /** 删除评论时清除长按菜单 */
    @Test
    fun deleteComment_clearsLongPressedMenu() = runTest {
        val vm = createViewModel()
        vm.showCommentMenu(testComment)
        assertNotNull(vm.uiState.value.longPressedComment)
        assertEquals(200L, vm.uiState.value.longPressedComment?.id)

        vm.deleteComment(200L)
        // deleteComment 在 viewModelScope.launch 中清除 longPressedComment，需推进协程
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.uiState.value.longPressedComment)
    }

    // ================================================================
    // 楼中楼回复加载
    // ================================================================

    /** 展开回复成功 */
    @Test
    fun expandReplies_success_loadsReplies() = runTest {
        val replies = listOf(
            testComment.copy(id = 700L, parentId = 200L, content = "Reply 1"),
            testComment.copy(id = 701L, parentId = 200L, content = "Reply 2")
        )
        coEvery { repository.getCommentReplies(100L, 200L) } returns Result.Success(replies)

        val vm = createViewModel()
        vm.expandReplies(200L)
        testDispatcher.scheduler.advanceUntilIdle()

        val expanded = vm.uiState.value.expandedReplies[200L]
        assertNotNull(expanded)
        assertEquals(2, expanded?.size)
        assertNull(vm.uiState.value.loadingReplies)
    }

    /** 再次点击折叠回复 */
    @Test
    fun expandReplies_alreadyExpanded_collapses() = runTest {
        val replies = listOf(testComment.copy(id = 700L, parentId = 200L))
        coEvery { repository.getCommentReplies(100L, 200L) } returns Result.Success(replies)

        val vm = createViewModel()
        vm.expandReplies(200L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.expandedReplies[200L])

        // 折叠
        vm.expandReplies(200L)
        assertNull(vm.uiState.value.expandedReplies[200L])
    }

    /** 展开回复失败 */
    @Test
    fun expandReplies_error_setsError() = runTest {
        coEvery { repository.getCommentReplies(100L, 200L) } returns Result.Error(50000, "服务器错误")

        val vm = createViewModel()
        vm.expandReplies(200L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.loadingReplies)
        assertNotNull(vm.uiState.value.error)
    }

    // ================================================================
    // 评论点赞 / 取消点赞
    // ================================================================

    /** 评论点赞成功时乐观更新 */
    @Test
    fun toggleCommentLike_notLiked_success_updatesLiked() = runTest {
        val vm = createViewModel()
        val comment = vm.uiState.value.comments[0]
        assertFalse(comment.liked)

        vm.toggleCommentLike(200L)
        // 乐观更新立即生效
        val updated = vm.uiState.value.comments.find { it.id == 200L }
        assertTrue(updated?.liked == true)
        assertEquals(3, updated?.likeCount) // 2 + 1
    }

    /** 评论取消点赞成功 */
    @Test
    fun toggleCommentLike_liked_success_unlikes() = runTest {
        val likedComment = testComment.copy(liked = true, likeCount = 5)
        val likedPage = PageData(
            items = listOf(likedComment), page = 1, pageSize = 20, total = 10, hasMore = true
        )
        coEvery { repository.getComments(100L, 1) } returns Result.Success(likedPage)

        val vm = createViewModel()
        vm.toggleCommentLike(200L)
        testDispatcher.scheduler.advanceUntilIdle()

        val updated = vm.uiState.value.comments.find { it.id == 200L }
        assertFalse(updated?.liked == true)
        assertEquals(4, updated?.likeCount)
    }

    /** 评论点赞失败回滚 */
    @Test
    fun toggleCommentLike_error_revertsLiked() = runTest {
        coEvery { repository.likeComment(100L, 200L) } returns Result.Error(ErrorCode.COMMENT_LIKE_ALREADY, "已点赞")

        val vm = createViewModel()
        vm.toggleCommentLike(200L)
        testDispatcher.scheduler.advanceUntilIdle()

        val updated = vm.uiState.value.comments.find { it.id == 200L }
        assertTrue(updated?.liked == true)
        assertEquals(3, updated?.likeCount)
        assertNotNull(vm.uiState.value.error)
    }

    /** 同一评论点赞请求未完成时，重复点击不应发起第二个请求 */
    @Test
    fun toggleCommentLike_whilePending_doesNotToggleAgain() = runTest {
        val pending = kotlinx.coroutines.CompletableDeferred<Result<Unit>>()
        coEvery { repository.likeComment(100L, 200L) } coAnswers {
            pending.await()
        }

        val vm = createViewModel()
        vm.toggleCommentLike(200L)
        vm.toggleCommentLike(200L)
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) { repository.likeComment(100L, 200L) }
        assertEquals(200L, vm.uiState.value.likingCommentId)

        pending.complete(Result.Success(Unit))
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.uiState.value.likingCommentId)
    }

    /** 找不到评论时不执行 */
    @Test
    fun toggleCommentLike_commentNotFound_doesNothing() = runTest {
        val vm = createViewModel()
        vm.toggleCommentLike(9999L) // 不存在的评论
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.likeComment(any(), any()) }
    }

    // ================================================================
    // ReplyTarget 管理
    // ================================================================

    /** setReplyTarget 正确设置回复目标 */
    @Test
    fun setReplyTarget_setsCorrectly() = runTest {
        val vm = createViewModel()
        vm.setReplyTarget(testComment)

        val target = vm.uiState.value.replyTarget
        assertNotNull(target)
        assertEquals(200L, target?.commentId)
        assertEquals("Commenter", target?.authorName)
        assertEquals(200L, target?.topLevelCommentId) // 顶级评论
    }

    /** clearReplyTarget 清除回复目标 */
    @Test
    fun clearReplyTarget_clears() = runTest {
        val vm = createViewModel()
        vm.setReplyTarget(testComment)
        assertNotNull(vm.uiState.value.replyTarget)

        vm.clearReplyTarget()
        assertNull(vm.uiState.value.replyTarget)
    }

    /** 楼中楼回复：顶级评论的子评论，topLevelCommentId 应为父评论ID */
    @Test
    fun setReplyTarget_nestedReply_usesTopLevelCommentId() = runTest {
        val parentComment = testComment.copy(
            id = 200L, replies = listOf(
                testComment.copy(id = 800L, parentId = 200L)
            )
        )
        val page = PageData(
            items = listOf(parentComment), page = 1, pageSize = 20, total = 5, hasMore = false
        )
        coEvery { repository.getComments(100L, 1) } returns Result.Success(page)

        val vm = createViewModel()
        // 回复子评论 800L
        vm.setReplyTarget(parentComment.replies[0])

        assertEquals(200L, vm.uiState.value.replyTarget?.topLevelCommentId)
    }

    // ================================================================
    // 删除笔记
    // ================================================================

    /** 删除自己的笔记成功 */
    @Test
    fun deleteNote_selfNote_success_setsDeleted() = runTest {
        val selfNote = testNoteDetail.copy(isAuthorSelf = true)
        coEvery { repository.getNoteDetail(100L) } returns Result.Success(selfNote)

        val vm = createViewModel()
        vm.deleteNote()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isDeleted)
        assertFalse(vm.uiState.value.isDeleting)
    }

    /** 删除笔记失败 */
    @Test
    fun deleteNote_error_setsError() = runTest {
        val selfNote = testNoteDetail.copy(isAuthorSelf = true)
        coEvery { repository.getNoteDetail(100L) } returns Result.Success(selfNote)
        coEvery { repository.deleteNote(100L) } returns Result.Error(ErrorCode.NOTE_FORBIDDEN, "无权删除")

        val vm = createViewModel()
        vm.deleteNote()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isDeleted)
        assertNotNull(vm.uiState.value.error)
    }

    /** 非自己的笔记不触发删除 */
    @Test
    fun deleteNote_notSelfNote_doesNothing() = runTest {
        val vm = createViewModel()
        vm.deleteNote()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.deleteNote(any()) }
    }

    // ================================================================
    // 长按菜单
    // ================================================================

    /** showCommentMenu 设置长按评论 */
    @Test
    fun showCommentMenu_setsComment() = runTest {
        val vm = createViewModel()
        vm.showCommentMenu(testComment)
        assertEquals(200L, vm.uiState.value.longPressedComment?.id)
    }

    /** dismissCommentMenu 清除长按评论 */
    @Test
    fun dismissCommentMenu_clearsComment() = runTest {
        val vm = createViewModel()
        vm.showCommentMenu(testComment)
        vm.dismissCommentMenu()
        assertNull(vm.uiState.value.longPressedComment)
    }

    // ================================================================
    // 工具方法
    // ================================================================

    /** commentSendSuccessConsumed 重置 flag */
    @Test
    fun commentSendSuccessConsumed_resetsFlag() = runTest {
        val vm = createViewModel()
        coEvery {
            repository.createComment(100L, "Hi", null, null)
        } returns Result.Success(testComment.copy(id = 888L))

        vm.sendComment("Hi")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.commentSendSuccess)

        vm.commentSendSuccessConsumed()
        assertFalse(vm.uiState.value.commentSendSuccess)
    }

    /** errorShown 清除错误 */
    @Test
    fun errorShown_clearsError() = runTest {
        coEvery { repository.getNoteDetail(100L) } returns Result.Error(50000, "错误")
        coEvery { repository.getComments(100L, 1) } returns Result.Success(testCommentsPage)

        val vm = createViewModel()
        assertNotNull(vm.uiState.value.error)

        vm.errorShown()
        assertNull(vm.uiState.value.error)
    }
}
