package com.example.noteshare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.core.datastore.TokenManager
import com.example.noteshare.core.network.TokenInterceptor
import com.example.noteshare.core.network.UnauthorizedEventBus
import com.example.noteshare.feature.notification.data.NotificationRepository
import com.example.noteshare.feature.profile.data.UserApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val tokenInterceptor: TokenInterceptor,
    private val userApi: UserApi,
    private val unauthorizedEventBus: UnauthorizedEventBus,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<Boolean?>(null)
    val loginState: StateFlow<Boolean?> = _loginState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0L)
    val unreadCount: StateFlow<Long> = _unreadCount.asStateFlow()

    val unauthorizedEvents = unauthorizedEventBus.events

    private var pollingJob: Job? = null

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
                    fetchUnreadCount()
                    startPollingUnreadCount()
                }
            }
        }
    }

    private suspend fun validateToken(): Boolean {
        return try {
            val response = userApi.getMyProfile()
            response.code == ErrorCode.SUCCESS && response.data != null
        } catch (e: HttpException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun fetchUnreadCount() {
        viewModelScope.launch {
            when (val result = notificationRepository.getUnreadCount()) {
                is Result.Success -> {
                    _unreadCount.value = result.data.toLong()
                }
                is Result.Error -> { /* ignore */ }
            }
        }
    }

    /** 每 5 秒轮询未读数，自动取消上一次轮询 */
    private fun startPollingUnreadCount() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(5_000)
                fetchUnreadCount()
            }
        }
    }

    /** 进入通知页后调用，清零未读数 */
    fun clearUnreadCount() {
        _unreadCount.value = 0
    }

    /** 登录成功后调用，刷新登录状态并获取未读数 */
    fun onLoginSuccess() {
        _loginState.value = true
        fetchUnreadCount()
        startPollingUnreadCount()
    }

    fun logout() {
        pollingJob?.cancel()
        pollingJob = null
        viewModelScope.launch {
            tokenManager.clearToken()
            tokenInterceptor.invalidateCache()
            _loginState.value = false
            _unreadCount.value = 0
        }
    }
}
