package com.example.noteshare.feature.notification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteshare.core.common.Result
import com.example.noteshare.feature.notification.data.NotificationRepository
import com.example.noteshare.feature.notification.domain.model.NotificationResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val notifications: List<NotificationResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = true
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private var requestGeneration = 0L

    init {
        loadNotifications()
        markAllAsRead()
    }

    fun loadNotifications() {
        if (_uiState.value.isLoading) return
        val generation = ++requestGeneration
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, isLoadingMore = false, error = null)
            }
            when (val result = notificationRepository.getNotifications(page = 1)) {
                is Result.Success -> {
                    if (generation != requestGeneration) return@launch
                    _uiState.update {
                        it.copy(
                            notifications = result.data.items,
                            isLoading = false,
                            currentPage = 1,
                            hasMore = result.data.hasMore
                        )
                    }
                }
                is Result.Error -> {
                    if (generation != requestGeneration) return@launch
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoading || _uiState.value.isLoadingMore || !_uiState.value.hasMore) return
        val nextPage = _uiState.value.currentPage + 1
        val generation = requestGeneration
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            when (val result = notificationRepository.getNotifications(page = nextPage)) {
                is Result.Success -> {
                    if (generation != requestGeneration) return@launch
                    _uiState.update {
                        it.copy(
                            notifications = it.notifications + result.data.items,
                            isLoadingMore = false,
                            currentPage = result.data.page,
                            hasMore = result.data.hasMore
                        )
                    }
                }
                is Result.Error -> {
                    if (generation != requestGeneration) return@launch
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            }
        }
    }

    fun errorShown() {
        _uiState.update { it.copy(error = null) }
    }

    private fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
        }
    }
}
