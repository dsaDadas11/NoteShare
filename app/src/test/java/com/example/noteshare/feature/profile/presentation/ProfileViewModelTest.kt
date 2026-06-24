package com.example.noteshare.feature.profile.presentation

import com.example.noteshare.core.common.Result
import com.example.noteshare.feature.profile.data.ProfileRepository
import com.example.noteshare.feature.profile.domain.model.UserProfileResponse
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class ProfileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: ProfileRepository

    // 使用固定的 userId 模拟 SavedStateHandle 中的 "userId"
    private val otherUserId = 100L

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

    /**
     * 创建一个 mock SavedStateHandle，模拟导航参数中 userId = null（即自己的主页）
     */
    private fun mySavedStateHandle(): androidx.lifecycle.SavedStateHandle {
        return androidx.lifecycle.SavedStateHandle(emptyMap())
    }

    /**
     * 创建一个 mock SavedStateHandle，模拟导航参数中 userId = otherUserId
     */
    private fun otherSavedStateHandle(): androidx.lifecycle.SavedStateHandle {
        return androidx.lifecycle.SavedStateHandle(mapOf("userId" to otherUserId.toString()))
    }

    private fun fakeProfile(
        id: Long = 1L,
        username: String = "testuser",
        nickname: String? = "测试用户",
        isFollowing: Boolean? = null,
        followerCount: Int = 0,
        noteCount: Int = 5
    ) = UserProfileResponse(
        id = id,
        username = username,
        nickname = nickname,
        avatarUrl = "https://example.com/avatar.jpg",
        bio = "简介",
        followerCount = followerCount,
        followingCount = 10,
        noteCount = noteCount,
        isFollowing = isFollowing
    )

    private fun fakeNotesPage(
        page: Int = 1,
        total: Long = 10,
        hasMore: Boolean = true,
        count: Int = 3
    ) = com.example.noteshare.core.network.PageData(
        items = (1..count).map { i ->
            com.example.noteshare.feature.feed.domain.model.NoteResponse(
                id = (page - 1) * 20L + i,
                title = "Note $i",
                content = "Content $i",
                likeCount = i,
                commentCount = 0,
                createdAt = "2024-01-01T00:00:00Z",
                author = com.example.noteshare.feature.feed.domain.model.UserBrief(
                    id = 1L,
                    username = "testuser"
                )
            )
        },
        page = page,
        pageSize = 20,
        total = total,
        hasMore = hasMore
    )

    // ========================================================================
    // 加载自己的主页
    // ========================================================================

    /** 场景：加载自己的主页 - 成功返回 profile */
    @Test
    fun loadProfile_myProfile_success() = runTest {
        val profile = fakeProfile(id = 1L)
        coEvery { repository.getMyProfile() } returns Result.Success(profile)
        coEvery { repository.getUserNotes(1L, 1) } returns Result.Success(fakeNotesPage())

        val vm = ProfileViewModel(repository, mySavedStateHandle())

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.profile)
        assertEquals("testuser", state.profile?.username)
        assertTrue(state.isMyProfile)
        assertNull(state.error)
    }

    // ========================================================================
    // 加载他人的主页
    // ========================================================================

    /** 场景：加载他人主页 - 成功返回 profile，isMyProfile 应为 false */
    @Test
    fun loadProfile_otherProfile_success() = runTest {
        val profile = fakeProfile(id = otherUserId, isFollowing = false, followerCount = 50)
        coEvery { repository.getUserProfile(otherUserId) } returns Result.Success(profile)
        coEvery { repository.getUserNotes(otherUserId, 1) } returns Result.Success(fakeNotesPage())

        val vm = ProfileViewModel(repository, otherSavedStateHandle())

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isMyProfile)
        assertEquals(otherUserId, state.profile?.id)
        assertFalse(state.profile?.isFollowing!!)
    }

    // ========================================================================
    // 加载失败
    // ========================================================================

    /** 场景：加载自己的 profile 时 API 返回错误 */
    @Test
    fun loadProfile_myProfile_failure_setsError() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Error(50000, "服务器错误")

        val vm = ProfileViewModel(repository, mySavedStateHandle())

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.profile)
        assertEquals("服务器错误", state.error)
    }

    /** 场景：加载他人 profile 时 API 返回错误 */
    @Test
    fun loadProfile_otherProfile_failure_setsError() = runTest {
        coEvery { repository.getUserProfile(otherUserId) } returns Result.Error(40200, "用户不存在")

        val vm = ProfileViewModel(repository, otherSavedStateHandle())

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.profile)
        assertEquals("用户不存在", state.error)
    }

    /** 场景：网络异常时返回网络错误信息 */
    @Test
    fun loadProfile_networkException_setsNetworkError() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Error(-1, "网络请求失败: timeout")

        val vm = ProfileViewModel(repository, mySavedStateHandle())

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("网络请求失败: timeout", state.error)
    }

    // ========================================================================
    // 关注用户 - 乐观更新
    // ========================================================================

    /** 场景：关注未关注用户 - 乐观更新 isFollowing=true，followerCount+1 */
    @Test
    fun toggleFollow_followSuccess_optimisticUpdate() = runTest {
        // 使用 StandardTestDispatcher，让 toggleFollow 内的 launch 协程不会立即执行
        val stdDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(stdDispatcher)

        val profile = fakeProfile(id = otherUserId, isFollowing = false, followerCount = 10)
        coEvery { repository.getUserProfile(otherUserId) } returns Result.Success(profile)
        coEvery { repository.getUserNotes(otherUserId, 1) } returns Result.Success(fakeNotesPage())
        coEvery { repository.followUser(otherUserId) } returns Result.Success(Unit)

        val vm = ProfileViewModel(repository, otherSavedStateHandle())
        // 推进 init 中 loadProfile 的协程完成
        stdDispatcher.scheduler.advanceUntilIdle()
        // 等待初始化完成
        assertFalse(vm.uiState.value.isMyProfile)

        vm.toggleFollow()
        // 乐观更新是同步的（在 launch 之前），立即检查
        val afterToggle = vm.uiState.value
        assertTrue(afterToggle.profile?.isFollowing!!)
        assertEquals(11, afterToggle.profile?.followerCount)
        assertTrue(afterToggle.isFollowLoading)
    }

    /** 场景：关注用户 - API 成功返回后 isFollowLoading 重置 */
    @Test
    fun toggleFollow_followApiSuccess_loadingReset() = runTest {
        val profile = fakeProfile(id = otherUserId, isFollowing = false, followerCount = 10)
        coEvery { repository.getUserProfile(otherUserId) } returns Result.Success(profile)
        coEvery { repository.getUserNotes(otherUserId, 1) } returns Result.Success(fakeNotesPage())
        coEvery { repository.followUser(otherUserId) } returns Result.Success(Unit)

        val vm = ProfileViewModel(repository, otherSavedStateHandle())

        vm.toggleFollow()
        // 协程在 testDispatcher 上执行完
        val state = vm.uiState.value
        assertFalse(state.isFollowLoading)
        assertTrue(state.profile?.isFollowing!!)
    }

    // ========================================================================
    // 关注失败 - 回滚
    // ========================================================================

    /** 场景：关注用户 API 失败 - 回滚乐观更新 */
    @Test
    fun toggleFollow_followFailure_reverts() = runTest {
        val profile = fakeProfile(id = otherUserId, isFollowing = false, followerCount = 10)
        coEvery { repository.getUserProfile(otherUserId) } returns Result.Success(profile)
        coEvery { repository.getUserNotes(otherUserId, 1) } returns Result.Success(fakeNotesPage())
        coEvery { repository.followUser(otherUserId) } returns Result.Error(40210, "不能关注自己")

        val vm = ProfileViewModel(repository, otherSavedStateHandle())

        vm.toggleFollow()
        val state = vm.uiState.value
        assertFalse(state.isFollowLoading)
        assertFalse(state.profile?.isFollowing!!)
        assertEquals(10, state.profile?.followerCount)
        assertTrue(state.error?.contains("操作失败") == true)
    }

    // ========================================================================
    // 取消关注 - 乐观更新
    // ========================================================================

    /** 场景：取消关注已关注用户 - 乐观更新 isFollowing=false，followerCount-1 */
    @Test
    fun toggleFollow_unfollowSuccess_optimisticUpdate() = runTest {
        val profile = fakeProfile(id = otherUserId, isFollowing = true, followerCount = 10)
        coEvery { repository.getUserProfile(otherUserId) } returns Result.Success(profile)
        coEvery { repository.getUserNotes(otherUserId, 1) } returns Result.Success(fakeNotesPage())
        coEvery { repository.unfollowUser(otherUserId) } returns Result.Success(Unit)

        val vm = ProfileViewModel(repository, otherSavedStateHandle())

        vm.toggleFollow()
        val afterToggle = vm.uiState.value
        assertFalse(afterToggle.profile?.isFollowing!!)
        assertEquals(9, afterToggle.profile?.followerCount)
    }

    /** 场景：取消关注 - API 成功后 loading 重置 */
    @Test
    fun toggleFollow_unfollowApiSuccess_loadingReset() = runTest {
        val profile = fakeProfile(id = otherUserId, isFollowing = true, followerCount = 10)
        coEvery { repository.getUserProfile(otherUserId) } returns Result.Success(profile)
        coEvery { repository.getUserNotes(otherUserId, 1) } returns Result.Success(fakeNotesPage())
        coEvery { repository.unfollowUser(otherUserId) } returns Result.Success(Unit)

        val vm = ProfileViewModel(repository, otherSavedStateHandle())

        vm.toggleFollow()
        val state = vm.uiState.value
        assertFalse(state.isFollowLoading)
    }

    // ========================================================================
    // 取消关注失败 - 回滚
    // ========================================================================

    /** 场景：取消关注 API 失败 - 回滚乐观更新 */
    @Test
    fun toggleFollow_unfollowFailure_reverts() = runTest {
        val profile = fakeProfile(id = otherUserId, isFollowing = true, followerCount = 10)
        coEvery { repository.getUserProfile(otherUserId) } returns Result.Success(profile)
        coEvery { repository.getUserNotes(otherUserId, 1) } returns Result.Success(fakeNotesPage())
        coEvery { repository.unfollowUser(otherUserId) } returns Result.Error(40212, "未关注该用户")

        val vm = ProfileViewModel(repository, otherSavedStateHandle())

        vm.toggleFollow()
        val state = vm.uiState.value
        assertFalse(state.isFollowLoading)
        assertTrue(state.profile?.isFollowing!!)
        assertEquals(10, state.profile?.followerCount)
        assertTrue(state.error?.contains("操作失败") == true)
    }

    // ========================================================================
    // isSelf 判断逻辑
    // ========================================================================

    /** 场景：savedStateHandle 中无 userId 时 isMyProfile 为 true */
    @Test
    fun loadProfile_noUserId_isMyProfileTrue() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile())
        coEvery { repository.getUserNotes(any(), any()) } returns Result.Success(fakeNotesPage())

        val vm = ProfileViewModel(repository, mySavedStateHandle())

        assertTrue(vm.uiState.value.isMyProfile)
    }

    /** 场景：savedStateHandle 中有 userId 时 isMyProfile 为 false */
    @Test
    fun loadProfile_hasUserId_isMyProfileFalse() = runTest {
        coEvery { repository.getUserProfile(otherUserId) } returns Result.Success(fakeProfile(id = otherUserId))
        coEvery { repository.getUserNotes(otherUserId, 1) } returns Result.Success(fakeNotesPage())

        val vm = ProfileViewModel(repository, otherSavedStateHandle())

        assertFalse(vm.uiState.value.isMyProfile)
    }

    /** 场景：自己的主页不允许 toggleFollow */
    @Test
    fun toggleFollow_myProfile_doesNothing() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Success(fakeProfile())
        coEvery { repository.getUserNotes(any(), any()) } returns Result.Success(fakeNotesPage())

        val vm = ProfileViewModel(repository, mySavedStateHandle())

        vm.toggleFollow()
        // 没有发生任何变化
        coVerify(exactly = 0) { repository.followUser(any()) }
        coVerify(exactly = 0) { repository.unfollowUser(any()) }
    }

    // ========================================================================
    // 加载笔记列表
    // ========================================================================

    /** 场景：加载用户笔记成功 */
    @Test
    fun loadNotes_success_updatesNotesList() = runTest {
        val profile = fakeProfile()
        coEvery { repository.getMyProfile() } returns Result.Success(profile)
        coEvery { repository.getUserNotes(1L, 1) } returns Result.Success(fakeNotesPage(count = 3))

        val vm = ProfileViewModel(repository, mySavedStateHandle())

        val state = vm.uiState.value
        assertEquals(3, state.notes.size)
        assertEquals(1, state.notesCurrentPage)
        assertFalse(state.notesLoading)
    }

    /** 场景：笔记列表为空 */
    @Test
    fun loadNotes_emptyNotes_setsEmptyList() = runTest {
        val profile = fakeProfile()
        coEvery { repository.getMyProfile() } returns Result.Success(profile)
        coEvery { repository.getUserNotes(1L, 1) } returns Result.Success(fakeNotesPage(count = 0, total = 0, hasMore = false))

        val vm = ProfileViewModel(repository, mySavedStateHandle())

        val state = vm.uiState.value
        assertEquals(0, state.notes.size)
        assertFalse(state.notesHasMore)
    }

    // ========================================================================
    // 加载更多笔记
    // ========================================================================

    /** 场景：加载更多笔记成功 - 追加到列表 */
    @Test
    fun loadNotes_loadMore_appendsNotes() = runTest {
        val profile = fakeProfile()
        coEvery { repository.getMyProfile() } returns Result.Success(profile)
        coEvery { repository.getUserNotes(1L, 1) } returns Result.Success(fakeNotesPage(count = 3, total = 5, hasMore = true))
        coEvery { repository.getUserNotes(1L, 2) } returns Result.Success(fakeNotesPage(page = 2, count = 2, total = 5, hasMore = false))

        val vm = ProfileViewModel(repository, mySavedStateHandle())

        // 触发加载更多
        vm.loadNotes(isRefresh = false)

        val state = vm.uiState.value
        assertEquals(5, state.notes.size) // 3 + 2
        assertEquals(2, state.notesCurrentPage)
        assertFalse(state.notesHasMore)
        assertFalse(state.notesLoading)
    }

    /** 场景：加载更多笔记失败 - 设置 loadMoreFailed */
    @Test
    fun loadNotes_loadMore_failure_setsLoadMoreFailed() = runTest {
        val profile = fakeProfile()
        coEvery { repository.getMyProfile() } returns Result.Success(profile)
        coEvery { repository.getUserNotes(1L, 1) } returns Result.Success(fakeNotesPage(count = 3, hasMore = true))
        coEvery { repository.getUserNotes(1L, 2) } returns Result.Error(50000, "服务器错误")

        val vm = ProfileViewModel(repository, mySavedStateHandle())

        vm.loadNotes(isRefresh = false)

        val state = vm.uiState.value
        assertTrue(state.loadMoreFailed)
        assertFalse(state.notesLoading)
        assertEquals("服务器错误", state.error)
    }

    /** 场景：loadMoreFailed 时再次调用 loadNotes 应直接 return，不发请求 */
    @Test
    fun loadNotes_loadMoreFailed_doesNotLoad() = runTest {
        val profile = fakeProfile()
        coEvery { repository.getMyProfile() } returns Result.Success(profile)
        coEvery { repository.getUserNotes(1L, 1) } returns Result.Success(fakeNotesPage(count = 3, hasMore = true))
        coEvery { repository.getUserNotes(1L, 2) } returns Result.Error(50000, "服务器错误")

        val vm = ProfileViewModel(repository, mySavedStateHandle())
        vm.loadNotes(isRefresh = false) // 失败，loadMoreFailed = true
        vm.loadNotes(isRefresh = false) // 不应再发请求

        coVerify(exactly = 1) { repository.getUserNotes(1L, 2) }
    }

    /** 场景：notesHasMore = false 时调用 loadNotes 不发请求 */
    @Test
    fun loadNotes_noMore_doesNotLoad() = runTest {
        val profile = fakeProfile()
        coEvery { repository.getMyProfile() } returns Result.Success(profile)
        coEvery { repository.getUserNotes(1L, 1) } returns Result.Success(fakeNotesPage(hasMore = false))

        val vm = ProfileViewModel(repository, mySavedStateHandle())
        vm.loadNotes(isRefresh = false)

        // 只在 init 时调用了 getUserNotes，loadMore 未触发
        coVerify(exactly = 1) { repository.getUserNotes(1L, 1) }
    }

    // ========================================================================
    // 下拉刷新
    // ========================================================================

    /** 场景：refresh 重新加载 profile 和笔记 */
    @Test
    fun refresh_reloadsProfileAndNotes() = runTest {
        val profile = fakeProfile()
        coEvery { repository.getMyProfile() } returns Result.Success(profile)
        coEvery { repository.getUserNotes(1L, 1) } returns Result.Success(fakeNotesPage(count = 2))

        val vm = ProfileViewModel(repository, mySavedStateHandle())
        // 第一次 init 已经调用一次，现在 refresh
        vm.refresh()

        coVerify(atLeast = 2) { repository.getMyProfile() }
        coVerify(atLeast = 2) { repository.getUserNotes(1L, 1) }
    }

    // ========================================================================
    // errorShown 清除错误
    // ========================================================================

    /** 场景：errorShown 将 error 置为 null */
    @Test
    fun errorShown_clearsError() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Error(50000, "错误")

        val vm = ProfileViewModel(repository, mySavedStateHandle())
        assertNotNull(vm.uiState.value.error)

        vm.errorShown()
        assertNull(vm.uiState.value.error)
    }

    // ========================================================================
    // 笔记加载失败
    // ========================================================================

    /** 场景：笔记加载失败 - error 被设置 */
    @Test
    fun loadNotes_failure_setsError() = runTest {
        val profile = fakeProfile()
        coEvery { repository.getMyProfile() } returns Result.Success(profile)
        coEvery { repository.getUserNotes(1L, 1) } returns Result.Error(50000, "笔记加载失败")

        val vm = ProfileViewModel(repository, mySavedStateHandle())

        val state = vm.uiState.value
        assertFalse(state.notesLoading)
        assertEquals("笔记加载失败", state.error)
    }

    // ========================================================================
    // 无 profile 时调用 loadNotes 不发请求
    // ========================================================================

    /** 场景：profile 为 null 时调用 loadNotes 不会发请求 */
    @Test
    fun loadNotes_noProfile_doesNothing() = runTest {
        coEvery { repository.getMyProfile() } returns Result.Error(50000, "加载失败")

        val vm = ProfileViewModel(repository, mySavedStateHandle())
        vm.loadNotes(isRefresh = true)

        coVerify(exactly = 0) { repository.getUserNotes(any(), any()) }
    }
}
