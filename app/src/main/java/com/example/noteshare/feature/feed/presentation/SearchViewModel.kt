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

data class SearchUiState(
    val keyword: String = "",
    val results: List<NoteResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val error: String? = null,
    val hasSearched: Boolean = false,
    val loadMoreFailed: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: FeedRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun updateKeyword(keyword: String) {
        _uiState.update { it.copy(keyword = keyword) }
    }

    fun clearKeyword() {
        _uiState.update { it.copy(keyword = "", results = emptyList(), hasSearched = false, error = null) }
    }

    fun search() {
        val keyword = _uiState.value.keyword.trim()
        if (keyword.isEmpty()) return

        _uiState.update { 
            it.copy(
                isLoading = true, 
                hasSearched = true, 
                error = null, 
                results = emptyList(), 
                currentPage = 1,
                hasMore = true
            ) 
        }

        viewModelScope.launch {
            when (val result = repository.searchNotes(keyword, 1)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            results = result.data.items,
                            currentPage = result.data.page,
                            hasMore = result.data.hasMore
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isLoadingMore || !currentState.hasMore || currentState.loadMoreFailed) return

        _uiState.update { it.copy(isLoadingMore = true, error = null) }
        val nextPage = currentState.currentPage + 1

        viewModelScope.launch {
            when (val result = repository.searchNotes(currentState.keyword.trim(), nextPage)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoadingMore = false,
                            results = state.results + result.data.items,
                            currentPage = result.data.page,
                            hasMore = result.data.hasMore,
                            loadMoreFailed = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingMore = false, error = result.message, loadMoreFailed = true) }
                    // Reset loadMoreFailed after 3 seconds to allow retry
                    viewModelScope.launch {
                        delay(3000)
                        _uiState.update { it.copy(loadMoreFailed = false) }
                    }
                }
            }
        }
    }
}
