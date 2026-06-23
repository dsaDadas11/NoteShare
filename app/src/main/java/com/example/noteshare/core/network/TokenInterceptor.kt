package com.example.noteshare.core.network

import com.example.noteshare.core.datastore.TokenManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
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

    /** 401 事件回调，由外部注入（如 MainActivity），用于跳转登录页 */
    @Volatile
    var onUnauthorized: (() -> Unit)? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 双重检查锁，避免并发 runBlocking 阻塞 OkHttp 线程池
        val token = cachedToken ?: synchronized(this) {
            cachedToken ?: runBlocking {
                withTimeoutOrNull(3000L) { tokenManager.tokenFlow.firstOrNull() }
                    .also { cachedToken = it }
            }
        }

        val newRequest = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(newRequest)

        // 运行时 401 处理：token 过期或被服务器清除
        if (response.code == 401) {
            cachedToken = null
            onUnauthorized?.invoke()
        }

        return response
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
