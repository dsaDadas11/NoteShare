package com.example.noteshare.core.network

import android.content.Context
import android.net.Uri
import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

private const val MAX_IMAGE_BYTES = 5L * 1024L * 1024L
private const val MAX_VIDEO_BYTES = 50L * 1024L * 1024L

/**
 * 通用图片上传，供 ProfileRepository / PublishRepository 等复用。
 * @param prefix 文件名前缀，如 "avatar"、"upload"
 */
suspend fun uploadFile(
    context: Context,
    uploadApi: UploadApi,
    uri: Uri,
    prefix: String
): Result<String> {
    return try {
        val contentResolver = context.contentResolver
        val mimeType = withContext(Dispatchers.IO) { contentResolver.getType(uri) ?: "image/jpeg" }
        val statSize = withContext(Dispatchers.IO) {
            contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
        }
        if (statSize > MAX_IMAGE_BYTES) {
            return Result.Error(ErrorCode.FILE_TOO_LARGE, "单张图片不能超过 5MB")
        }
        val bytes = withContext(Dispatchers.IO) {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } ?: return Result.Error(ErrorCode.INVALID_PARAMETER, "无法读取图片内容")
        if (bytes.size > MAX_IMAGE_BYTES) {
            return Result.Error(ErrorCode.FILE_TOO_LARGE, "单张图片不能超过 5MB")
        }
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val extension = mimeType.substringAfter("/", "jpg")
        val filename = "${prefix}_${UUID.randomUUID()}.$extension"
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

/**
 * 通用视频上传。
 */
suspend fun uploadVideo(
    context: Context,
    uploadApi: UploadApi,
    uri: Uri,
    prefix: String
): Result<String> {
    return try {
        val contentResolver = context.contentResolver
        val mimeType = withContext(Dispatchers.IO) { contentResolver.getType(uri) ?: "video/mp4" }
        val statSize = withContext(Dispatchers.IO) {
            contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
        }
        if (statSize > MAX_VIDEO_BYTES) {
            return Result.Error(ErrorCode.FILE_TOO_LARGE, "单个视频不能超过 50MB")
        }
        val bytes = withContext(Dispatchers.IO) {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } ?: return Result.Error(ErrorCode.INVALID_PARAMETER, "无法读取视频内容")
        if (bytes.size > MAX_VIDEO_BYTES) {
            return Result.Error(ErrorCode.FILE_TOO_LARGE, "单个视频不能超过 50MB")
        }
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val extension = when {
            mimeType.contains("webm") -> "webm"
            mimeType.contains("3gpp") -> "3gp"
            else -> "mp4"
        }
        val filename = "${prefix}_${UUID.randomUUID()}.$extension"
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
