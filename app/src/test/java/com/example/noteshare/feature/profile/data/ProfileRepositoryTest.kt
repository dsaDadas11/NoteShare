package com.example.noteshare.feature.profile.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.core.network.ApiResponse
import com.example.noteshare.core.network.PageData
import com.example.noteshare.core.network.UploadApi
import com.example.noteshare.feature.feed.domain.model.NoteResponse
import com.example.noteshare.feature.feed.domain.model.UserBrief
import com.example.noteshare.feature.profile.domain.model.UpdateProfileRequest
import com.example.noteshare.feature.profile.domain.model.UserProfileResponse
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import android.os.ParcelFileDescriptor

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProfileRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var userApi: UserApi
    private lateinit var uploadApi: UploadApi
    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var repository: ProfileRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userApi = mockk(relaxed = true)
        uploadApi = mockk(relaxed = true)
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        every { context.contentResolver } returns contentResolver
        repository = ProfileRepository(userApi, uploadApi, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    private fun fakeProfile(id: Long = 1L) = UserProfileResponse(
        id = id,
        username = "testuser",
        nickname = "测试用户",
        avatarUrl = "https://example.com/avatar.jpg",
        bio = "简介",
        followerCount = 10,
        followingCount = 5,
        noteCount = 3,
        isFollowing = false
    )

    private fun fakeSuccessResponse(data: UserProfileResponse) = ApiResponse(
        code = ErrorCode.SUCCESS,
        message = "success",
        data = data
    )

    private fun fakeErrorResponse(code: Int, message: String) = ApiResponse<UserProfileResponse>(
        code = code,
        message = message,
        data = null
    )

    private fun fakeNotesPage() = PageData(
        items = listOf(
            NoteResponse(
                id = 1L,
                title = "Note 1",
                content = "Content 1",
                createdAt = "2024-01-01",
                author = UserBrief(1L, "testuser")
            )
        ),
        page = 1,
        pageSize = 20,
        total = 5,
        hasMore = true
    )

    // ========================================================================
    // getMyProfile
    // ========================================================================

    /** 场景：获取自己的资料 - 成功 */
    @Test
    fun getMyProfile_success_returnsSuccess() = runTest {
        val profile = fakeProfile()
        coEvery { userApi.getMyProfile() } returns fakeSuccessResponse(profile)

        val result = repository.getMyProfile()

        assertTrue(result is Result.Success)
        assertEquals("testuser", (result as Result.Success).data.username)
    }

    /** 场景：获取自己的资料 - 服务器返回业务错误 */
    @Test
    fun getMyProfile_serverError_returnsError() = runTest {
        coEvery { userApi.getMyProfile() } returns fakeErrorResponse(50000, "服务器内部错误")

        val result = repository.getMyProfile()

        assertTrue(result is Result.Error)
        assertEquals(50000, (result as Result.Error).code)
        assertEquals("服务器内部错误", result.message)
    }

    /** 场景：获取自己的资料 - 网络异常 */
    @Test
    fun getMyProfile_networkException_returnsNetworkError() = runTest {
        coEvery { userApi.getMyProfile() } throws RuntimeException("timeout")

        val result = repository.getMyProfile()

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
        assertTrue(result.message.contains("timeout"))
    }

    // ========================================================================
    // getUserProfile
    // ========================================================================

    /** 场景：获取他人资料 - 成功 */
    @Test
    fun getUserProfile_success_returnsSuccess() = runTest {
        val profile = fakeProfile(id = 100L)
        coEvery { userApi.getUserProfile(100L) } returns fakeSuccessResponse(profile)

        val result = repository.getUserProfile(100L)

        assertTrue(result is Result.Success)
        assertEquals(100L, (result as Result.Success).data.id)
    }

    /** 场景：获取他人资料 - 用户不存在 */
    @Test
    fun getUserProfile_notFound_returnsError() = runTest {
        coEvery { userApi.getUserProfile(999L) } returns fakeErrorResponse(40200, "用户不存在")

        val result = repository.getUserProfile(999L)

        assertTrue(result is Result.Error)
        assertEquals(40200, (result as Result.Error).code)
    }

    /** 场景：获取他人资料 - 网络异常 */
    @Test
    fun getUserProfile_networkException_returnsNetworkError() = runTest {
        coEvery { userApi.getUserProfile(any()) } throws RuntimeException("connection refused")

        val result = repository.getUserProfile(100L)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
    }

    // ========================================================================
    // updateProfile
    // ========================================================================

    /** 场景：更新资料 - 成功 */
    @Test
    fun updateProfile_success_returnsSuccess() = runTest {
        val request = UpdateProfileRequest(nickname = "新昵称", bio = "新简介")
        val updatedProfile = fakeProfile().copy(nickname = "新昵称", bio = "新简介")
        coEvery { userApi.updateProfile(request) } returns fakeSuccessResponse(updatedProfile)

        val result = repository.updateProfile(request)

        assertTrue(result is Result.Success)
        assertEquals("新昵称", (result as Result.Success).data.nickname)
    }

    /** 场景：更新资料 - 参数无效 */
    @Test
    fun updateProfile_invalidParam_returnsError() = runTest {
        val request = UpdateProfileRequest(nickname = "")
        coEvery { userApi.updateProfile(request) } returns fakeErrorResponse(40000, "参数无效")

        val result = repository.updateProfile(request)

        assertTrue(result is Result.Error)
        assertEquals(40000, (result as Result.Error).code)
    }

    /** 场景：更新资料 - 网络异常 */
    @Test
    fun updateProfile_networkException_returnsNetworkError() = runTest {
        coEvery { userApi.updateProfile(any()) } throws RuntimeException("IO error")

        val result = repository.updateProfile(UpdateProfileRequest(nickname = "测试"))

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
    }

    // ========================================================================
    // followUser
    // ========================================================================

    /** 场景：关注用户 - 成功 */
    @Test
    fun followUser_success_returnsSuccess() = runTest {
        coEvery { userApi.followUser(100L) } returns ApiResponse(
            code = ErrorCode.SUCCESS, message = "success", data = Unit
        )

        val result = repository.followUser(100L)

        assertTrue(result is Result.Success)
    }

    /** 场景：关注用户 - 不能关注自己 */
    @Test
    fun followUser_cannotFollowSelf_returnsError() = runTest {
        coEvery { userApi.followUser(1L) } returns ApiResponse(
            code = ErrorCode.CANNOT_FOLLOW_SELF, message = "不能关注自己", data = null
        )

        val result = repository.followUser(1L)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.CANNOT_FOLLOW_SELF, (result as Result.Error).code)
    }

    /** 场景：关注用户 - 已经关注 */
    @Test
    fun followUser_alreadyFollowing_returnsError() = runTest {
        coEvery { userApi.followUser(100L) } returns ApiResponse(
            code = ErrorCode.ALREADY_FOLLOWING, message = "已经关注", data = null
        )

        val result = repository.followUser(100L)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.ALREADY_FOLLOWING, (result as Result.Error).code)
    }

    /** 场景：关注用户 - 网络异常 */
    @Test
    fun followUser_networkException_returnsNetworkError() = runTest {
        coEvery { userApi.followUser(any()) } throws RuntimeException("timeout")

        val result = repository.followUser(100L)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
    }

    // ========================================================================
    // unfollowUser
    // ========================================================================

    /** 场景：取消关注 - 成功 */
    @Test
    fun unfollowUser_success_returnsSuccess() = runTest {
        coEvery { userApi.unfollowUser(100L) } returns ApiResponse(
            code = ErrorCode.SUCCESS, message = "success", data = Unit
        )

        val result = repository.unfollowUser(100L)

        assertTrue(result is Result.Success)
    }

    /** 场景：取消关注 - 未关注该用户 */
    @Test
    fun unfollowUser_notFollowing_returnsError() = runTest {
        coEvery { userApi.unfollowUser(100L) } returns ApiResponse(
            code = ErrorCode.NOT_FOLLOWING, message = "未关注该用户", data = null
        )

        val result = repository.unfollowUser(100L)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NOT_FOLLOWING, (result as Result.Error).code)
    }

    /** 场景：取消关注 - 网络异常 */
    @Test
    fun unfollowUser_networkException_returnsNetworkError() = runTest {
        coEvery { userApi.unfollowUser(any()) } throws RuntimeException("connection reset")

        val result = repository.unfollowUser(100L)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
    }

    // ========================================================================
    // getUserNotes
    // ========================================================================

    /** 场景：获取用户笔记 - 成功 */
    @Test
    fun getUserNotes_success_returnsSuccess() = runTest {
        val pageData = fakeNotesPage()
        coEvery { userApi.getUserNotes(1L, 1) } returns ApiResponse(
            code = ErrorCode.SUCCESS, message = "success", data = pageData
        )

        val result = repository.getUserNotes(1L, 1)

        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.items.size)
        assertEquals(5, result.data.total)
        assertTrue(result.data.hasMore)
    }

    /** 场景：获取用户笔记 - 用户不存在 */
    @Test
    fun getUserNotes_notFound_returnsError() = runTest {
        coEvery { userApi.getUserNotes(999L, 1) } returns ApiResponse(
            code = 40200, message = "用户不存在", data = null
        )

        val result = repository.getUserNotes(999L, 1)

        assertTrue(result is Result.Error)
        assertEquals(40200, (result as Result.Error).code)
    }

    /** 场景：获取用户笔记 - 分页参数传递正确（page=2） */
    @Test
    fun getUserNotes_page2_passesCorrectParam() = runTest {
        coEvery { userApi.getUserNotes(1L, 2) } returns ApiResponse(
            code = ErrorCode.SUCCESS, message = "success", data = fakeNotesPage()
        )

        val result = repository.getUserNotes(1L, 2)

        assertTrue(result is Result.Success)
    }

    /** 场景：获取用户笔记 - 网络异常 */
    @Test
    fun getUserNotes_networkException_returnsNetworkError() = runTest {
        coEvery { userApi.getUserNotes(any(), any()) } throws RuntimeException("timeout")

        val result = repository.getUserNotes(1L, 1)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
    }

    /** 场景：获取用户笔记 - 空列表 */
    @Test
    fun getUserNotes_emptyList_returnsEmptyPage() = runTest {
        val emptyPage = PageData<NoteResponse>(
            items = emptyList(),
            page = 1,
            pageSize = 20,
            total = 0,
            hasMore = false
        )
        coEvery { userApi.getUserNotes(1L, 1) } returns ApiResponse(
            code = ErrorCode.SUCCESS, message = "success", data = emptyPage
        )

        val result = repository.getUserNotes(1L, 1)

        assertTrue(result is Result.Success)
        assertEquals(0, (result as Result.Success).data.items.size)
        assertFalse(result.data.hasMore)
    }

    // ========================================================================
    // uploadAvatar
    // ========================================================================

    /** 场景：头像上传 - 成功 */
    @Test
    fun uploadAvatar_success_returnsUrl() = runTest {
        val uri = mockk<Uri>()
        val mockPfd = mockk<ParcelFileDescriptor>(relaxed = true)
        every { mockPfd.statSize } returns 1024L

        coEvery { contentResolver.getType(uri) } returns "image/jpeg"
        coEvery { contentResolver.openFileDescriptor(uri, "r") } returns mockPfd
        coEvery { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(ByteArray(1024) { it.toByte() })
        coEvery { uploadApi.uploadImage(any()) } returns ApiResponse(
            code = ErrorCode.SUCCESS, message = "success", data = "https://example.com/uploaded.jpg"
        )

        val result = repository.uploadAvatar(uri)

        assertTrue(result is Result.Success)
        assertEquals("https://example.com/uploaded.jpg", (result as Result.Success).data)
    }

    /** 场景：头像上传 - 文件过大（超过 5MB） */
    @Test
    fun uploadAvatar_fileTooLarge_returnsError() = runTest {
        val uri = mockk<Uri>()
        val mockPfd = mockk<ParcelFileDescriptor>(relaxed = true)
        every { mockPfd.statSize } returns 6L * 1024L * 1024L // 6MB

        coEvery { contentResolver.getType(uri) } returns "image/jpeg"
        coEvery { contentResolver.openFileDescriptor(uri, "r") } returns mockPfd

        val result = repository.uploadAvatar(uri)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_TOO_LARGE, (result as Result.Error).code)
        assertTrue(result.message.contains("5MB"))
    }

    /** 场景：头像上传 - 无法读取图片内容 */
    @Test
    fun uploadAvatar_cannotRead_returnsError() = runTest {
        val uri = mockk<Uri>()
        val mockPfd = mockk<ParcelFileDescriptor>(relaxed = true)
        every { mockPfd.statSize } returns 1024L

        coEvery { contentResolver.getType(uri) } returns "image/jpeg"
        coEvery { contentResolver.openFileDescriptor(uri, "r") } returns mockPfd
        coEvery { contentResolver.openInputStream(uri) } returns null

        val result = repository.uploadAvatar(uri)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.INVALID_PARAMETER, (result as Result.Error).code)
        assertTrue(result.message.contains("无法读取"))
    }

    /** 场景：头像上传 - 服务器返回错误 */
    @Test
    fun uploadAvatar_serverError_returnsError() = runTest {
        val uri = mockk<Uri>()
        val mockPfd = mockk<ParcelFileDescriptor>(relaxed = true)
        every { mockPfd.statSize } returns 1024L

        coEvery { contentResolver.getType(uri) } returns "image/png"
        coEvery { contentResolver.openFileDescriptor(uri, "r") } returns mockPfd
        coEvery { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(ByteArray(1024) { it.toByte() })
        coEvery { uploadApi.uploadImage(any()) } returns ApiResponse(
            code = ErrorCode.FILE_TYPE_INVALID, message = "文件类型不支持"
        )

        val result = repository.uploadAvatar(uri)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_TYPE_INVALID, (result as Result.Error).code)
    }

    /** 场景：头像上传 - 网络异常 */
    @Test
    fun uploadAvatar_networkException_returnsNetworkError() = runTest {
        val uri = mockk<Uri>()
        val mockPfd = mockk<ParcelFileDescriptor>(relaxed = true)
        every { mockPfd.statSize } returns 1024L

        coEvery { contentResolver.getType(uri) } returns "image/jpeg"
        coEvery { contentResolver.openFileDescriptor(uri, "r") } returns mockPfd
        coEvery { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(ByteArray(100) { it.toByte() })
        coEvery { uploadApi.uploadImage(any()) } throws RuntimeException("socket timeout")

        val result = repository.uploadAvatar(uri)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
        assertTrue(result.message.contains("socket timeout"))
    }

    /** 场景：头像上传 - mimeType 为 null 时默认使用 image/jpeg */
    @Test
    fun uploadAvatar_nullMimeType_defaultsToJpeg() = runTest {
        val uri = mockk<Uri>()
        val mockPfd = mockk<ParcelFileDescriptor>(relaxed = true)
        every { mockPfd.statSize } returns 512L

        coEvery { contentResolver.getType(uri) } returns null
        coEvery { contentResolver.openFileDescriptor(uri, "r") } returns mockPfd
        coEvery { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(ByteArray(512) { it.toByte() })
        coEvery { uploadApi.uploadImage(any()) } returns ApiResponse(
            code = ErrorCode.SUCCESS, message = "success", data = "https://example.com/avatar.jpg"
        )

        val result = repository.uploadAvatar(uri)

        assertTrue(result is Result.Success)
    }

    /** 场景：头像上传 - statSize 返回 -1 时跳过 statSize 检查，继续读取字节检查 */
    @Test
    fun uploadAvatar_statSizeNegative_checksByteSize() = runTest {
        val uri = mockk<Uri>()
        val mockPfd = mockk<ParcelFileDescriptor>(relaxed = true)
        every { mockPfd.statSize } returns -1L

        val smallBytes = ByteArray(100) { it.toByte() }

        coEvery { contentResolver.getType(uri) } returns "image/jpeg"
        coEvery { contentResolver.openFileDescriptor(uri, "r") } returns mockPfd
        coEvery { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(smallBytes)
        coEvery { uploadApi.uploadImage(any()) } returns ApiResponse(
            code = ErrorCode.SUCCESS, message = "success", data = "https://example.com/avatar.jpg"
        )

        val result = repository.uploadAvatar(uri)

        // statSize = -1 < MAX_UPLOAD_BYTES, 字节数 100 < MAX, 上传成功
        assertTrue(result is Result.Success)
    }
}
