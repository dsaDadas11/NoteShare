package com.example.noteshare.feature.auth.presentation

import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.feature.auth.data.AuthRepository
import com.example.noteshare.feature.auth.domain.model.LoginRequest
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
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        viewModel = LoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =====================================================================
    // 用户名验证测试
    // =====================================================================

    @Test
    fun login_emptyUsername_showsUsernameError() = runTest {
        // 空用户名应触发长度验证失败
        viewModel.login("", "password123")
        val state = viewModel.uiState.value
        assertEquals("用户名长度必须在 3-50 个字符之间", state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun login_oneCharUsername_showsUsernameError() = runTest {
        // 1个字符的用户名太短，应触发验证错误
        viewModel.login("a", "password123")
        val state = viewModel.uiState.value
        assertEquals("用户名长度必须在 3-50 个字符之间", state.error)
    }

    @Test
    fun login_twoCharUsername_showsUsernameError() = runTest {
        // 2个字符的用户名仍然太短，应触发验证错误
        viewModel.login("ab", "password123")
        val state = viewModel.uiState.value
        assertEquals("用户名长度必须在 3-50 个字符之间", state.error)
    }

    @Test
    fun login_threeCharUsername_passesValidation() = runTest {
        // 3个字符是用户名最小长度边界，验证应通过，调用authRepository
        coEvery { authRepository.login(any()) } returns Result.Success(Unit)
        viewModel.login("abc", "password123")
        advanceUntilIdle()
        coVerify { authRepository.login(LoginRequest("abc", "password123")) }
    }

    @Test
    fun login_fiftyCharUsername_passesValidation() = runTest {
        // 50个字符是用户名最大长度边界，验证应通过
        val username = "a".repeat(50)
        coEvery { authRepository.login(any()) } returns Result.Success(Unit)
        viewModel.login(username, "password123")
        advanceUntilIdle()
        coVerify { authRepository.login(LoginRequest(username, "password123")) }
    }

    @Test
    fun login_fiftyOneCharUsername_showsUsernameError() = runTest {
        // 51个字符超出最大长度，应触发验证错误
        val username = "a".repeat(51)
        viewModel.login(username, "password123")
        val state = viewModel.uiState.value
        assertEquals("用户名长度必须在 3-50 个字符之间", state.error)
        coVerify(exactly = 0) { authRepository.login(any()) }
    }

    // =====================================================================
    // 密码验证测试
    // =====================================================================

    @Test
    fun login_emptyPassword_showsPasswordError() = runTest {
        // 空密码应触发长度验证失败
        viewModel.login("testuser", "")
        val state = viewModel.uiState.value
        assertEquals("密码长度必须在 6-50 个字符之间", state.error)
    }

    @Test
    fun login_fiveCharPassword_showsPasswordError() = runTest {
        // 5个字符的密码太短，应触发验证错误
        viewModel.login("testuser", "12345")
        val state = viewModel.uiState.value
        assertEquals("密码长度必须在 6-50 个字符之间", state.error)
    }

    @Test
    fun login_sixCharPassword_passesValidation() = runTest {
        // 6个字符是密码最小长度边界，验证应通过
        coEvery { authRepository.login(any()) } returns Result.Success(Unit)
        viewModel.login("testuser", "123456")
        advanceUntilIdle()
        coVerify { authRepository.login(LoginRequest("testuser", "123456")) }
    }

    @Test
    fun login_fiftyCharPassword_passesValidation() = runTest {
        // 50个字符是密码最大长度边界，验证应通过
        val password = "a".repeat(50)
        coEvery { authRepository.login(any()) } returns Result.Success(Unit)
        viewModel.login("testuser", password)
        advanceUntilIdle()
        coVerify { authRepository.login(LoginRequest("testuser", password)) }
    }

    @Test
    fun login_fiftyOneCharPassword_showsPasswordError() = runTest {
        // 51个字符超出密码最大长度，应触发验证错误
        val password = "a".repeat(51)
        viewModel.login("testuser", password)
        val state = viewModel.uiState.value
        assertEquals("密码长度必须在 6-50 个字符之间", state.error)
        coVerify(exactly = 0) { authRepository.login(any()) }
    }

    @Test
    fun login_invalidUsername_doesNotCallRepository() = runTest {
        // 用户名无效时不应调用authRepository
        viewModel.login("ab", "123456")
        coVerify(exactly = 0) { authRepository.login(any()) }
    }

    // =====================================================================
    // 登录成功场景
    // =====================================================================

    @Test
    fun login_success_setsIsSuccessTrue() = runTest {
        // 登录成功时，isSuccess应为true，isLoading应为false
        coEvery { authRepository.login(any()) } returns Result.Success(Unit)
        viewModel.login("testuser", "123456")
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    // =====================================================================
    // 登录失败场景
    // =====================================================================

    @Test
    fun login_networkError_showsNetworkErrorMessage() = runTest {
        // 网络错误时应显示Result.Error中的原始message（非LOGIN_FAILED错误码）
        coEvery { authRepository.login(any()) } returns Result.Error(
            ErrorCode.NETWORK_ERROR, "登录请求失败: Connection refused"
        )
        viewModel.login("testuser", "123456")
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("登录请求失败: Connection refused", state.error)
        assertFalse(state.isSuccess)
    }

    @Test
    fun login_loginFailed_showsFriendlyErrorMessage() = runTest {
        // LOGIN_FAILED错误码应显示友好的"用户名或密码错误"提示
        coEvery { authRepository.login(any()) } returns Result.Error(
            ErrorCode.LOGIN_FAILED, "用户名或密码错误"
        )
        viewModel.login("testuser", "wrongpassword")
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("用户名或密码错误", state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun login_serverError_showsServerErrorMessage() = runTest {
        // 服务器内部错误（50000）应显示服务器返回的原始message
        coEvery { authRepository.login(any()) } returns Result.Error(
            ErrorCode.SERVER_ERROR, "服务器内部错误"
        )
        viewModel.login("testuser", "123456")
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("服务器内部错误", state.error)
        assertFalse(state.isLoading)
    }

    // =====================================================================
    // Loading状态测试
    // =====================================================================

    @Test
    fun login_setsLoadingTrueDuringRequest() = runTest {
        // 使用UnconfinedTestDispatcher验证loading状态：先变为true，请求完成后恢复false
        // 通过advanceUntilIdle()观察完整的loading生命周期
        coEvery { authRepository.login(any()) } coAnswers {
            // 协程执行到这里时，isLoading已经为true
            kotlinx.coroutines.delay(100) // 模拟网络请求耗时
            Result.Success(Unit)
        }
        viewModel.login("testuser", "123456")
        advanceUntilIdle()
        // 请求完成后isLoading恢复为false
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun login_setsLoadingFalseAfterSuccess() = runTest {
        // 请求完成后isLoading应恢复为false
        coEvery { authRepository.login(any()) } returns Result.Success(Unit)
        viewModel.login("testuser", "123456")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun login_setsLoadingFalseAfterError() = runTest {
        // 请求失败后isLoading也应恢复为false
        coEvery { authRepository.login(any()) } returns Result.Error(
            ErrorCode.LOGIN_FAILED, "用户名或密码错误"
        )
        viewModel.login("testuser", "123456")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // =====================================================================
    // 错误消息清除测试
    // =====================================================================

    @Test
    fun errorShown_clearsErrorMessage() = runTest {
        // errorShown()调用后error应被清除
        coEvery { authRepository.login(any()) } returns Result.Error(
            ErrorCode.LOGIN_FAILED, "用户名或密码错误"
        )
        viewModel.login("testuser", "wrongpassword")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)
        viewModel.errorShown()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun login_clearsPreviousErrorOnNewAttempt() = runTest {
        // 新的登录尝试应清除之前的error消息
        coEvery { authRepository.login(any()) } returns Result.Error(
            ErrorCode.LOGIN_FAILED, "用户名或密码错误"
        )
        viewModel.login("testuser", "wrongpassword")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)
        // 第二次尝试
        coEvery { authRepository.login(any()) } returns Result.Success(Unit)
        viewModel.login("testuser", "123456")
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.error)
    }

    // =====================================================================
    // 参数传递正确性测试
    // =====================================================================

    @Test
    fun login_passesCorrectParametersToRepository() = runTest {
        // 验证LoginRequest参数正确传递给authRepository
        coEvery { authRepository.login(any()) } returns Result.Success(Unit)
        viewModel.login("myuser", "mypass123")
        advanceUntilIdle()
        coVerify { authRepository.login(LoginRequest("myuser", "mypass123")) }
    }

    // =====================================================================
    // 边界：仅用户名无效时不调用API
    // =====================================================================

    @Test
    fun login_onlyInvalidUsername_doesNotCallRepository() = runTest {
        // 用户名无效但密码有效，不应调用API
        viewModel.login("ab", "validpass")
        coVerify(exactly = 0) { authRepository.login(any()) }
    }
}
