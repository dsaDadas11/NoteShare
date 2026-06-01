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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val notes: List<NoteResponse> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val error: String? = null
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
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        
        viewModelScope.launch {
            when (val result = repository.getNotes(1)) {
                is Result.Success -> {
                    val pageData = result.data
                    _uiState.update { state ->
                        state.copy(
                            notes = pageData.items,
                            currentPage = pageData.page,
                            hasMore = pageData.hasMore,
                            isRefreshing = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(isRefreshing = false, error = result.message) 
                    }
                }
                else -> {}
            }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.isRefreshing || currentState.isLoadingMore || !currentState.hasMore) return
        
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
                            isLoadingMore = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(isLoadingMore = false, error = result.message) 
                    }
                }
                else -> {}
            }
        }
    }

    fun errorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
