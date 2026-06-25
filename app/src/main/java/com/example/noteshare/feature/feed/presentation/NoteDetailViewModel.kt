package com.example.noteshare.feature.feed.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.feature.feed.data.NoteDetailRepository
import com.example.noteshare.feature.feed.domain.model.CommentResponse
import com.example.noteshare.feature.feed.domain.model.NoteDetailResponse
import com.example.noteshare.feature.profile.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 当前正在回复的评论信息 */
data class ReplyTarget(
    val commentId: Long,
    val authorName: String,
    /** 顶层评论 ID（楼中楼：回复的评论所属的顶级评论） */
    val topLevelCommentId: Long,
    /** 被回复作者昵称（传递给后端用于展示 "回复xxx："） */
    val replyToAuthor: String? = null
)

data class NoteDetailUiState(
    val isLoading: Boolean = false,
    val noteDetail: NoteDetailResponse? = null,
    val comments: List<CommentResponse> = emptyList(),
    val commentsLoading: Boolean = false,
    val commentCurrentPage: Int = 1,
    val commentsHasMore: Boolean = true,
    val error: String? = null,
    val isSendingComment: Boolean = false,
    val commentSendSuccess: Boolean = false,
    val isAuthorFollowLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val deletingCommentId: Long? = null,
    // 回复相关
    val replyTarget: ReplyTarget? = null,
    val expandedReplies: Map<Long, List<CommentResponse>> = emptyMap(),
    val loadingReplies: Long? = null,
    // 评论点赞
    val likingCommentId: Long? = null,
    // 长按菜单
    val longPressedComment: CommentResponse? = null,
    // 防止并发点赞切换
    val isTogglingLike: Boolean = false
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val repository: NoteDetailRepository,
    private val profileRepository: ProfileRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val noteId: Long = savedStateHandle.get<String>("noteId")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    private var noteDetailRequestGeneration = 0L
    private var likeToggleGeneration = 0L
    private var commentsRequestGeneration = 0L
    private val repliesRequestGeneration = mutableMapOf<Long, Long>()
    private val pendingCommentLikeIds = mutableSetOf<Long>()

    init {
        if (noteId != 0L) {
            loadNoteDetail()
            loadComments(isRefresh = true)
        } else {
            _uiState.update { it.copy(error = "无效的笔记 ID") }
        }
    }

    fun loadNoteDetail() {
        val generation = ++noteDetailRequestGeneration
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getNoteDetail(noteId)) {
                is Result.Success -> {
                    if (generation != noteDetailRequestGeneration) return@launch
                    _uiState.update { it.copy(isLoading = false, noteDetail = result.data) }
                }
                is Result.Error -> {
                    if (generation != noteDetailRequestGeneration) return@launch
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun loadComments(isRefresh: Boolean = false) {
        if (isRefresh) {
            _uiState.update { it.copy(commentsLoading = true) }
        } else {
            if (_uiState.value.commentsLoading || !_uiState.value.commentsHasMore) return
            _uiState.update { it.copy(commentsLoading = true) }
        }

        val pageToLoad = if (isRefresh) 1 else _uiState.value.commentCurrentPage + 1
        val generation = ++commentsRequestGeneration

        viewModelScope.launch {
            when (val result = repository.getComments(noteId, pageToLoad)) {
                is Result.Success -> {
                    if (generation != commentsRequestGeneration) return@launch
                    val pageData = result.data
                    _uiState.update { state ->
                        state.copy(
                            comments = if (isRefresh) pageData.items else state.comments + pageData.items,
                            commentCurrentPage = pageData.page,
                            commentsHasMore = pageData.hasMore,
                            commentsLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    if (generation != commentsRequestGeneration) return@launch
                    _uiState.update { it.copy(commentsLoading = false, error = result.message) }
                }
            }
        }
    }

    fun toggleLike() {
        val snapshot = _uiState.value.noteDetail ?: return
        if (_uiState.value.isTogglingLike) return
        val currentlyLiked = snapshot.isLiked
        val generation = ++likeToggleGeneration

        _uiState.update { state ->
            val detail = state.noteDetail ?: return@update state
            state.copy(
                noteDetail = detail.copy(
                    isLiked = !currentlyLiked,
                    likeCount = maxOf(0, detail.likeCount + if (currentlyLiked) -1 else 1)
                ),
                isTogglingLike = true,
                error = null
            )
        }

        viewModelScope.launch {
            val result = if (currentlyLiked) repository.unlikeNote(noteId) else repository.likeNote(noteId)
            if (generation != likeToggleGeneration) {
                _uiState.update { it.copy(isTogglingLike = false) }
                return@launch
            }

            when {
                result is Result.Success || isIdempotentLikeError(result) -> {
                    syncNoteDetailAfterLikeToggle(generation)
                }
                result is Result.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            noteDetail = snapshot,
                            isTogglingLike = false,
                            error = "操作失败: ${result.message}"
                        )
                    }
                }
            }
        }
    }

    private suspend fun syncNoteDetailAfterLikeToggle(toggleGeneration: Long) {
        when (val result = repository.getNoteDetail(noteId)) {
            is Result.Success -> {
                if (toggleGeneration != likeToggleGeneration) return
                _uiState.update { it.copy(noteDetail = result.data, isTogglingLike = false) }
            }
            is Result.Error -> {
                if (toggleGeneration != likeToggleGeneration) return
                _uiState.update { it.copy(isTogglingLike = false, error = result.message) }
            }
        }
    }

    fun toggleAuthorFollow() {
        val detail = _uiState.value.noteDetail ?: return
        if (detail.isAuthorSelf || _uiState.value.isAuthorFollowLoading) return
        val currentlyFollowing = detail.isAuthorFollowed
        noteDetailRequestGeneration++

        _uiState.update { state ->
            state.copy(
                noteDetail = state.noteDetail?.copy(isAuthorFollowed = !currentlyFollowing),
                isAuthorFollowLoading = true
            )
        }

        viewModelScope.launch {
            val result = if (currentlyFollowing) {
                profileRepository.unfollowUser(detail.author.id)
            } else {
                profileRepository.followUser(detail.author.id)
            }

            _uiState.update { state ->
                if (result is Result.Error) {
                    state.copy(
                        noteDetail = state.noteDetail?.copy(isAuthorFollowed = currentlyFollowing),
                        isAuthorFollowLoading = false,
                        error = "操作失败: ${result.message}"
                    )
                } else {
                    state.copy(isAuthorFollowLoading = false)
                }
            }
        }
    }

    fun deleteNote() {
        val detail = _uiState.value.noteDetail ?: return
        if (!detail.isAuthorSelf || _uiState.value.isDeleting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null) }
            when (val result = repository.deleteNote(noteId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isDeleting = false, isDeleted = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isDeleting = false, error = "删除失败: ${result.message}") }
                }
            }
        }
    }

    // ==================== 发送评论 / 回复 ====================

    /** 设置回复目标 */
    fun setReplyTarget(comment: CommentResponse) {
        val name = comment.user.nickname ?: comment.user.username
        // 楼中楼：找到该评论所属的顶级评论 ID
        val topLevelId = findTopLevelCommentId(comment.id)
        _uiState.update {
            it.copy(replyTarget = ReplyTarget(
                commentId = comment.id,
                authorName = name,
                topLevelCommentId = topLevelId,
                replyToAuthor = name
            ))
        }
    }

    /** 查找评论所属的顶级评论 ID */
    private fun findTopLevelCommentId(commentId: Long): Long {
        val state = _uiState.value
        // 如果 commentId 本身就是顶级评论
        state.comments.find { it.id == commentId }?.let { return it.id }
        // 在顶级评论的回复列表中查找
        state.comments.forEach { topComment ->
            if (topComment.replies.any { it.id == commentId }) return topComment.id
        }
        // 在展开回复中查找
        for ((topId, replies) in state.expandedReplies) {
            if (replies.any { it.id == commentId }) return topId
        }
        // 兜底：如果评论是顶级评论的 parentId 指向（回复列表中的评论的 parentId 指向顶级评论）
        val allReplies = state.comments.flatMap { it.replies } +
            state.expandedReplies.values.flatten()
        allReplies.find { it.id == commentId }?.let { reply ->
            // 该回复的 parentId 就是顶级评论 ID
            reply.parentId?.let { return it }
        }
        return commentId
    }

    /** 清除回复目标 */
    fun clearReplyTarget() {
        _uiState.update { it.copy(replyTarget = null) }
    }

    fun sendComment(content: String) {
        if (content.isBlank() || _uiState.value.isSendingComment) return

        val replyTarget = _uiState.value.replyTarget
        // 楼中楼：始终使用顶级评论 ID 作为 parentId，确保回复列表正确加载
        val parentId = replyTarget?.topLevelCommentId
        val replyToAuthor = replyTarget?.replyToAuthor

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingComment = true, commentSendSuccess = false) }
            when (val result = repository.createComment(noteId, content, parentId, replyToAuthor)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        val updatedDetail = state.noteDetail?.copy(
                            commentCount = state.noteDetail.commentCount + 1
                        )
                        if (parentId == null) {
                            // 顶级评论：添加到列表头部
                            state.copy(
                                comments = listOf(result.data) + state.comments,
                                noteDetail = updatedDetail,
                                isSendingComment = false,
                                commentSendSuccess = true,
                                replyTarget = null
                            )
                        } else {
                            // 回复：添加到对应评论的回复列表中
                            val updatedComments = state.comments.map { comment ->
                                if (comment.id == parentId) {
                                    comment.copy(
                                        replies = comment.replies + result.data,
                                        replyCount = comment.replyCount + 1
                                    )
                                } else comment
                            }
                            // 同时更新展开回复列表
                            val updatedExpanded = state.expandedReplies.toMutableMap()
                            updatedExpanded[parentId] = (updatedExpanded[parentId] ?: emptyList()) + result.data
                            state.copy(
                                comments = updatedComments,
                                expandedReplies = updatedExpanded,
                                noteDetail = updatedDetail,
                                isSendingComment = false,
                                commentSendSuccess = true,
                                replyTarget = null
                            )
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSendingComment = false, error = "发送失败: ${result.message}") }
                }
            }
        }
    }

    // ==================== 删除评论 ====================

    fun deleteComment(commentId: Long) {
        if (_uiState.value.deletingCommentId != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(deletingCommentId = commentId, error = null, longPressedComment = null) }
            when (val result = repository.deleteComment(noteId, commentId)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        // 判断是顶级评论还是回复
                        val isTopLevel = state.comments.any { it.id == commentId }
                        if (isTopLevel) {
                            val updatedDetail = state.noteDetail?.copy(
                                commentCount = maxOf(0, state.noteDetail.commentCount - 1)
                            )
                            state.copy(
                                comments = state.comments.filterNot { it.id == commentId },
                                expandedReplies = state.expandedReplies - commentId,
                                noteDetail = updatedDetail,
                                deletingCommentId = null
                            )
                        } else {
                            // 是回复：从父评论的回复列表或展开回复中移除
                            // 先在 comments.replies 中查找父评论
                            val parentId = state.comments.find { c ->
                                c.replies.any { it.id == commentId }
                            }?.id
                                // 再在 expandedReplies 中查找父评论
                                ?: state.expandedReplies.entries.find { (_, replies) ->
                                    replies.any { it.id == commentId }
                                }?.key

                            val updatedComments = if (parentId != null) {
                                state.comments.map { comment ->
                                    if (comment.id == parentId) {
                                        comment.copy(
                                            replies = comment.replies.filterNot { it.id == commentId },
                                            replyCount = maxOf(0, comment.replyCount - 1)
                                        )
                                    } else comment
                                }
                            } else state.comments

                            // 从所有展开回复中移除被删除的评论
                            val updatedExpanded = state.expandedReplies.mapValues { (_, replies) ->
                                replies.filterNot { it.id == commentId }
                            }

                            val updatedDetail = state.noteDetail?.copy(
                                commentCount = maxOf(0, state.noteDetail.commentCount - 1)
                            )
                            state.copy(
                                comments = updatedComments,
                                expandedReplies = updatedExpanded,
                                noteDetail = updatedDetail,
                                deletingCommentId = null
                            )
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(deletingCommentId = null, error = "删除评论失败: ${result.message}") }
                }
            }
        }
    }

    // ==================== 展开回复 ====================

    fun expandReplies(commentId: Long) {
        // 如果已展开则折叠
        if (_uiState.value.expandedReplies.containsKey(commentId)) {
            repliesRequestGeneration[commentId] = (repliesRequestGeneration[commentId] ?: 0L) + 1L
            _uiState.update { state ->
                state.copy(expandedReplies = state.expandedReplies - commentId)
            }
            return
        }
        if (_uiState.value.loadingReplies == commentId) return

        val generation = (repliesRequestGeneration[commentId] ?: 0L) + 1L
        repliesRequestGeneration[commentId] = generation

        // 加载全部回复
        viewModelScope.launch {
            _uiState.update { it.copy(loadingReplies = commentId) }
            when (val result = repository.getCommentReplies(noteId, commentId)) {
                is Result.Success -> {
                    if (repliesRequestGeneration[commentId] != generation) return@launch
                    _uiState.update { state ->
                        state.copy(
                            expandedReplies = state.expandedReplies + (commentId to result.data),
                            loadingReplies = null
                        )
                    }
                }
                is Result.Error -> {
                    if (repliesRequestGeneration[commentId] != generation) return@launch
                    _uiState.update { it.copy(loadingReplies = null, error = "加载回复失败: ${result.message}") }
                }
            }
        }
    }

    // ==================== 评论点赞 ====================

    fun toggleCommentLike(commentId: Long) {
        // 找到评论
        val comment = findComment(commentId) ?: return
        if (!pendingCommentLikeIds.add(commentId)) return
        val currentlyLiked = comment.liked

        // 乐观更新
        _uiState.update { it.copy(likingCommentId = commentId) }
        updateCommentOptimistic(commentId, !currentlyLiked, maxOf(0, comment.likeCount + if (currentlyLiked) -1 else 1))

        viewModelScope.launch {
            val result = if (currentlyLiked) {
                repository.unlikeComment(noteId, commentId)
            } else {
                repository.likeComment(noteId, commentId)
            }
            if (result is Result.Error) {
                val (resolvedLiked, resolvedCount) = resolveCommentLikeStateAfterError(
                    currentlyLiked = currentlyLiked,
                    originalLikeCount = comment.likeCount,
                    errorCode = result.code
                )
                updateCommentOptimistic(commentId, resolvedLiked, resolvedCount)
                _uiState.update { it.copy(error = "操作失败: ${result.message}") }
            }
            pendingCommentLikeIds.remove(commentId)
            _uiState.update {
                it.copy(likingCommentId = pendingCommentLikeIds.firstOrNull())
            }
        }
    }

    private fun findComment(commentId: Long): CommentResponse? {
        val state = _uiState.value
        // 先在顶级评论中找
        state.comments.find { it.id == commentId }?.let { return it }
        // 再在回复中找
        state.comments.forEach { c ->
            c.replies.find { it.id == commentId }?.let { return it }
        }
        state.expandedReplies.values.forEach { replies ->
            replies.find { it.id == commentId }?.let { return it }
        }
        return null
    }

    private fun updateCommentOptimistic(commentId: Long, liked: Boolean, likeCount: Int) {
        _uiState.update { state ->
            val updatedComments = state.comments.map { comment ->
                if (comment.id == commentId) {
                    comment.copy(liked = liked, likeCount = likeCount)
                } else {
                    comment.copy(replies = comment.replies.map { reply ->
                        if (reply.id == commentId) reply.copy(liked = liked, likeCount = likeCount)
                        else reply
                    })
                }
            }
            val updatedExpanded = state.expandedReplies.mapValues { (_, replies) ->
                replies.map { reply ->
                    if (reply.id == commentId) reply.copy(liked = liked, likeCount = likeCount)
                    else reply
                }
            }
            state.copy(comments = updatedComments, expandedReplies = updatedExpanded)
        }
    }

    // ==================== 长按菜单 ====================

    fun showCommentMenu(comment: CommentResponse) {
        _uiState.update { it.copy(longPressedComment = comment) }
    }

    fun dismissCommentMenu() {
        _uiState.update { it.copy(longPressedComment = null) }
    }

    // ==================== 工具方法 ====================

    fun commentSendSuccessConsumed() {
        _uiState.update { it.copy(commentSendSuccess = false) }
    }

    fun errorShown() {
        _uiState.update { it.copy(error = null) }
    }
}

private fun isIdempotentLikeError(result: Result<*>): Boolean {
    return result is Result.Error && (
        result.code == ErrorCode.ALREADY_LIKED || result.code == ErrorCode.NOT_LIKED
    )
}

private fun resolveCommentLikeStateAfterError(
    currentlyLiked: Boolean,
    originalLikeCount: Int,
    errorCode: Int
): Pair<Boolean, Int> = when (errorCode) {
    ErrorCode.COMMENT_LIKE_ALREADY -> true to maxOf(0, originalLikeCount + 1)
    ErrorCode.COMMENT_LIKE_NOT_FOUND -> false to maxOf(0, originalLikeCount - 1)
    else -> currentlyLiked to maxOf(0, originalLikeCount)
}
