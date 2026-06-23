package com.example.noteshare.feature.profile.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteshare.core.common.Result
import com.example.noteshare.feature.profile.data.ProfileRepository
import com.example.noteshare.feature.profile.domain.model.UpdateProfileRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val username: String = "",
    val nickname: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val newAvatarUri: Uri? = null,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            when (val result = repository.getMyProfile()) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            username = result.data.username,
                            nickname = result.data.nickname ?: "",
                            bio = result.data.bio ?: "",
                            avatarUrl = result.data.avatarUrl
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = "加载资料失败: ${result.message}") }
                }
                Result.Loading -> {}
            }
        }
    }

    fun updateNickname(nickname: String) {
        if (nickname.length <= 50) {
            _uiState.update { it.copy(nickname = nickname) }
        }
    }

    fun updateBio(bio: String) {
        if (bio.length <= 500) {
            _uiState.update { it.copy(bio = bio) }
        }
    }

    fun updateAvatar(uri: Uri) {
        _uiState.update { it.copy(newAvatarUri = uri) }
    }

    fun saveProfile() {
        val currentState = _uiState.value
        if (currentState.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            
            var finalAvatarUrl = currentState.avatarUrl
            
            // Upload new avatar if selected
            currentState.newAvatarUri?.let { uri ->
                when (val uploadResult = repository.uploadAvatar(uri)) {
                    is Result.Success -> {
                        finalAvatarUrl = uploadResult.data
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(isSaving = false, error = "头像上传失败: ${uploadResult.message}") }
                        return@launch
                    }
                    Result.Loading -> {}
                }
            }
            
            val request = UpdateProfileRequest(
                nickname = currentState.nickname.ifBlank { null },
                avatarUrl = finalAvatarUrl,
                bio = currentState.bio.ifBlank { null }
            )

            when (val result = repository.updateProfile(request)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, isSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = result.message) }
                }
                Result.Loading -> {}
            }
        }
    }

    fun errorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
