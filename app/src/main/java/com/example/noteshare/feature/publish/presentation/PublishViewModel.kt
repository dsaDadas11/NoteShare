package com.example.noteshare.feature.publish.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteshare.core.common.Result
import com.example.noteshare.feature.publish.data.PublishRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PublishUiState(
    val title: String = "",
    val content: String = "",
    val selectedImages: List<Uri> = emptyList(),
    val selectedVideo: Uri? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class PublishViewModel @Inject constructor(
    private val repository: PublishRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublishUiState())
    val uiState: StateFlow<PublishUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) {
        if (title.length <= 100) {
            _uiState.update { it.copy(title = title) }
        }
    }

    fun updateContent(content: String) {
        if (content.length <= 5000) {
            _uiState.update { it.copy(content = content) }
        }
    }

    fun addImages(uris: List<Uri>) {
        _uiState.update { state ->
            val currentImages = state.selectedImages
            val newImages = (currentImages + uris).distinct()
            if (newImages.size > 3) {
                state.copy(selectedImages = newImages.take(3), selectedVideo = null, error = "最多只能选择 3 张图片")
            } else {
                state.copy(selectedImages = newImages, selectedVideo = null)
            }
        }
    }

    fun setVideo(uri: Uri) {
        _uiState.update { state ->
            state.copy(selectedVideo = uri, selectedImages = emptyList())
        }
    }

    fun removeVideo() {
        _uiState.update { state ->
            state.copy(selectedVideo = null)
        }
    }

    fun removeImage(uri: Uri) {
        _uiState.update { state ->
            state.copy(selectedImages = state.selectedImages - uri)
        }
    }

    fun publish() {
        val currentState = _uiState.value
        if (currentState.isLoading) return
        if (currentState.title.isBlank() || currentState.title.length > 100) {
            _uiState.update { it.copy(error = "标题长度必须在 1-100 之间") }
            return
        }
        if (currentState.content.isBlank() || currentState.content.length > 5000) {
            _uiState.update { it.copy(error = "正文长度必须在 1-5000 之间") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            // 1. Upload images
            val uploadedUrls = mutableListOf<String>()
            for (uri in currentState.selectedImages) {
                when (val uploadResult = repository.uploadImage(uri)) {
                    is Result.Success -> {
                        uploadedUrls.add(uploadResult.data)
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = "图片上传失败: ${uploadResult.message}") }
                        return@launch
                    }
                }
            }

            // 2. Upload video
            var uploadedVideoUrl: String? = null
            if (currentState.selectedVideo != null) {
                when (val uploadResult = repository.uploadVideo(currentState.selectedVideo)) {
                    is Result.Success -> {
                        uploadedVideoUrl = uploadResult.data
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = "视频上传失败: ${uploadResult.message}") }
                        return@launch
                    }
                }
            }

            // 3. Create note
            when (val result = repository.createNote(currentState.title, currentState.content, uploadedUrls, uploadedVideoUrl)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun errorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
