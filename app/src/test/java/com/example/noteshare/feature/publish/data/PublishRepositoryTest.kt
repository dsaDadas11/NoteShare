package com.example.noteshare.feature.publish.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.core.network.UploadApi
import com.example.noteshare.core.network.ApiResponse
import com.example.noteshare.feature.feed.data.NoteApi
import com.example.noteshare.feature.feed.domain.model.CreateNoteRequest
import com.example.noteshare.feature.feed.domain.model.NoteResponse
import com.example.noteshare.feature.feed.domain.model.UserBrief
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MultipartBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import android.os.ParcelFileDescriptor

/**
 * PublishRepository 单元测试
 * 覆盖：图片上传成功/失败、视频上传成功/失败、笔记创建成功/失败、文件大小验证
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PublishRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var uploadApi: UploadApi
    private lateinit var noteApi: NoteApi
    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var repository: PublishRepository

    private lateinit var testUri: Uri

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        uploadApi = mockk()
        noteApi = mockk()
        context = mockk()
        contentResolver = mockk()
        testUri = mockk()

        every { context.contentResolver } returns contentResolver
        repository = PublishRepository(uploadApi, noteApi, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ======== 上传图片 ========

    /**
     * 上传图片成功：ContentResolver读取图片，API返回成功
     */
    @Test
    fun uploadImage_apiSuccess_returnsUrl() = runTest {
        // arrange
        val imageBytes = "fake image content".toByteArray()
        val inputStream = ByteArrayInputStream(imageBytes)

        every { contentResolver.getType(testUri) } returns "image/jpeg"

        val mockPfd = mockk<ParcelFileDescriptor>()
        every { mockPfd.statSize } returns imageBytes.size.toLong()
        every { mockPfd.close() } returns Unit
        every { contentResolver.openFileDescriptor(testUri, "r") } returns mockPfd

        every { contentResolver.openInputStream(testUri) } returns inputStream

        coEvery { uploadApi.uploadImage(any()) } returns ApiResponse(
            code = ErrorCode.SUCCESS,
            message = "ok",
            data = "https://cdn.example.com/img1.jpg"
        )

        // act
        val result = repository.uploadImage(testUri)

        // assert
        assertTrue(result is Result.Success)
        assertEquals("https://cdn.example.com/img1.jpg", (result as Result.Success).data)
    }

    /**
     * 上传图片失败：API返回错误码
     */
    @Test
    fun uploadImage_apiError_returnsError() = runTest {
        val imageBytes = "fake image content".toByteArray()
        val inputStream = ByteArrayInputStream(imageBytes)

        every { contentResolver.getType(testUri) } returns "image/jpeg"

        val mockPfd = mockk<ParcelFileDescriptor>()
        every { mockPfd.statSize } returns imageBytes.size.toLong()
        every { mockPfd.close() } returns Unit
        every { contentResolver.openFileDescriptor(testUri, "r") } returns mockPfd

        every { contentResolver.openInputStream(testUri) } returns inputStream

        coEvery { uploadApi.uploadImage(any()) } returns ApiResponse(
            code = ErrorCode.FILE_TYPE_INVALID,
            message = "文件格式不支持",
            data = null
        )

        val result = repository.uploadImage(testUri)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_TYPE_INVALID, (result as Result.Error).code)
    }

    /**
     * 上传图片失败：ContentResolver读取图片返回null（无法读取内容）
     */
    @Test
    fun uploadImage_contentResolverNull_returnsError() = runTest {
        every { contentResolver.getType(testUri) } returns "image/jpeg"

        val mockPfd = mockk<ParcelFileDescriptor>()
        every { mockPfd.statSize } returns 100L
        every { mockPfd.close() } returns Unit
        every { contentResolver.openFileDescriptor(testUri, "r") } returns mockPfd

        every { contentResolver.openInputStream(testUri) } returns null

        val result = repository.uploadImage(testUri)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.INVALID_PARAMETER, (result as Result.Error).code)
    }

    /**
     * 上传图片失败：文件大小超过statSize限制（>5MB）
     */
    @Test
    fun uploadImage_statSizeTooLarge_returnsFileTooLargeError() = runTest {
        val tooLarge = 6L * 1024L * 1024L // 6MB
        every { contentResolver.getType(testUri) } returns "image/jpeg"

        val mockPfd = mockk<ParcelFileDescriptor>()
        every { mockPfd.statSize } returns tooLarge
        every { mockPfd.close() } returns Unit
        every { contentResolver.openFileDescriptor(testUri, "r") } returns mockPfd

        val result = repository.uploadImage(testUri)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_TOO_LARGE, (result as Result.Error).code)
        assertEquals("单张图片不能超过 5MB", (result as Result.Error).message)
    }

    /**
     * 上传图片失败：字节数超过5MB限制
     */
    @Test
    fun uploadImage_bytesSizeTooLarge_returnsFileTooLargeError() = runTest {
        val tooLargeBytes = ByteArray(6 * 1024 * 1024) // 6MB
        val inputStream = ByteArrayInputStream(tooLargeBytes)

        every { contentResolver.getType(testUri) } returns "image/jpeg"

        val mockPfd = mockk<ParcelFileDescriptor>()
        every { mockPfd.statSize } returns -1L // statSize未知，走bytes检查
        every { mockPfd.close() } returns Unit
        every { contentResolver.openFileDescriptor(testUri, "r") } returns mockPfd

        every { contentResolver.openInputStream(testUri) } returns inputStream

        val result = repository.uploadImage(testUri)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_TOO_LARGE, (result as Result.Error).code)
    }

    /**
     * 上传图片时API抛异常，返回网络错误
     */
    @Test
    fun uploadImage_apiThrowsException_returnsNetworkError() = runTest {
        val imageBytes = "fake image content".toByteArray()
        val inputStream = ByteArrayInputStream(imageBytes)

        every { contentResolver.getType(testUri) } returns "image/jpeg"

        val mockPfd = mockk<ParcelFileDescriptor>()
        every { mockPfd.statSize } returns imageBytes.size.toLong()
        every { mockPfd.close() } returns Unit
        every { contentResolver.openFileDescriptor(testUri, "r") } returns mockPfd

        every { contentResolver.openInputStream(testUri) } returns inputStream

        coEvery { uploadApi.uploadImage(any()) } throws RuntimeException("Network timeout")

        val result = repository.uploadImage(testUri)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
        assertTrue((result as Result.Error).message.contains("网络超时") || result.message.contains("Network timeout"))
    }

    // ======== 上传视频 ========

    /**
     * 上传视频成功：API返回成功
     */
    @Test
    fun uploadVideo_apiSuccess_returnsUrl() = runTest {
        val videoBytes = "fake video content".toByteArray()
        val inputStream = ByteArrayInputStream(videoBytes)

        every { contentResolver.getType(testUri) } returns "video/mp4"

        val mockPfd = mockk<ParcelFileDescriptor>()
        every { mockPfd.statSize } returns videoBytes.size.toLong()
        every { mockPfd.close() } returns Unit
        every { contentResolver.openFileDescriptor(testUri, "r") } returns mockPfd

        every { contentResolver.openInputStream(testUri) } returns inputStream

        coEvery { uploadApi.uploadVideo(any()) } returns ApiResponse(
            code = ErrorCode.SUCCESS,
            message = "ok",
            data = "https://cdn.example.com/video.mp4"
        )

        val result = repository.uploadVideo(testUri)

        assertTrue(result is Result.Success)
        assertEquals("https://cdn.example.com/video.mp4", (result as Result.Success).data)
    }

    /**
     * 上传视频失败：API返回错误码
     */
    @Test
    fun uploadVideo_apiError_returnsError() = runTest {
        val videoBytes = "fake video content".toByteArray()
        val inputStream = ByteArrayInputStream(videoBytes)

        every { contentResolver.getType(testUri) } returns "video/mp4"

        val mockPfd = mockk<ParcelFileDescriptor>()
        every { mockPfd.statSize } returns videoBytes.size.toLong()
        every { mockPfd.close() } returns Unit
        every { contentResolver.openFileDescriptor(testUri, "r") } returns mockPfd

        every { contentResolver.openInputStream(testUri) } returns inputStream

        coEvery { uploadApi.uploadVideo(any()) } returns ApiResponse(
            code = ErrorCode.FILE_TYPE_INVALID,
            message = "视频格式不支持",
            data = null
        )

        val result = repository.uploadVideo(testUri)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_TYPE_INVALID, (result as Result.Error).code)
    }

    /**
     * 上传视频失败：文件大小超过statSize限制（>50MB）
     */
    @Test
    fun uploadVideo_statSizeTooLarge_returnsFileTooLargeError() = runTest {
        val tooLarge = 51L * 1024L * 1024L // 51MB
        every { contentResolver.getType(testUri) } returns "video/mp4"

        val mockPfd = mockk<ParcelFileDescriptor>()
        every { mockPfd.statSize } returns tooLarge
        every { mockPfd.close() } returns Unit
        every { contentResolver.openFileDescriptor(testUri, "r") } returns mockPfd

        val result = repository.uploadVideo(testUri)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.FILE_TOO_LARGE, (result as Result.Error).code)
        assertEquals("单个视频不能超过 50MB", (result as Result.Error).message)
    }

    /**
     * 上传视频失败：ContentResolver返回null（无法读取内容）
     */
    @Test
    fun uploadVideo_contentResolverNull_returnsError() = runTest {
        every { contentResolver.getType(testUri) } returns "video/mp4"

        val mockPfd = mockk<ParcelFileDescriptor>()
        every { mockPfd.statSize } returns 100L
        every { mockPfd.close() } returns Unit
        every { contentResolver.openFileDescriptor(testUri, "r") } returns mockPfd

        every { contentResolver.openInputStream(testUri) } returns null

        val result = repository.uploadVideo(testUri)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.INVALID_PARAMETER, (result as Result.Error).code)
    }

    // ======== 创建笔记 ========

    /**
     * 创建笔记成功：API返回成功
     */
    @Test
    fun createNote_apiSuccess_returnsNoteResponse() = runTest {
        val noteResponse = NoteResponse(
            id = 1L,
            title = "Test",
            content = "Content",
            createdAt = "2025-01-01T00:00:00",
            author = UserBrief(id = 1L, username = "user1")
        )
        coEvery {
            noteApi.createNote(any())
        } returns ApiResponse(code = ErrorCode.SUCCESS, message = "ok", data = noteResponse)

        val result = repository.createNote("Test", "Content", listOf("url1"), null)

        assertTrue(result is Result.Success)
        assertEquals(1L, (result as Result.Success).data.id)
    }

    /**
     * 创建笔记失败：API返回错误码
     */
    @Test
    fun createNote_apiError_returnsError() = runTest {
        coEvery {
            noteApi.createNote(any())
        } returns ApiResponse(code = ErrorCode.PARAM_INVALID, message = "参数无效", data = null)

        val result = repository.createNote("Test", "Content", emptyList(), null)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.PARAM_INVALID, (result as Result.Error).code)
    }

    /**
     * 创建笔记失败：API抛异常，返回网络错误
     */
    @Test
    fun createNote_apiThrowsException_returnsNetworkError() = runTest {
        coEvery {
            noteApi.createNote(any())
        } throws RuntimeException("Connection refused")

        val result = repository.createNote("Test", "Content", emptyList(), null)

        assertTrue(result is Result.Error)
        assertEquals(ErrorCode.NETWORK_ERROR, (result as Result.Error).code)
    }
}
