package com.example.noteshare.core.common

import com.example.noteshare.core.network.ApiResponse

/**
 * 通用 API 调用包装，统一处理 try/catch 和响应码校验。
 * [T] 为业务数据类型。
 *
 * 用法：
 * ```kotlin
 * suspend fun getProfile(): Result<UserProfile> = safeApiCall {
 *     userApi.getMyProfile()
 * }
 * ```
 */
suspend fun <T> safeApiCall(
    errorMessage: String = "网络请求失败",
    apiCall: suspend () -> ApiResponse<T>
): Result<T> {
    return try {
        val response = apiCall()
        if (response.code == ErrorCode.SUCCESS && response.data != null) {
            Result.Success(response.data)
        } else {
            Result.Error(response.code, response.message ?: "请求失败")
        }
    } catch (e: Exception) {
        Result.Error(ErrorCode.NETWORK_ERROR, "$errorMessage: ${e.message}")
    }
}

/**
 * 适用于响应中 data 可能为 null 的场景（如关注/取消关注）。
 */
suspend fun safeApiCallUnit(
    errorMessage: String = "网络请求失败",
    apiCall: suspend () -> ApiResponse<*>
): Result<Unit> {
    return try {
        val response = apiCall()
        if (response.code == ErrorCode.SUCCESS) {
            Result.Success(Unit)
        } else {
            Result.Error(response.code, response.message ?: "请求失败")
        }
    } catch (e: Exception) {
        Result.Error(ErrorCode.NETWORK_ERROR, "$errorMessage: ${e.message}")
    }
}
