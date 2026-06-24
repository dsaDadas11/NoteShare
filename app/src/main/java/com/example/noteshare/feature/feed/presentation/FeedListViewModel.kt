package com.example.noteshare.feature.feed.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteshare.core.common.Result
import com.example.noteshare.feature.feed.data.FeedRepository
import com.example.noteshare.feature.feed.domain.model.NoteResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val notes: List<NoteResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val error: String? = null,
    val loadMoreFailed: Boolean = false
)

@HiltViewModel
class FeedListViewModel @Inject constructor(
    private val repository: FeedRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = repository.getNotes(1)) {
                is Result.Success -> {
                    val pageData = result.data
                    _uiState.update { state ->
                        state.copy(
                            notes = pageData.items,
                            currentPage = pageData.page,
                            hasMore = pageData.hasMore,
                            error = null,
                            loadMoreFailed = false,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(error = result.message, isLoading = false)
                    }
                }
                Result.Loading -> {}
            }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isLoadingMore || !currentState.hasMore || currentState.loadMoreFailed) return

        _uiState.update { it.copy(isLoadingMore = true, error = null) }
        val nextPage = currentState.currentPage + 1

        viewModelScope.launch {
            when (val result = repository.getNotes(nextPage)) {
                is Result.Success -> {
                    val pageData = result.data
                    _uiState.update { state ->
                        state.copy(
                            notes = state.notes + pageData.items,
                            currentPage = pageData.page,
                            hasMore = pageData.hasMore,
                            isLoadingMore = false,
                            loadMoreFailed = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoadingMore = false, error = result.message, loadMoreFailed = true)
                    }
                    // Reset loadMoreFailed after 3 seconds to allow retry
                    viewModelScope.launch {
                        delay(3000)
                        _uiState.update { it.copy(loadMoreFailed = false) }
                    }
                }
                Result.Loading -> {}
            }
        }
    }

    fun errorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
