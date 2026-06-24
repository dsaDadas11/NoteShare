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

    init {
        loadNotifications()
        markAllAsRead()
    }

    fun loadNotifications() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = notificationRepository.getNotifications(page = 1)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        notifications = result.data.items,
                        isLoading = false,
                        currentPage = 1,
                        hasMore = result.data.hasMore
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoading || _uiState.value.isLoadingMore || !_uiState.value.hasMore) return
        val nextPage = _uiState.value.currentPage + 1
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            when (val result = notificationRepository.getNotifications(page = nextPage)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        notifications = _uiState.value.notifications + result.data.items,
                        isLoadingMore = false,
                        currentPage = nextPage,
                        hasMore = result.data.hasMore
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
            }
        }
    }

    private fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
        }
    }

    fun errorShown() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
