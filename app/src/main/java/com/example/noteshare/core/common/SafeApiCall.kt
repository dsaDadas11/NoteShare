package com.example.noteshare.core.common

import com.example.noteshare.core.network.ApiResponse
import kotlinx.serialization.json.Json
import retrofit2.HttpException

private val apiErrorJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/**
 * 解析 HTTP 4xx/5xx 响应体中的业务错误信息。
 * 服务端校验失败等场景会返回 HTTP 400，但 body 仍是 ApiResponse 格式。
 */
internal fun parseHttpException(e: HttpException): Result.Error? {
    val body = e.response()?.errorBody()?.string() ?: return null
    return try {
        val response = apiErrorJson.decodeFromString<ApiResponse<String?>>(body)
        Result.Error(response.code, response.message.ifBlank { "请求失败" })
    } catch (_: Exception) {
        null
    }
}

internal fun mapApiException(e: Exception, errorMessage: String): Result.Error {
    if (e is HttpException) {
        parseHttpException(e)?.let { return it }
    }
    return Result.Error(ErrorCode.NETWORK_ERROR, "$errorMessage: ${e.message}")
}

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
        mapApiException(e, errorMessage)
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
        mapApiException(e, errorMessage)
    }
}
