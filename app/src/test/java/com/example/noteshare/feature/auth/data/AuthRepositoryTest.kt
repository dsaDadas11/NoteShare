package com.example.noteshare.feature.auth.data

import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.core.datastore.TokenManager
import com.example.noteshare.core.network.ApiResponse
import com.example.noteshare.core.network.TokenInterceptor
import com.example.noteshare.feature.auth.domain.model.LoginRequest
import com.example.noteshare.feature.auth.domain.model.LoginResponse
import com.example.noteshare.feature.auth.domain.model.RegisterRequest
import com.example.noteshare.feature.auth.domain.model.UserResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private lateinit var authApi: AuthApi
    private lateinit var tokenManager: TokenManager
    private lateinit var tokenInterceptor: TokenInterceptor
    private lateinit var repository: AuthRepository

    private val dummyUser = UserResponse(
        id = 1L,
        username = "testuser",
        nickname = "Test",
        createdAt = "2025-01-01T00:00:00Z"
    )

    private val dummyLoginResponse = LoginResponse(
        token = "jwt-token-abc123",
        user = dummyUser
    )

    @Before
    fun setUp() {
        authApi = mockk(relaxed = true)
        tokenManager = mockk(relaxed = true)
        tokenInterceptor = mockk(relaxed = true)
        repository = AuthRepository(authApi, tokenManager, tokenInterceptor)
    }

    // =====================================================================
    // 登录成功测试
    // =====================================================================

    @Test
    fun login_success_returnsSuccessResult() = runTest {
        // 登录成功时应返回Result.Success
        coEvery { authApi.login(any()) } returns ApiResponse(
            code = ErrorCode.SUCCESS,
            message = "ok",
            data = dummyLoginResponse
        )
        val result = repository.login(LoginRequest("testuser", "password123"))
        assertTrue(result is Result.Success)
    }

    @Test
    fun login_success_savesTokenToTokenManager() = runTest {
        // 登录成功时应将token保存到TokenManager
        coEvery { authApi.login(any()) } returns ApiResponse(
            code = ErrorCode.SUCCESS,
            message = "ok",
            data = dummyLoginResponse
        )
        repository.login(LoginRequest("testuser", "password123"))
        coVerify { tokenManager.saveToken("jwt-token-abc123") }
    }

    @Test
    fun login_success_updatesTokenInterceptorCache() = runTest {
        // 登录成功时应更新TokenInterceptor的缓存
        coEvery { authApi.login(any()) } returns ApiResponse(
            code = ErrorCode.SUCCESS,
            message = "ok",
            data = dummyLoginResponse
        )
        repository.login(LoginRequest("testuser", "password123"))
        verify { tokenInterceptor.updateCachedToken("jwt-token-abc123") }
    }

    @Test
    fun login_success_tokenSavedBeforeInterceptorUpdated() = runTest {
        // 验证token保存顺序：先保存到TokenManager，再更新Interceptor缓存
        coEvery { authApi.login(any()) } returns ApiResponse(
            code = ErrorCode.SUCCESS,
            message = "ok",
            data = dummyLoginResponse
        )
        repository.login(LoginRequest("testuser", "password123"))
        coVerifyOrder {
            tokenManager.saveToken("jwt-token-abc123")
            tokenInterceptor.updateCachedToken("jwt-token-abc123")
        }
    }

    @Test
    fun login_success_passesCorrectRequestToApi() = runTest {
        // 验证传递给API的LoginRequest参数正确
        coEvery { authApi.login(any()) } returns ApiResponse(
            code = ErrorCode.SUCCESS,
            message = "ok",
            data = dummyLoginResponse
        )
        repository.login(LoginRequest("myuser", "mypass"))
        coVerify { authApi.login(LoginRequest("myuser", "mypass")) }
    }

    // =====================================================================
    // 登录失败测试
    // =====================================================================

    @Test
    fun login_wrongPassword_returnsLoginFailedError() = runTest {
        // 错误密码时应返回LOGIN_FAILED错误码
        coEvery { authApi.login(any()) } returns ApiResponse(
            code = ErrorCode.LOGIN_FAILED,
            message = "用户名或密码错误"
        )
        val result = repository.login(LoginRequest("testuser", "wrongpassword"))
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.LOGIN_FAILED, (result as Result.Error).code)
        assertEquals("用户名或密码错误", result.message)
    }

    @Test
    fun login_wrongPassword_doesNotSaveToken() = runTest {
        // 登录失败时不应保存token
        coEvery { authApi.login(any()) } returns ApiResponse(
            code = ErrorCode.LOGIN_FAILED,
            message = "用户名或密码错误"
        )
        repository.login(LoginRequest("testuser", "wrongpassword"))
        coVerify(exactly = 0) { tokenManager.saveToken(any()) }
    }

    @Test
    fun login_wrongPassword_doesNotUpdateInterceptor() = runTest {
        // 登录失败时不应更新Interceptor缓存
        coEvery { authApi.login(any()) } returns ApiResponse(
            code = ErrorCode.LOGIN_FAILED,
            message = "用户名或密码错误"
        )
        repository.login(LoginRequest("testuser", "wrongpassword"))
        verify(exactly = 0) { tokenInterceptor.updateCachedToken(any()) }
    }

    @Test
    fun login_serverError_returnsServerError() = runTest {
        // 服务器内部错误
        coEvery { authApi.login(any()) } returns ApiResponse(
            code = ErrorCode.SERVER_ERROR,
            message = "服务器内部错误"
        )
        val result = repository.login(LoginRequest("testuser", "password123"))
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.SERVER_ERROR, (result as Result.Error).code)
    }

    @Test
    fun login_dataNull_returnsError() = runTest {
        // 服务器返回SUCCESS但data为null
        coEvery { authApi.login(any()) } returns ApiResponse(
            code = ErrorCode.SUCCESS,
            message = "ok",
            data = null
        )
        val result = repository.login(LoginRequest("testuser", "password123"))
        assertTrue(result is Result.Error)
    }

    @Test
    fun login_networkException_returnsNetworkError() = runTest {
        // 网络异常（抛出异常）时应返回NETWORK_ERROR
        coEvery { authApi.login(any()) } throws java.io.IOException("Connection refused")
        val result = repository.login(LoginRequest("testuser", "password123"))
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertEquals(ErrorCode.NETWORK_ERROR, error.code)
        assertTrue(error.message.contains("Connection refused"))
    }

    @Test
    fun login_timeoutException_returnsNetworkError() = runTest {
        // 超时异常也应被捕获并返回NETWORK_ERROR
        coEvery { authApi.login(any()) } throws java.net.SocketTimeoutException("Read timed out")
        val result = repository.login(LoginRequest("testuser", "password123"))
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
    }

    // =====================================================================
    // 注册成功测试
    // =====================================================================

    @Test
    fun register_success_returnsSuccessWithUserResponse() = runTest {
        // 注册成功时应返回包含UserResponse的Result.Success
        coEvery { authApi.register(any()) } returns ApiResponse(
            code = ErrorCode.SUCCESS,
            message = "ok",
            data = dummyUser
        )
        val result = repository.register(RegisterRequest("testuser", "password123"))
        assertTrue(result is Result.Success)
        assertEquals(dummyUser, (result as Result.Success).data)
    }

    @Test
    fun register_success_passesCorrectRequestToApi() = runTest {
        // 验证传递给API的RegisterRequest参数正确
        coEvery { authApi.register(any()) } returns ApiResponse(
            code = ErrorCode.SUCCESS,
            message = "ok",
            data = dummyUser
        )
        repository.register(RegisterRequest("newuser", "newpassword"))
        coVerify { authApi.register(RegisterRequest("newuser", "newpassword")) }
    }

    // =====================================================================
    // 注册失败测试
    // =====================================================================

    @Test
    fun register_usernameExists_returnsUsernameExistsError() = runTest {
        // 用户名已存在时应返回USERNAME_EXISTS错误码
        coEvery { authApi.register(any()) } returns ApiResponse(
            code = ErrorCode.USERNAME_EXISTS,
            message = "用户名已存在"
        )
        val result = repository.register(RegisterRequest("testuser", "password123"))
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.USERNAME_EXISTS, (result as Result.Error).code)
        assertEquals("用户名已存在", result.message)
    }

    @Test
    fun register_invalidUsernameFormat_returnsFormatInvalidError() = runTest {
        // 用户名格式不合法
        coEvery { authApi.register(any()) } returns ApiResponse(
            code = ErrorCode.USERNAME_FORMAT_INVALID,
            message = "用户名格式不合法"
        )
        val result = repository.register(RegisterRequest("testuser", "password123"))
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.USERNAME_FORMAT_INVALID, (result as Result.Error).code)
    }

    @Test
    fun register_invalidPasswordLength_returnsPasswordLengthError() = runTest {
        // 密码长度不符合要求
        coEvery { authApi.register(any()) } returns ApiResponse(
            code = ErrorCode.PASSWORD_LENGTH_INVALID,
            message = "密码长度不符合要求"
        )
        val result = repository.register(RegisterRequest("testuser", "password123"))
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.PASSWORD_LENGTH_INVALID, (result as Result.Error).code)
    }

    @Test
    fun register_dataNull_returnsError() = runTest {
        // 服务器返回SUCCESS但data为null
        coEvery { authApi.register(any()) } returns ApiResponse(
            code = ErrorCode.SUCCESS,
            message = "ok",
            data = null
        )
        val result = repository.register(RegisterRequest("testuser", "password123"))
        assertTrue(result is Result.Error)
    }

    @Test
    fun register_networkException_returnsNetworkError() = runTest {
        // 网络异常时应返回NETWORK_ERROR
        coEvery { authApi.register(any()) } throws java.io.IOException("Connection refused")
        val result = repository.register(RegisterRequest("testuser", "password123"))
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertEquals(ErrorCode.NETWORK_ERROR, error.code)
        assertTrue(error.message.contains("Connection refused"))
    }

    @Test
    fun register_serverError_returnsServerError() = runTest {
        // 服务器内部错误
        coEvery { authApi.register(any()) } returns ApiResponse(
            code = ErrorCode.SERVER_ERROR,
            message = "服务器内部错误"
        )
        val result = repository.register(RegisterRequest("testuser", "password123"))
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.SERVER_ERROR, (result as Result.Error).code)
    }

    // =====================================================================
    // 网络异常处理补充测试
    // =====================================================================

    @Test
    fun login_unknownException_returnsNetworkError() = runTest {
        // 未知异常也应被正确捕获
        coEvery { authApi.login(any()) } throws RuntimeException("Unknown error")
        val result = repository.login(LoginRequest("testuser", "password123"))
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
    }

    @Test
    fun register_unknownException_returnsNetworkError() = runTest {
        // 注册时未知异常也应被正确捕获
        coEvery { authApi.register(any()) } throws IllegalStateException("Illegal state")
        val result = repository.register(RegisterRequest("testuser", "password123"))
        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
    }

    @Test
    fun login_networkException_doesNotSaveToken() = runTest {
        // 网络异常时不应保存token
        coEvery { authApi.login(any()) } throws java.io.IOException("Connection refused")
        repository.login(LoginRequest("testuser", "password123"))
        coVerify(exactly = 0) { tokenManager.saveToken(any()) }
    }

    @Test
    fun login_networkException_doesNotUpdateInterceptor() = runTest {
        // 网络异常时不应更新Interceptor缓存
        coEvery { authApi.login(any()) } throws java.io.IOException("Connection refused")
        repository.login(LoginRequest("testuser", "password123"))
        verify(exactly = 0) { tokenInterceptor.updateCachedToken(any()) }
    }
}
