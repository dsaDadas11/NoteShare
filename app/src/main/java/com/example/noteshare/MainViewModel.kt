package com.example.noteshare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.datastore.TokenManager
import com.example.noteshare.core.network.NotificationWebSocketClient
import com.example.noteshare.core.network.TokenInterceptor
import com.example.noteshare.feature.notification.data.NotificationRepository
import com.example.noteshare.feature.profile.data.UserApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val tokenInterceptor: TokenInterceptor,
    private val userApi: UserApi,
    private val unauthorizedEventBus: UnauthorizedEventBus,
    private val webSocketClient: NotificationWebSocketClient,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<Boolean?>(null)
    val loginState: StateFlow<Boolean?> = _loginState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0L)
    val unreadCount: StateFlow<Long> = _unreadCount.asStateFlow()

    val unauthorizedEvents = unauthorizedEventBus.events

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.firstOrNull()
            if (token.isNullOrEmpty()) {
                _loginState.value = false
            } else {
                val valid = validateToken()
                _loginState.value = valid
                if (!valid) {
                    tokenManager.clearToken()
                    tokenInterceptor.invalidateCache()
                } else {
                    // 登录成功后连接 WebSocket 并获取未读数
                    connectWebSocket()
                    fetchUnreadCount()
                }
            }
        }
    }

    private suspend fun validateToken(): Boolean {
        return try {
            val response = userApi.getMyProfile()
            response.code == ErrorCode.SUCCESS && response.data != null
        } catch (_: Exception) {
            true
        }
    }

    private fun connectWebSocket() {
        webSocketClient.connect()
        viewModelScope.launch {
            webSocketClient.notifications.collect {
                // 收到推送时未读数 +1
                _unreadCount.value = _unreadCount.value + 1
            }
        }
    }

    private fun fetchUnreadCount() {
        viewModelScope.launch {
            notificationRepository.getUnreadCount()
                .onSuccess { count ->
                    _unreadCount.value = count.toLong()
                }
        }
    }

    /** 进入通知页后调用，清零未读数 */
    fun clearUnreadCount() {
        _unreadCount.value = 0
    }

    fun logout() {
        viewModelScope.launch {
            webSocketClient.disconnect()
            tokenManager.clearToken()
            tokenInterceptor.invalidateCache()
            _loginState.value = false
            _unreadCount.value = 0
        }
    }
}
