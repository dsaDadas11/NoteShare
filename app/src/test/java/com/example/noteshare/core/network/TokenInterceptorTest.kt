package com.example.noteshare.core.network

import com.example.noteshare.core.datastore.TokenManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * TokenInterceptor 单元测试
 * 覆盖：添加Authorization header、Token为空时不添加、401触发回调、
 * 缓存更新、并发Token一致性、invalidateCache、updateCachedToken
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TokenInterceptorTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var tokenManager: TokenManager
    private lateinit var interceptor: TokenInterceptor

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        tokenManager = mockk()
        interceptor = TokenInterceptor(tokenManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 辅助方法：构造一个 mock Interceptor.Chain
     */
    private fun buildChain(
        url: String = "https://example.com/api/test",
        tokenFlow: kotlinx.coroutines.flow.Flow<String?> = flowOf("test-token-123"),
        responseCode: Int = 200
    ): Interceptor.Chain {
        coEvery { tokenManager.tokenFlow } returns tokenFlow

        val request = Request.Builder()
            .url(url)
            .build()

        val responseBuilder = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(responseCode)
            .message("OK")
            .body(okhttp3.ResponseBody.create(null, ByteArray(0)))

        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } returns responseBuilder.build()

        return chain
    }

    /**
     * Token存在时，应在请求中添加Authorization header
     */
    @Test
    fun intercept_withToken_shouldAddAuthorizationHeader() = runTest {
        val chain = buildChain(tokenFlow = flowOf("my-secret-token"))
        interceptor.intercept(chain)

        val capturedRequest = io.mockk.slot<Request>()
        verify { chain.proceed(capture(capturedRequest)) }

        val authHeader = capturedRequest.captured.header("Authorization")
        assertEquals("Bearer my-secret-token", authHeader)
    }

    /**
     * Token为空字符串时，不应添加Authorization header
     */
    @Test
    fun intercept_emptyToken_shouldNotAddAuthorizationHeader() = runTest {
        val chain = buildChain(tokenFlow = flowOf(""))
        interceptor.intercept(chain)

        val capturedRequest = io.mockk.slot<Request>()
        verify { chain.proceed(capture(capturedRequest)) }

        val authHeader = capturedRequest.captured.header("Authorization")
        assertNull(authHeader)
    }

    /**
     * Token为null时，不应添加Authorization header
     */
    @Test
    fun intercept_nullToken_shouldNotAddAuthorizationHeader() = runTest {
        val chain = buildChain(tokenFlow = flowOf(null))
        interceptor.intercept(chain)

        val capturedRequest = io.mockk.slot<Request>()
        verify { chain.proceed(capture(capturedRequest)) }

        val authHeader = capturedRequest.captured.header("Authorization")
        assertNull(authHeader)
    }

    /**
     * 收到401响应时，应清空cachedToken并触发onUnauthorized回调
     */
    @Test
    fun intercept_401Response_shouldClearCacheAndTriggerCallback() = runTest {
        var callbackInvoked = false
        interceptor.onUnauthorized = { callbackInvoked = true }

        val chain = buildChain(tokenFlow = flowOf("expired-token"), responseCode = 401)
        interceptor.intercept(chain)

        assertTrue(callbackInvoked)
        // 缓存已被清空，下一次请求应重新从 tokenManager 读取
    }

    /**
     * 收到401响应时，cachedToken应被设为null
     */
    @Test
    fun intercept_401Response_cachedTokenShouldBeNull() = runTest {
        // 预热缓存
        val chain1 = buildChain(tokenFlow = flowOf("token-abc"))
        interceptor.intercept(chain1)

        // 第二次请求触发401
        coEvery { tokenManager.tokenFlow } returns flowOf("token-abc") // 还能读到token
        val request2 = Request.Builder().url("https://example.com/api/test2").build()
        val response401 = Response.Builder()
            .request(request2)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body(okhttp3.ResponseBody.create(null, ByteArray(0)))
        val chain2 = mockk<Interceptor.Chain>()
        every { chain2.request() } returns request2
        every { chain2.proceed(any()) } returns response401.build()

        interceptor.intercept(chain2)

        // 缓存已被清空，下一个请求应重新调用 tokenManager.tokenFlow
        // 再次发请求，token应该从 flow 重新读取
        val chain3 = buildChain(tokenFlow = flowOf("new-token"))
        interceptor.intercept(chain3)

        // chain3.proceed 应被调用一次，且 header 中携带了从 flow 重新读取的新 token
        val capturedRequest = io.mockk.slot<Request>()
        verify(exactly = 1) { chain3.proceed(capture(capturedRequest)) }
        assertEquals("Bearer new-token", capturedRequest.captured.header("Authorization"))
    }

    /**
     * 401未注册onUnauthorized时不应崩溃
     */
    @Test
    fun intercept_401Response_noCallbackInstalled_shouldNotCrash() = runTest {
        interceptor.onUnauthorized = null

        val chain = buildChain(tokenFlow = flowOf("some-token"), responseCode = 401)
        // 不应抛出异常
        interceptor.intercept(chain)
    }

    /**
     * 非401响应不应触发onUnauthorized回调
     */
    @Test
    fun intercept_200Response_shouldNotTriggerCallback() = runTest {
        var callbackInvoked = false
        interceptor.onUnauthorized = { callbackInvoked = true }

        val chain = buildChain(tokenFlow = flowOf("valid-token"), responseCode = 200)
        interceptor.intercept(chain)

        assertFalse(callbackInvoked)
    }

    /**
     * invalidateCache后，下次请求应重新从 tokenManager 读取token
     */
    @Test
    fun invalidateCache_shouldForceReReadFromTokenManager() = runTest {
        // 先预热缓存
        val chain1 = buildChain(tokenFlow = flowOf("old-token"))
        interceptor.intercept(chain1)

        // 清除缓存
        interceptor.invalidateCache()

        // 下一次请求应重新从 tokenManager 读取新token
        val chain2 = buildChain(tokenFlow = flowOf("new-token"))
        interceptor.intercept(chain2)

        val capturedRequest = io.mockk.slot<Request>()
        verify { chain2.proceed(capture(capturedRequest)) }

        val authHeader = capturedRequest.captured.header("Authorization")
        assertEquals("Bearer new-token", authHeader)
    }

    /**
     * updateCachedToken后，下次请求应使用新token
     */
    @Test
    fun updateCachedToken_shouldUseNewTokenImmediately() = runTest {
        // 先用默认token预热
        val chain1 = buildChain(tokenFlow = flowOf("initial-token"))
        interceptor.intercept(chain1)

        // 直接更新缓存
        interceptor.updateCachedToken("manually-set-token")

        // 下一次请求应使用手动设置的token
        val request = Request.Builder().url("https://example.com/api/test2").build()
        val chain2 = mockk<Interceptor.Chain>()
        every { chain2.request() } returns request
        every { chain2.proceed(any()) } returns Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(okhttp3.ResponseBody.create(null, ByteArray(0)))
            .build()
        interceptor.intercept(chain2)

        val capturedRequest = io.mockk.slot<Request>()
        verify { chain2.proceed(capture(capturedRequest)) }

        val authHeader = capturedRequest.captured.header("Authorization")
        assertEquals("Bearer manually-set-token", authHeader)
    }

    /**
     * 多次请求连续调用，应复用缓存的token（不需要每次都从 tokenManager 读取）
     */
    @Test
    fun intercept_multipleRequests_shouldReuseCachedToken() = runTest {
        coEvery { tokenManager.tokenFlow } returns flowOf("cached-token")

        val chain1 = buildChain(tokenFlow = flowOf("cached-token"))
        val chain2 = buildChain(tokenFlow = flowOf("cached-token"))
        val chain3 = buildChain(tokenFlow = flowOf("cached-token"))

        interceptor.intercept(chain1)
        interceptor.intercept(chain2)
        interceptor.intercept(chain3)

        // 三次请求都应正确添加 Authorization header
        val slot1 = io.mockk.slot<Request>()
        val slot2 = io.mockk.slot<Request>()
        val slot3 = io.mockk.slot<Request>()
        verify(exactly = 1) { chain1.proceed(capture(slot1)) }
        verify(exactly = 1) { chain2.proceed(capture(slot2)) }
        verify(exactly = 1) { chain3.proceed(capture(slot3)) }
        assertEquals("Bearer cached-token", slot1.captured.header("Authorization"))
        assertEquals("Bearer cached-token", slot2.captured.header("Authorization"))
        assertEquals("Bearer cached-token", slot3.captured.header("Authorization"))
    }
}
