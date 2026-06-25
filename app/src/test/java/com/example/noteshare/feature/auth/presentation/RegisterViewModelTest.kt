package com.example.noteshare.feature.auth.presentation

import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.feature.auth.data.AuthRepository
import com.example.noteshare.feature.auth.domain.model.RegisterRequest
import com.example.noteshare.feature.auth.domain.model.UserResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: RegisterViewModel

    private val dummyUserResponse = UserResponse(
        id = 1L,
        username = "testuser",
        nickname = "Test",
        createdAt = "2025-01-01T00:00:00Z"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        viewModel = RegisterViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =====================================================================
    // 用户名验证测试
    // =====================================================================

    @Test
    fun register_emptyUsername_showsUsernameError() = runTest {
        viewModel.register("", "password123", "password123")
        val state = viewModel.uiState.value
        assertEquals("用户名长度必须在 3-50 个字符之间", state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun register_oneCharUsername_showsUsernameError() = runTest {
        viewModel.register("a", "password123", "password123")
        assertEquals("用户名长度必须在 3-50 个字符之间", viewModel.uiState.value.error)
    }

    @Test
    fun register_twoCharUsername_showsUsernameError() = runTest {
        viewModel.register("ab", "password123", "password123")
        assertEquals("用户名长度必须在 3-50 个字符之间", viewModel.uiState.value.error)
    }

    @Test
    fun register_threeCharUsername_passesValidation() = runTest {
        coEvery { authRepository.register(any()) } returns Result.Success(dummyUserResponse)
        viewModel.register("abc", "password123", "password123")
        advanceUntilIdle()
        coVerify { authRepository.register(RegisterRequest("abc", "password123")) }
    }

    @Test
    fun register_fiftyCharUsername_passesValidation() = runTest {
        val username = "a".repeat(50)
        coEvery { authRepository.register(any()) } returns Result.Success(dummyUserResponse)
        viewModel.register(username, "password123", "password123")
        advanceUntilIdle()
        coVerify { authRepository.register(RegisterRequest(username, "password123")) }
    }

    @Test
    fun register_fiftyOneCharUsername_showsUsernameError() = runTest {
        val username = "a".repeat(51)
        viewModel.register(username, "password123", "password123")
        assertEquals("用户名长度必须在 3-50 个字符之间", viewModel.uiState.value.error)
        coVerify(exactly = 0) { authRepository.register(any()) }
    }

    // =====================================================================
    // 密码验证测试
    // =====================================================================

    @Test
    fun register_emptyPassword_showsPasswordError() = runTest {
        viewModel.register("testuser", "", "")
        assertEquals("密码长度必须在 6-50 个字符之间", viewModel.uiState.value.error)
    }

    @Test
    fun register_fiveCharPassword_showsPasswordError() = runTest {
        viewModel.register("testuser", "12345", "12345")
        assertEquals("密码长度必须在 6-50 个字符之间", viewModel.uiState.value.error)
    }

    @Test
    fun register_sixCharPassword_passesValidation() = runTest {
        coEvery { authRepository.register(any()) } returns Result.Success(dummyUserResponse)
        viewModel.register("testuser", "123456", "123456")
        advanceUntilIdle()
        coVerify { authRepository.register(RegisterRequest("testuser", "123456")) }
    }

    @Test
    fun register_fiftyCharPassword_passesValidation() = runTest {
        val password = "a".repeat(50)
        coEvery { authRepository.register(any()) } returns Result.Success(dummyUserResponse)
        viewModel.register("testuser", password, password)
        advanceUntilIdle()
        coVerify { authRepository.register(RegisterRequest("testuser", password)) }
    }

    @Test
    fun register_fiftyOneCharPassword_showsPasswordError() = runTest {
        val password = "a".repeat(51)
        viewModel.register("testuser", password, password)
        assertEquals("密码长度必须在 6-50 个字符之间", viewModel.uiState.value.error)
        coVerify(exactly = 0) { authRepository.register(any()) }
    }

    // =====================================================================
    // 确认密码验证测试
    // =====================================================================

    @Test
    fun register_passwordsMatch_passesValidation() = runTest {
        coEvery { authRepository.register(any()) } returns Result.Success(dummyUserResponse)
        viewModel.register("testuser", "password123", "password123")
        advanceUntilIdle()
        coVerify { authRepository.register(any()) }
    }

    @Test
    fun register_passwordsDoNotMatch_showsMismatchError() = runTest {
        viewModel.register("testuser", "password123", "password456")
        assertEquals("两次输入的密码不一致", viewModel.uiState.value.error)
        coVerify(exactly = 0) { authRepository.register(any()) }
    }

    @Test
    fun register_passwordsDoNotMatchEvenWithValidCredentials_doesNotCallApi() = runTest {
        coEvery { authRepository.register(any()) } returns Result.Success(dummyUserResponse)
        viewModel.register("testuser", "abcdef", "ghijkl")
        advanceUntilIdle()
        coVerify(exactly = 0) { authRepository.register(any()) }
    }

    // =====================================================================
    // 注册成功场景
    // =====================================================================

    @Test
    fun register_success_setsIsSuccessTrue() = runTest {
        coEvery { authRepository.register(any()) } returns Result.Success(dummyUserResponse)
        viewModel.register("testuser", "password123", "password123")
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    // =====================================================================
    // 注册失败场景
    // =====================================================================

    @Test
    fun register_usernameExists_showsUsernameExistsError() = runTest {
        coEvery { authRepository.register(any()) } returns Result.Error(
            ErrorCode.USERNAME_EXISTS, "用户名已存在"
        )
        viewModel.register("testuser", "password123", "password123")
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("用户名已存在", state.error)
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
    }

    @Test
    fun register_networkError_showsNetworkErrorMessage() = runTest {
        coEvery { authRepository.register(any()) } returns Result.Error(
            ErrorCode.NETWORK_ERROR, "网络请求失败: timeout"
        )
        viewModel.register("testuser", "password123", "password123")
        advanceUntilIdle()
        assertEquals("网络请求失败: timeout", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun register_invalidUsernameFormat_showsServerValidationMessage() = runTest {
        coEvery { authRepository.register(any()) } returns Result.Error(
            ErrorCode.PARAM_INVALID,
            "username: 用户名只能包含字母、数字、下划线"
        )
        viewModel.register("user@name", "password123", "password123")
        advanceUntilIdle()
        assertEquals("username: 用户名只能包含字母、数字、下划线", viewModel.uiState.value.error)
    }

    @Test
    fun register_serverError_showsServerErrorMessage() = runTest {
        coEvery { authRepository.register(any()) } returns Result.Error(
            ErrorCode.SERVER_ERROR, "服务器内部错误"
        )
        viewModel.register("testuser", "password123", "password123")
        advanceUntilIdle()
        assertEquals("服务器内部错误", viewModel.uiState.value.error)
    }

    // =====================================================================
    // Loading状态测试
    // =====================================================================

    @Test
    fun register_setsLoadingTrueDuringRequest() = runTest {
        coEvery { authRepository.register(any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.Success(dummyUserResponse)
        }
        viewModel.register("testuser", "password123", "password123")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun register_setsLoadingFalseAfterSuccess() = runTest {
        coEvery { authRepository.register(any()) } returns Result.Success(dummyUserResponse)
        viewModel.register("testuser", "password123", "password123")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun register_setsLoadingFalseAfterError() = runTest {
        coEvery { authRepository.register(any()) } returns Result.Error(
            ErrorCode.USERNAME_EXISTS, "用户名已存在"
        )
        viewModel.register("testuser", "password123", "password123")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // =====================================================================
    // 错误消息清除测试
    // =====================================================================

    @Test
    fun errorShown_clearsErrorMessage() = runTest {
        coEvery { authRepository.register(any()) } returns Result.Error(
            ErrorCode.USERNAME_EXISTS, "用户名已存在"
        )
        viewModel.register("testuser", "password123", "password123")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)
        viewModel.errorShown()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun register_clearsPreviousErrorOnNewAttempt() = runTest {
        coEvery { authRepository.register(any()) } returns Result.Error(
            ErrorCode.USERNAME_EXISTS, "用户名已存在"
        )
        viewModel.register("testuser", "password123", "password123")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)
        coEvery { authRepository.register(any()) } returns Result.Success(dummyUserResponse)
        viewModel.register("newuser", "password123", "password123")
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.error)
    }

    // =====================================================================
    // 参数传递正确性测试
    // =====================================================================

    @Test
    fun register_passesCorrectParametersToRepository() = runTest {
        coEvery { authRepository.register(any()) } returns Result.Success(dummyUserResponse)
        viewModel.register("myuser", "mypass123", "mypass123")
        advanceUntilIdle()
        coVerify { authRepository.register(RegisterRequest("myuser", "mypass123")) }
    }

    // =====================================================================
    // 验证优先级测试：用户名无效时不检查密码
    // =====================================================================

    @Test
    fun register_invalidUsername_doesNotCheckPassword() = runTest {
        viewModel.register("ab", "short", "different")
        assertEquals("用户名长度必须在 3-50 个字符之间", viewModel.uiState.value.error)
    }
}
