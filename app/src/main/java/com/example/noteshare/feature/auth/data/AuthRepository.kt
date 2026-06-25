package com.example.noteshare.feature.auth.data

import com.example.noteshare.core.common.Result
import com.example.noteshare.core.common.safeApiCall
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
    suspend fun register(request: RegisterRequest): Result<UserResponse> =
        safeApiCall("网络请求失败") { authApi.register(request) }

    suspend fun login(request: LoginRequest): Result<Unit> {
        return when (val result = safeApiCall("登录请求失败") { authApi.login(request) }) {
            is Result.Success -> {
                tokenManager.saveToken(result.data.token)
                tokenInterceptor.updateCachedToken(result.data.token)
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }
}
