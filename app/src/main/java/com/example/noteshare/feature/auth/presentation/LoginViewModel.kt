package com.example.noteshare.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.feature.auth.data.AuthRepository
import com.example.noteshare.feature.auth.domain.model.LoginRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        if (_uiState.value.isLoading) return
        if (username.length < 3 || username.length > 50) {
            _uiState.update { it.copy(error = "用户名长度必须在 3-50 个字符之间") }
            return
        }
        if (password.length < 6 || password.length > 50) {
            _uiState.update { it.copy(error = "密码长度必须在 6-50 个字符之间") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.login(LoginRequest(username, password))
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is Result.Error -> {
                    val errorMsg = if (result.code == ErrorCode.LOGIN_FAILED) "用户名或密码错误" else result.message
                    _uiState.update { it.copy(isLoading = false, error = errorMsg) }
                }
            }
        }
    }

    fun errorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
