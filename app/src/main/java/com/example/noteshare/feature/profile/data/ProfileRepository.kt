package com.example.noteshare.feature.profile.data

import android.content.Context
import android.net.Uri
import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.core.network.PageData
import com.example.noteshare.core.network.UploadApi
import com.example.noteshare.feature.feed.domain.model.NoteResponse
import com.example.noteshare.feature.profile.domain.model.UpdateProfileRequest
import com.example.noteshare.feature.profile.domain.model.UserProfileResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val userApi: UserApi,
    private val uploadApi: UploadApi,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val MAX_UPLOAD_BYTES = 5L * 1024L * 1024L
    }

    suspend fun getMyProfile(): Result<UserProfileResponse> {
        return try {
            val response = userApi.getMyProfile()
            if (response.code == ErrorCode.SUCCESS && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }

    suspend fun getUserProfile(id: Long): Result<UserProfileResponse> {
        return try {
            val response = userApi.getUserProfile(id)
            if (response.code == ErrorCode.SUCCESS && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }

    suspend fun updateProfile(request: UpdateProfileRequest): Result<UserProfileResponse> {
        return try {
            val response = userApi.updateProfile(request)
            if (response.code == ErrorCode.SUCCESS && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }

    suspend fun followUser(id: Long): Result<Unit> {
        return try {
            val response = userApi.followUser(id)
            if (response.code == ErrorCode.SUCCESS) {
                Result.Success(Unit)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }

    suspend fun unfollowUser(id: Long): Result<Unit> {
        return try {
            val response = userApi.unfollowUser(id)
            if (response.code == ErrorCode.SUCCESS) {
                Result.Success(Unit)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }

    suspend fun getUserNotes(id: Long, page: Int): Result<PageData<NoteResponse>> {
        return try {
            val response = userApi.getUserNotes(id, page)
            if (response.code == ErrorCode.SUCCESS && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }

    suspend fun uploadAvatar(uri: Uri): Result<String> {
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
            val filename = "avatar_${UUID.randomUUID()}.$extension"
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
}
