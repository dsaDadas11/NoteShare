package com.example.noteshare.core.network

import com.example.noteshare.core.datastore.TokenManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    @Volatile
    private var cachedToken: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Use cached token if available, otherwise read from DataStore once
        val token = cachedToken ?: runBlocking {
            tokenManager.tokenFlow.firstOrNull().also { cachedToken = it }
        }

        val newRequest = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }

    /**
     * Token 变更时由外部调用清除缓存（登录/登出时）
     */
    fun invalidateCache() {
        cachedToken = null
    }

    /**
     * 外部设置新 token 缓存
     */
    fun updateCachedToken(token: String?) {
        cachedToken = token
    }
}
