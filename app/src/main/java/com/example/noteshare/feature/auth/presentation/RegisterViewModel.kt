package com.example.noteshare.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.feature.auth.data.AuthRepository
import com.example.noteshare.feature.auth.domain.model.RegisterRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun register(username: String, password: String, confirmPassword: String) {
        if (_uiState.value.isLoading) return
        if (username.length < 3 || username.length > 50) {
            _uiState.update { it.copy(error = "用户名长度必须在 3-50 个字符之间") }
            return
        }
        if (password.length < 6 || password.length > 50) {
            _uiState.update { it.copy(error = "密码长度必须在 6-50 个字符之间") }
            return
        }
        if (password != confirmPassword) {
            _uiState.update { it.copy(error = "两次输入的密码不一致") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.register(RegisterRequest(username, password))
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is Result.Error -> {
                    val errorMsg = when (result.code) {
                        ErrorCode.USERNAME_EXISTS -> "用户名已存在"
                        ErrorCode.USERNAME_FORMAT_INVALID -> "用户名格式不正确（3-50字符，字母数字下划线）"
                        ErrorCode.PASSWORD_LENGTH_INVALID -> "密码长度需 6-50 字符"
                        else -> result.message
                    }
                    _uiState.update { it.copy(isLoading = false, error = errorMsg) }
                }
            }
        }
    }

    fun errorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
