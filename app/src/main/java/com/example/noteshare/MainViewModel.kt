package com.example.noteshare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteshare.core.datastore.TokenManager
import com.example.noteshare.core.network.TokenInterceptor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val tokenInterceptor: TokenInterceptor
) : ViewModel() {
    val isUserLoggedIn = tokenManager.tokenFlow.map { !it.isNullOrEmpty() }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearToken()
            tokenInterceptor.invalidateCache()
        }
    }
}
