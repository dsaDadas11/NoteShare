package com.example.noteshare.feature.profile.data

import android.content.Context
import android.net.Uri
import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.core.common.safeApiCall
import com.example.noteshare.core.common.safeApiCallUnit
import com.example.noteshare.core.network.PageData
import com.example.noteshare.core.network.UploadApi
import com.example.noteshare.core.network.uploadFile
import com.example.noteshare.feature.feed.domain.model.NoteResponse
import com.example.noteshare.feature.profile.domain.model.UpdateProfileRequest
import com.example.noteshare.feature.profile.domain.model.UserProfileResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val userApi: UserApi,
    private val uploadApi: UploadApi,
    @ApplicationContext private val context: Context
) {
    suspend fun getMyProfile(): Result<UserProfileResponse> = safeApiCall("获取个人信息失败") {
        userApi.getMyProfile()
    }

    suspend fun getUserProfile(id: Long): Result<UserProfileResponse> = safeApiCall("获取用户信息失败") {
        userApi.getUserProfile(id)
    }

    suspend fun updateProfile(request: UpdateProfileRequest): Result<UserProfileResponse> = safeApiCall("更新资料失败") {
        userApi.updateProfile(request)
    }

    suspend fun followUser(id: Long): Result<Unit> = safeApiCallUnit("关注失败") {
        userApi.followUser(id)
    }

    suspend fun unfollowUser(id: Long): Result<Unit> = safeApiCallUnit("取消关注失败") {
        userApi.unfollowUser(id)
    }

    suspend fun getUserNotes(id: Long, page: Int): Result<PageData<NoteResponse>> = safeApiCall("获取笔记失败") {
        userApi.getUserNotes(id, page)
    }

    suspend fun uploadAvatar(uri: Uri): Result<String> = uploadFile(context, uploadApi, uri, "avatar")
}
