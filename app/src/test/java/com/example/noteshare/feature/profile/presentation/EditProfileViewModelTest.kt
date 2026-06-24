package com.example.noteshare.feature.profile.presentation

import android.net.Uri
import com.example.noteshare.core.common.Result
import com.example.noteshare.feature.profile.data.ProfileRepository
import com.example.noteshare.feature.profile.domain.model.UpdateProfileRequest
import com.example.noteshare.feature.profile.domain.model.UserProfileResponse
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EditProfileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: ProfileRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    private fun fakeProfile(
        username: String = "testuser",
        nickname: String? = "测试昵称",
        bio: String? = "个人简介",
        avatarUrl: String? = "https://example.com/avatar.jpg"
    ) = UserProfileResponse(
        id = 1L,
        username = username,
        nickname = nickname,
        avatarUrl = avatarUrl,
        bio = bio,
        followerCount = 0,
        followingCount = 0,
        noteCount = 0
    )

    // ========================================================================
    // 加载当前资料
    // ========================================================================

    /** 场景：init 时成功加载当前用户资料，表单字段被填充 */
    @Test
    fun init_loadProfile_success() = runTest {
        val profile = fakeProfile()
        coEvery { repository.getMyProfile() } returns Result.Success(profile)

        val vm = EditProfileViewModel(repository)

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("testuser", state.username)
        assertEquals("测试昵称", state.nickname)
        assertEquals("个人简介", state.bio)
        assertEquals("https://example.com/avatar.jpg", state.avatarUrl)
        assertNull(state.error)
    }

    /** 场景：加载资料时 nickname 为 null，应显示空字符串 */
    @Test
    fun init_loadProfile_nullNickname_showsEmpty() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile(nickname = null))

        val vm = EditProfileViewModel(repository)

        assertEquals("", vm.uiState.value.nickname)
    }

    /** 场景：加载资料时 bio 为 null，应显示空字符串 */
    @Test
    fun init_loadProfile_nullBio_showsEmpty() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile(bio = null))

        val vm = EditProfileViewModel(repository)

        assertEquals("", vm.uiState.value.bio)
    }

    /** 场景：加载失败 - error 被设置 */
    @Test
    fun init_loadProfile_failure_setsError() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Error(50000, "服务器错误")

        val vm = EditProfileViewModel(repository)

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("服务器错误"))
    }

    // ========================================================================
    // 保存昵称
    // ========================================================================

    /** 场景：updateNickname 正常更新 state */
    @Test
    fun updateNickname_normalValue_updatesState() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile())

        val vm = EditProfileViewModel(repository)
        vm.updateNickname("新昵称")

        assertEquals("新昵称", vm.uiState.value.nickname)
    }

    /** 场景：updateNickname 刚好 50 字 - 应允许 */
    @Test
    fun updateNickname_exactly50Chars_allowed() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile())

        val vm = EditProfileViewModel(repository)
        val name50 = "A".repeat(50)
        vm.updateNickname(name50)

        assertEquals(name50, vm.uiState.value.nickname)
    }

    /** 场景：updateNickname 超过 50 字 - 不更新 */
    @Test
    fun updateNickname_over50Chars_notUpdated() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile())

        val vm = EditProfileViewModel(repository)
        val original = vm.uiState.value.nickname
        val name51 = "A".repeat(51)
        vm.updateNickname(name51)

        assertEquals(original, vm.uiState.value.nickname)
    }

    /** 场景：updateNickname 空字符串 - 应允许（清除昵称） */
    @Test
    fun updateNickname_emptyString_allowed() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile())

        val vm = EditProfileViewModel(repository)
        vm.updateNickname("")

        assertEquals("", vm.uiState.value.nickname)
    }

    // ========================================================================
    // 保存简介
    // ========================================================================

    /** 场景：updateBio 正常更新 state */
    @Test
    fun updateBio_normalValue_updatesState() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile())

        val vm = EditProfileViewModel(repository)
        vm.updateBio("新的个人简介")

        assertEquals("新的个人简介", vm.uiState.value.bio)
    }

    /** 场景：updateBio 刚好 500 字 - 应允许 */
    @Test
    fun updateBio_exactly500Chars_allowed() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile())

        val vm = EditProfileViewModel(repository)
        val bio500 = "B".repeat(500)
        vm.updateBio(bio500)

        assertEquals(bio500, vm.uiState.value.bio)
    }

    /** 场景：updateBio 超过 500 字 - 不更新 */
    @Test
    fun updateBio_over500Chars_notUpdated() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile())

        val vm = EditProfileViewModel(repository)
        val original = vm.uiState.value.bio
        val bio501 = "B".repeat(501)
        vm.updateBio(bio501)

        assertEquals(original, vm.uiState.value.bio)
    }

    // ========================================================================
    // 头像更新
    // ========================================================================

    /** 场景：updateAvatar 设置 newAvatarUri */
    @Test
    fun updateAvatar_setsNewAvatarUri() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile())

        val uri = mockk<Uri>()
        val vm = EditProfileViewModel(repository)
        vm.updateAvatar(uri)

        assertEquals(uri, vm.uiState.value.newAvatarUri)
    }

    // ========================================================================
    // 保存 profile - 成功
    // ========================================================================

    /** 场景：保存 profile 无头像更新 - 成功 */
    @Test
    fun saveProfile_noAvatar_success() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile())
        coEvery { repository.updateProfile(any()) } returns Result.Success(fakeProfile())

        val vm = EditProfileViewModel(repository)
        vm.updateNickname("新昵称")
        vm.saveProfile()

        val state = vm.uiState.value
        assertTrue(state.isSuccess)
        assertFalse(state.isSaving)
        assertNull(state.error)
    }

    /** 场景：保存 profile 无头像 - 验证 UpdateProfileRequest 参数正确 */
    @Test
    fun saveProfile_noAvatar_correctRequest() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile())
        val requestSlot = slot<UpdateProfileRequest>()
        coEvery { repository.updateProfile(capture(requestSlot)) } returns Result.Success(fakeProfile())

        val vm = EditProfileViewModel(repository)
        vm.updateNickname("新昵称")
        vm.updateBio("新简介")
        vm.saveProfile()

        assertEquals("新昵称", requestSlot.captured.nickname)
        assertEquals("新简介", requestSlot.captured.bio)
    }

    /** 场景：保存 profile 带新头像 - 头像上传成功后更新资料 */
    @Test
    fun saveProfile_withNewAvatar_success() = runTest {
        val newAvatarUrl = "https://example.com/new-avatar.jpg"
        val uri = mockk<Uri>()
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile())
        coEvery { repository.uploadAvatar(uri) } returns Result.Success(newAvatarUrl)
        coEvery { repository.updateProfile(any()) } returns Result.Success(fakeProfile())

        val vm = EditProfileViewModel(repository)
        vm.updateAvatar(uri)
        vm.saveProfile()

        val state = vm.uiState.value
        assertTrue(state.isSuccess)
        assertFalse(state.isSaving)
    }

    /** 场景：保存 profile 带新头像 - 验证 avatarUrl 被传入 request */
    @Test
    fun saveProfile_withNewAvatar_avatarUrlInRequest() = runTest {
        val newAvatarUrl = "https://example.com/new-avatar.jpg"
        val uri = mockk<Uri>()
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile())
        coEvery { repository.uploadAvatar(uri) } returns Result.Success(newAvatarUrl)
        val requestSlot = slot<UpdateProfileRequest>()
        coEvery { repository.updateProfile(capture(requestSlot)) } returns Result.Success(fakeProfile())

        val vm = EditProfileViewModel(repository)
        vm.updateAvatar(uri)
        vm.saveProfile()

        assertEquals(newAvatarUrl, requestSlot.captured.avatarUrl)
    }

    // ========================================================================
    // 保存 profile - 失败
    // ========================================================================

    /** 场景：保存 profile API 失败 */
    @Test
    fun saveProfile_apiFailure_setsError() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile())
        coEvery { repository.updateProfile(any()) } returns Result.Error(40000, "参数无效")

        val vm = EditProfileViewModel(repository)
        vm.saveProfile()

        val state = vm.uiState.value
        assertFalse(state.isSaving)
        assertFalse(state.isSuccess)
        assertEquals("参数无效", state.error)
    }

    /** 场景：头像上传失败 - 不继续保存 profile */
    @Test
    fun saveProfile_avatarUploadFailure_setsError() = runTest {
        val uri = mockk<Uri>()
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile())
        coEvery { repository.uploadAvatar(uri) } returns Result.Error(40602, "图片超过5MB")

        val vm = EditProfileViewModel(repository)
        vm.updateAvatar(uri)
        vm.saveProfile()

        val state = vm.uiState.value
        assertFalse(state.isSaving)
        assertFalse(state.isSuccess)
        assertTrue(state.error!!.contains("头像上传失败"))

        // updateProfile 不应被调用
        coVerify(exactly = 0) { repository.updateProfile(any()) }
    }

    // ========================================================================
    // Loading / 保存中状态
    // ========================================================================

    /** 场景：保存期间 isSaving 为 true（初始 isLoading = true，init 后 isLoading = false） */
    @Test
    fun init_isLoadingTrue_becomesFalseAfterLoad() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile())

        val vm = EditProfileViewModel(repository)

        // init 结束后 isLoading 应为 false
        assertFalse(vm.uiState.value.isLoading)
    }

    /** 场景：保存中 isSaving 不允许重复提交 */
    @Test
    fun saveProfile_alreadySaving_doesNotReenter() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile())

        // 用 CompletableDeferred 控制 updateProfile 返回，使其在 UnconfinedTestDispatcher 下也能挂起
        val updateDeferred = CompletableDeferred<Result<UserProfileResponse>>()
        coEvery { repository.updateProfile(any()) } coAnswers {
            updateDeferred.await()
        }

        val vm = EditProfileViewModel(repository)

        // 第一次 saveProfile：launch 会内联执行到 updateProfile 的挂起点
        // coAnswers 中的 await() 会真正挂起协程，控制权返回给测试
        vm.saveProfile()

        // 此时 isSaving 应为 true（协程挂起在 updateProfile）
        assertTrue(vm.uiState.value.isSaving)

        // 第二次 saveProfile：isSaving=true，应被 guard 拦截
        vm.saveProfile()

        // 让第一次调用的协程完成
        updateDeferred.complete(Result.Success(fakeProfile()))

        // updateProfile 只被调用一次
        coVerify(exactly = 1) { repository.updateProfile(any()) }
    }

    // ========================================================================
    // errorShown 清除错误
    // ========================================================================

    /** 场景：errorShown 将 error 置为 null */
    @Test
    fun errorShown_clearsError() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Error(50000, "失败")

        val vm = EditProfileViewModel(repository)
        assertNotNull(vm.uiState.value.error)

        vm.errorShown()
        assertNull(vm.uiState.value.error)
    }

    // ========================================================================
    // 空白字段处理
    // ========================================================================

    /** 场景：昵称为空白时 request 中 nickname 为 null */
    @Test
    fun saveProfile_blankNickname_sendsNullNickname() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile(nickname = "原昵称"))
        val requestSlot = slot<UpdateProfileRequest>()
        coEvery { repository.updateProfile(capture(requestSlot)) } returns Result.Success(fakeProfile())

        val vm = EditProfileViewModel(repository)
        vm.updateNickname("  ")  // 空白字符串
        vm.saveProfile()

        assertNull(requestSlot.captured.nickname)
    }

    /** 场景：简介为空白时 request 中 bio 为 null */
    @Test
    fun saveProfile_blankBio_sendsNullBio() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile())
        val requestSlot = slot<UpdateProfileRequest>()
        coEvery { repository.updateProfile(capture(requestSlot)) } returns Result.Success(fakeProfile())

        val vm = EditProfileViewModel(repository)
        vm.updateBio("  ")  // 空白字符串
        vm.saveProfile()

        assertNull(requestSlot.captured.bio)
    }
}
