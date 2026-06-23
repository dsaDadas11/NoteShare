package com.example.noteshare.feature.publish.data

import android.content.Context
import android.net.Uri
import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.core.network.UploadApi
import com.example.noteshare.feature.feed.data.NoteApi
import com.example.noteshare.feature.feed.domain.model.CreateNoteRequest
import com.example.noteshare.feature.feed.domain.model.NoteResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import javax.inject.Inject

class PublishRepository @Inject constructor(
    private val uploadApi: UploadApi,
    private val noteApi: NoteApi,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val MAX_UPLOAD_BYTES = 5L * 1024L * 1024L
        private const val MAX_VIDEO_UPLOAD_BYTES = 50L * 1024L * 1024L
    }

    suspend fun uploadImage(uri: Uri): Result<String> {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = withContext(Dispatchers.IO) { contentResolver.getType(uri) ?: "image/jpeg" }
            val statSize = withContext(Dispatchers.IO) {
                contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
            }
            if (statSize > MAX_UPLOAD_BYTES) {
                return Result.Error(ErrorCode.FILE_TOO_LARGE, "单张图片不能超过 5MB")
            }
            val bytes = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return Result.Error(ErrorCode.INVALID_PARAMETER, "无法读取图片内容")
            if (bytes.size > MAX_UPLOAD_BYTES) {
                return Result.Error(ErrorCode.FILE_TOO_LARGE, "单张图片不能超过 5MB")
            }
            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val extension = mimeType.substringAfter("/", "jpg")
            val filename = "upload_${UUID.randomUUID()}.$extension"
            val part = MultipartBody.Part.createFormData("file", filename, requestBody)

            val response = uploadApi.uploadImage(part)
            if (response.code == ErrorCode.SUCCESS && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "图片上传失败: ${e.message}")
        }
    }

    suspend fun uploadVideo(uri: Uri): Result<String> {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = withContext(Dispatchers.IO) { contentResolver.getType(uri) ?: "video/mp4" }
            val statSize = withContext(Dispatchers.IO) {
                contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
            }
            if (statSize > MAX_VIDEO_UPLOAD_BYTES) {
                return Result.Error(ErrorCode.FILE_TOO_LARGE, "单个视频不能超过 50MB")
            }
            val bytes = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return Result.Error(ErrorCode.INVALID_PARAMETER, "无法读取视频内容")
            if (bytes.size > MAX_VIDEO_UPLOAD_BYTES) {
                return Result.Error(ErrorCode.FILE_TOO_LARGE, "单个视频不能超过 50MB")
            }
            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val extension = when {
                mimeType.contains("webm") -> "webm"
                mimeType.contains("3gpp") -> "3gp"
                else -> "mp4"
            }
            val filename = "video_${UUID.randomUUID()}.$extension"
            val part = MultipartBody.Part.createFormData("file", filename, requestBody)

            val response = uploadApi.uploadVideo(part)
            if (response.code == ErrorCode.SUCCESS && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "视频上传失败: ${e.message}")
        }
    }

    suspend fun createNote(title: String, content: String, imageUrls: List<String>, videoUrl: String? = null): Result<NoteResponse> {
        return try {
            val response = noteApi.createNote(CreateNoteRequest(title, content, imageUrls, videoUrl))
            if (response.code == ErrorCode.SUCCESS && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "发布失败: ${e.message}")
        }
    }
}
