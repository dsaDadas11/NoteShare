package com.example.noteshare.feature.publish.presentation

import android.app.Application
import android.net.Uri
import app.cash.turbine.test
import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.feature.publish.data.PublishRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * PublishViewModel 单元测试
 * 覆盖：标题/正文验证、图片/视频选择与互斥、删除操作、发布成功/失败、loading状态
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PublishViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: PublishRepository
    private lateinit var viewModel: PublishViewModel

    // 预构造的 mock Uri
    private lateinit var uri1: Uri
    private lateinit var uri2: Uri
    private lateinit var uri3: Uri
    private lateinit var uri4: Uri
    private lateinit var videoUri: Uri

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = PublishViewModel(repository)

        // 使用 Uri.parse() 创建真实的 Uri 实例（Robolectric 会提供真实实现）
        uri1 = Uri.parse("content://media/image1")
        uri2 = Uri.parse("content://media/image2")
        uri3 = Uri.parse("content://media/image3")
        uri4 = Uri.parse("content://media/image4")
        videoUri = Uri.parse("content://media/video1")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ======== 标题验证 ========

    /**
     * 标题为空字符串时，发布应报错
     */
    @Test
    fun updateTitle_emptyTitle_publishShouldFail() = runTest {
        viewModel.updateTitle("")
        viewModel.updateContent("Some content here")
        viewModel.publish()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("标题长度必须在 1-100 之间", state.error)
        assertFalse(state.isSuccess)
    }

    /**
     * 标题只有空白字符（如空格），应视为无效
     */
    @Test
    fun updateTitle_blankTitle_publishShouldFail() = runTest {
        viewModel.updateTitle("   ")
        viewModel.updateContent("Some content here")
        viewModel.publish()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("标题长度必须在 1-100 之间", state.error)
        assertFalse(state.isSuccess)
    }

    /**
     * 标题为1个字符（边界值下限），验证可通过
     */
    @Test
    fun updateTitle_oneCharacter_publishShouldSucceed() = runTest {
        coEvery { repository.createNote(any(), any(), any(), any()) } returns Result.Success(mockk())

        viewModel.updateTitle("A")
        viewModel.updateContent("Some content here")
        viewModel.publish()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertTrue(state.isSuccess)
    }

    /**
     * 标题为100个字符（边界值上限），验证可通过
     */
    @Test
    fun updateTitle_hundredCharacters_publishShouldSucceed() = runTest {
        coEvery { repository.createNote(any(), any(), any(), any()) } returns Result.Success(mockk())

        val title100 = "A".repeat(100)
        viewModel.updateTitle(title100)
        viewModel.updateContent("Some content here")
        viewModel.publish()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertTrue(state.isSuccess)
    }

    /**
     * 标题为101个字符（超过上限），updateTitle应截断不更新
     */
    @Test
    fun updateTitle_hundredOneCharacters_titleShouldNotExceed100() = runTest {
        val title100 = "A".repeat(100)
        viewModel.updateTitle(title100)
        assertEquals(title100, viewModel.uiState.value.title)

        // 尝试输入101字符，ViewModel会拒绝更新
        val title101 = "A".repeat(101)
        viewModel.updateTitle(title101)
        // title 仍应保持为100字符
        assertEquals(100, viewModel.uiState.value.title.length)
    }

    // ======== 正文验证 ========

    /**
     * 正文为空字符串时，发布应报错
     */
    @Test
    fun updateContent_emptyContent_publishShouldFail() = runTest {
        viewModel.updateTitle("Valid Title")
        viewModel.updateContent("")
        viewModel.publish()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("正文长度必须在 1-5000 之间", state.error)
        assertFalse(state.isSuccess)
    }

    /**
     * 正文为1个字符（边界值下限），验证可通过
     */
    @Test
    fun updateContent_oneCharacter_publishShouldSucceed() = runTest {
        coEvery { repository.createNote(any(), any(), any(), any()) } returns Result.Success(mockk())

        viewModel.updateTitle("Valid Title")
        viewModel.updateContent("X")
        viewModel.publish()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertTrue(state.isSuccess)
    }

    /**
     * 正文为5000个字符（边界值上限），验证可通过
     */
    @Test
    fun updateContent_fiveThousandCharacters_publishShouldSucceed() = runTest {
        coEvery { repository.createNote(any(), any(), any(), any()) } returns Result.Success(mockk())

        val content5000 = "X".repeat(5000)
        viewModel.updateTitle("Valid Title")
        viewModel.updateContent(content5000)
        viewModel.publish()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertTrue(state.isSuccess)
    }

    /**
     * 正文为5001个字符（超过上限），updateContent应截断不更新
     */
    @Test
    fun updateContent_fiveThousandOneCharacters_contentShouldNotExceed5000() = runTest {
        val content5000 = "X".repeat(5000)
        viewModel.updateContent(content5000)
        assertEquals(5000, viewModel.uiState.value.content.length)

        val content5001 = "X".repeat(5001)
        viewModel.updateContent(content5001)
        assertEquals(5000, viewModel.uiState.value.content.length)
    }

    // ======== 图片选择 ========

    /**
     * 添加图片后状态正确更新
     */
    @Test
    fun addImages_success_imagesAdded() = runTest {
        viewModel.addImages(listOf(uri1))
        val state = viewModel.uiState.value
        assertEquals(1, state.selectedImages.size)
        assertEquals(uri1, state.selectedImages[0])
        assertNull(state.selectedVideo) // 选图时清空视频
    }

    /**
     * 添加超过3张图片时，应截断到3张并提示错误
     */
    @Test
    fun addImages_moreThanThree_imagesCappedAndErrorShown() = runTest {
        viewModel.addImages(listOf(uri1, uri2, uri3, uri4))
        val state = viewModel.uiState.value
        assertEquals(3, state.selectedImages.size)
        assertEquals("最多只能选择 3 张图片", state.error)
    }

    // ======== 视频选择 ========

    /**
     * 添加视频后，图片列表应被清空
     */
    @Test
    fun setVideo_withExistingImages_imagesCleared() = runTest {
        viewModel.addImages(listOf(uri1, uri2))
        assertEquals(2, viewModel.uiState.value.selectedImages.size)

        viewModel.setVideo(videoUri)
        val state = viewModel.uiState.value
        assertEquals(videoUri, state.selectedVideo)
        assertTrue(state.selectedImages.isEmpty())
    }

    /**
     * 添加图片后，视频应被清空（图片和视频互斥）
     */
    @Test
    fun addImages_withExistingVideo_videoCleared() = runTest {
        viewModel.setVideo(videoUri)
        assertEquals(videoUri, viewModel.uiState.value.selectedVideo)

        viewModel.addImages(listOf(uri1))
        val state = viewModel.uiState.value
        assertTrue(state.selectedImages.isNotEmpty())
        assertNull(state.selectedVideo)
    }

    // ======== 删除操作 ========

    /**
     * 删除图片后列表正确更新
     */
    @Test
    fun removeImage_existingImage_imageRemoved() = runTest {
        viewModel.addImages(listOf(uri1, uri2))
        assertEquals(2, viewModel.uiState.value.selectedImages.size)

        viewModel.removeImage(uri1)
        assertEquals(1, viewModel.uiState.value.selectedImages.size)
        assertEquals(uri2, viewModel.uiState.value.selectedImages[0])
    }

    /**
     * 删除视频后，selectedVideo变为null
     */
    @Test
    fun removeVideo_existingVideo_videoCleared() = runTest {
        viewModel.setVideo(videoUri)
        assertNotNull(viewModel.uiState.value.selectedVideo)

        viewModel.removeVideo()
        assertNull(viewModel.uiState.value.selectedVideo)
    }

    // ======== 发布流程 ========

    /**
     * 标题为空时直接发布，应报错且不调用repository
     */
    @Test
    fun publish_emptyTitle_shouldNotCallRepository() = runTest {
        viewModel.publish()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.createNote(any(), any(), any(), any()) }
        assertEquals("标题长度必须在 1-100 之间", viewModel.uiState.value.error)
    }

    /**
     * 发布成功：所有验证通过，创建笔记成功，isSuccess为true
     */
    @Test
    fun publish_allValid_createNoteSuccess() = runTest {
        coEvery { repository.createNote(any(), any(), any(), any()) } returns Result.Success(mockk())

        viewModel.updateTitle("Test Title")
        viewModel.updateContent("Test Content")
        viewModel.publish()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertFalse(state.isLoading)
        assertNull(state.error)
        coVerify(exactly = 1) { repository.createNote("Test Title", "Test Content", emptyList(), null) }
    }

    /**
     * 发布失败：创建笔记返回错误，应显示错误信息
     */
    @Test
    fun publish_createNoteFails_errorShown() = runTest {
        coEvery {
            repository.createNote(any(), any(), any(), any())
        } returns Result.Error(ErrorCode.SERVER_ERROR, "服务器错误")

        viewModel.updateTitle("Test Title")
        viewModel.updateContent("Test Content")
        viewModel.publish()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSuccess)
        assertFalse(state.isLoading)
        assertEquals("服务器错误", state.error)
    }

    /**
     * 发布过程中有图片时，应先上传图片再创建笔记
     */
    @Test
    fun publish_withImages_shouldUploadImagesThenCreateNote() = runTest {
        coEvery { repository.uploadImage(uri1) } returns Result.Success("https://cdn.example.com/img1.jpg")
        coEvery { repository.createNote(any(), any(), any(), any()) } returns Result.Success(mockk())

        viewModel.updateTitle("Title")
        viewModel.updateContent("Content")
        viewModel.addImages(listOf(uri1))
        viewModel.publish()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.uploadImage(uri1) }
        coVerify(exactly = 1) {
            repository.createNote("Title", "Content", listOf("https://cdn.example.com/img1.jpg"), null)
        }
    }

    /**
     * 发布过程中图片上传失败，应停止并显示错误
     */
    @Test
    fun publish_imageUploadFails_shouldShowErrorAndStop() = runTest {
        coEvery {
            repository.uploadImage(uri1)
        } returns Result.Error(ErrorCode.FILE_UPLOAD_FAILED, "timeout")

        viewModel.updateTitle("Title")
        viewModel.updateContent("Content")
        viewModel.addImages(listOf(uri1))
        viewModel.publish()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("图片上传失败: timeout", state.error)
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        // 图片上传失败后不应调用 createNote
        coVerify(exactly = 0) { repository.createNote(any(), any(), any(), any()) }
    }

    /**
     * 发布过程中视频上传失败，应停止并显示错误
     */
    @Test
    fun publish_videoUploadFails_shouldShowErrorAndStop() = runTest {
        coEvery {
            repository.uploadVideo(videoUri)
        } returns Result.Error(ErrorCode.FILE_TOO_LARGE, "文件过大")

        viewModel.updateTitle("Title")
        viewModel.updateContent("Content")
        viewModel.setVideo(videoUri)
        viewModel.publish()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("视频上传失败: 文件过大", state.error)
        assertFalse(state.isLoading)
        coVerify(exactly = 0) { repository.createNote(any(), any(), any(), any()) }
    }

    /**
     * 发布成功并带有视频时，应上传视频并传入videoUrl
     */
    @Test
    fun publish_withVideo_shouldUploadVideoAndCreateNote() = runTest {
        coEvery { repository.uploadVideo(videoUri) } returns Result.Success("https://cdn.example.com/video.mp4")
        coEvery { repository.createNote(any(), any(), any(), any()) } returns Result.Success(mockk())

        viewModel.updateTitle("Title")
        viewModel.updateContent("Content")
        viewModel.setVideo(videoUri)
        viewModel.publish()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.uploadVideo(videoUri) }
        coVerify(exactly = 1) {
            repository.createNote("Title", "Content", emptyList(), "https://cdn.example.com/video.mp4")
        }
    }

    // ======== Loading 状态 ========

    /**
     * 发布后在加载过程中isLoading应为true（需advanceUntilIdle前检查）
     * 使用 UnconfinedTestDispatcher 使 launch 立即执行到第一个挂起点
     */
    @Test
    fun publish_whileLoading_isLoadingTrue() {
        val eagerDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
        Dispatchers.setMain(eagerDispatcher)
        try {
            val deferred = kotlinx.coroutines.CompletableDeferred<Result<com.example.noteshare.feature.feed.domain.model.NoteResponse>>()
            coEvery { repository.createNote(any(), any(), any(), any()) } coAnswers {
                deferred.await()
            }

            val vm = PublishViewModel(repository)
            vm.updateTitle("Title")
            vm.updateContent("Content")
            vm.publish()

            // UnconfinedTestDispatcher 使 launch 立即执行到挂起点 (deferred.await)
            assertTrue(vm.uiState.value.isLoading)

            // 释放协程完成
            deferred.complete(Result.Success(mockk()))
            assertFalse(vm.uiState.value.isLoading)
            assertTrue(vm.uiState.value.isSuccess)
        } finally {
            Dispatchers.setMain(testDispatcher)
        }
    }

    /**
     * 发布请求未完成时重复点击，不应创建重复笔记
     */
    @Test
    fun publish_whileLoading_doesNotSubmitAgain() = runTest {
        coEvery { repository.createNote(any(), any(), any(), any()) } coAnswers {
            kotlinx.coroutines.delay(5000)
            Result.Success(mockk())
        }

        viewModel.updateTitle("Title")
        viewModel.updateContent("Content")
        viewModel.publish()
        viewModel.publish()
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) { repository.createNote("Title", "Content", emptyList(), null) }
    }

    /**
     * errorShown() 调用后，error 应被清除
     */
    @Test
    fun errorShown_errorCleared() = runTest {
        viewModel.updateTitle("")
        viewModel.updateContent("Content")
        viewModel.publish()
        advanceUntilIdle()

        assertEquals("标题长度必须在 1-100 之间", viewModel.uiState.value.error)

        viewModel.errorShown()
        assertNull(viewModel.uiState.value.error)
    }

    /**
     * 辅助方法：断言非空
     */
    private fun assertNotNull(value: Any?) {
        assertTrue("Expected non-null value", value != null)
    }
}
