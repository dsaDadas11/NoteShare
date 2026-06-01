package com.example.noteshare.feature.feed.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.core.network.ApiResponse
import com.example.noteshare.feature.feed.data.NoteDetailRepository
import com.example.noteshare.feature.feed.domain.model.CommentResponse
import com.example.noteshare.feature.feed.domain.model.NoteDetailResponse
import com.example.noteshare.feature.profile.data.UserApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val isDeleted: Boolean = false
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val repository: NoteDetailRepository,
    private val userApi: UserApi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    val noteId: Long = savedStateHandle.get<String>("noteId")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    init {
        if (noteId != 0L) {
            loadNoteDetail()
            loadComments(isRefresh = true)
        } else {
            _uiState.update { it.copy(error = "无效的笔记 ID") }
        }
    }

    fun loadNoteDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getNoteDetail(noteId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, noteDetail = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
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

        viewModelScope.launch {
            when (val result = repository.getComments(noteId, pageToLoad)) {
                is Result.Success -> {
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
                    _uiState.update { it.copy(commentsLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun toggleLike() {
        val detail = _uiState.value.noteDetail ?: return
        val currentlyLiked = detail.isLiked
        
        // Optimistic update
        _uiState.update { state ->
            val updatedDetail = state.noteDetail?.copy(
                isLiked = !currentlyLiked,
                likeCount = state.noteDetail.likeCount + if (currentlyLiked) -1 else 1
            )
            state.copy(noteDetail = updatedDetail)
        }

        viewModelScope.launch {
            val result = if (currentlyLiked) repository.unlikeNote(noteId) else repository.likeNote(noteId)
            if (result is Result.Error) {
                // Revert on failure
                _uiState.update { state ->
                    val revertedDetail = state.noteDetail?.copy(
                        isLiked = currentlyLiked,
                        likeCount = state.noteDetail.likeCount + if (currentlyLiked) 1 else -1
                    )
                    state.copy(noteDetail = revertedDetail, error = "操作失败: ${result.message}")
                }
            }
        }
    }

    fun toggleAuthorFollow() {
        val detail = _uiState.value.noteDetail ?: return
        if (detail.isAuthorSelf || _uiState.value.isAuthorFollowLoading) return
        val currentlyFollowing = detail.isAuthorFollowed

        _uiState.update { state ->
            state.copy(
                noteDetail = state.noteDetail?.copy(isAuthorFollowed = !currentlyFollowing),
                isAuthorFollowLoading = true
            )
        }

        viewModelScope.launch {
            val result = if (currentlyFollowing) {
                try {
                    val response = userApi.unfollowUser(detail.author.id)
                    if (response.code == ErrorCode.SUCCESS) Result.Success(Unit)
                    else Result.Error(response.code, response.message)
                } catch (e: Exception) {
                    Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
                }
            } else {
                try {
                    val response = userApi.followUser(detail.author.id)
                    if (response.code == ErrorCode.SUCCESS) Result.Success(Unit)
                    else Result.Error(response.code, response.message)
                } catch (e: Exception) {
                    Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
                }
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
                else -> {}
            }
        }
    }

    fun sendComment(content: String) {
        if (content.isBlank() || _uiState.value.isSendingComment) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingComment = true, commentSendSuccess = false) }
            when (val result = repository.createComment(noteId, content)) {
                is Result.Success -> {
                    // Prepend comment and update count
                    _uiState.update { state ->
                        val updatedDetail = state.noteDetail?.copy(
                            commentCount = state.noteDetail.commentCount + 1
                        )
                        state.copy(
                            comments = listOf(result.data) + state.comments,
                            noteDetail = updatedDetail,
                            isSendingComment = false,
                            commentSendSuccess = true
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSendingComment = false, error = "发送失败: ${result.message}") }
                }
                else -> {}
            }
        }
    }

    fun commentSendSuccessConsumed() {
        _uiState.update { it.copy(commentSendSuccess = false) }
    }

    fun errorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
