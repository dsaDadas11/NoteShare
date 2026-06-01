package com.example.noteshare.feature.profile.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteshare.core.common.Result
import com.example.noteshare.feature.feed.domain.model.NoteResponse
import com.example.noteshare.feature.profile.data.ProfileRepository
import com.example.noteshare.feature.profile.domain.model.UserProfileResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: UserProfileResponse? = null,
    val notes: List<NoteResponse> = emptyList(),
    val notesLoading: Boolean = false,
    val notesCurrentPage: Int = 1,
    val notesHasMore: Boolean = true,
    val error: String? = null,
    val isMyProfile: Boolean = true,
    val isFollowLoading: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val targetUserId: Long? = savedStateHandle.get<String>("userId")?.toLongOrNull()

    private val _uiState = MutableStateFlow(ProfileUiState(isMyProfile = targetUserId == null))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            if (targetUserId == null) {
                // 我的主页：只发一个请求
                when (val result = repository.getMyProfile()) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                profile = result.data,
                                isMyProfile = true
                            )
                        }
                        loadNotes(isRefresh = true)
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                    else -> {}
                }
            } else {
                // 他人主页：也只发一个请求
                when (val result = repository.getUserProfile(targetUserId)) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                profile = result.data,
                                isMyProfile = false
                            )
                        }
                        loadNotes(isRefresh = true)
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadNotes(isRefresh: Boolean = false) {
        val currentProfile = _uiState.value.profile ?: return
        if (isRefresh) {
            _uiState.update { it.copy(notesLoading = true) }
        } else {
            if (_uiState.value.notesLoading || !_uiState.value.notesHasMore) return
            _uiState.update { it.copy(notesLoading = true) }
        }

        val pageToLoad = if (isRefresh) 1 else _uiState.value.notesCurrentPage + 1

        viewModelScope.launch {
            when (val result = repository.getUserNotes(currentProfile.id, pageToLoad)) {
                is Result.Success -> {
                    val pageData = result.data
                    _uiState.update { state ->
                        state.copy(
                            notes = if (isRefresh) pageData.items else state.notes + pageData.items,
                            notesCurrentPage = pageData.page,
                            notesHasMore = pageData.hasMore,
                            notesLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(notesLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun toggleFollow() {
        val profile = _uiState.value.profile ?: return
        if (_uiState.value.isMyProfile || _uiState.value.isFollowLoading) return
        val currentlyFollowing = profile.isFollowing == true

        // Optimistic
        _uiState.update { state ->
            val updated = state.profile?.copy(
                isFollowing = !currentlyFollowing,
                followerCount = state.profile.followerCount + if (currentlyFollowing) -1 else 1
            )
            state.copy(profile = updated, isFollowLoading = true)
        }

        viewModelScope.launch {
            val result = if (currentlyFollowing) repository.unfollowUser(profile.id) else repository.followUser(profile.id)
            if (result is Result.Error) {
                // Revert
                _uiState.update { state ->
                    val reverted = state.profile?.copy(
                        isFollowing = currentlyFollowing,
                        followerCount = state.profile.followerCount + if (currentlyFollowing) 1 else -1
                    )
                    state.copy(profile = reverted, isFollowLoading = false, error = "操作失败: ${result.message}")
                }
            } else {
                _uiState.update { it.copy(isFollowLoading = false) }
            }
        }
    }

    fun errorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
