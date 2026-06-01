package com.example.noteshare.feature.auth.data

import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.core.datastore.TokenManager
import com.example.noteshare.core.network.TokenInterceptor
import com.example.noteshare.feature.auth.domain.model.LoginRequest
import com.example.noteshare.feature.auth.domain.model.RegisterRequest
import com.example.noteshare.feature.auth.domain.model.UserResponse
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val tokenInterceptor: TokenInterceptor
) {
    suspend fun register(request: RegisterRequest): Result<UserResponse> {
        return try {
            val response = authApi.register(request)
            if (response.code == ErrorCode.SUCCESS && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }

    suspend fun login(request: LoginRequest): Result<Unit> {
        return try {
            val response = authApi.login(request)
            if (response.code == ErrorCode.SUCCESS && response.data != null) {
                // Save token and update interceptor cache
                tokenManager.saveToken(response.data.token)
                tokenInterceptor.updateCachedToken(response.data.token)
                Result.Success(Unit)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "登录请求失败: ${e.message}")
        }
    }
}
