package com.example.noteshare.feature.feed.data

import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.core.network.PageData
import com.example.noteshare.feature.feed.domain.model.NoteResponse
import javax.inject.Inject

class FeedRepository @Inject constructor(
    private val noteApi: NoteApi
) {
    suspend fun getNotes(page: Int, size: Int = 20): Result<PageData<NoteResponse>> {
        return try {
            val response = noteApi.getNotes(page, size)
            if (response.code == ErrorCode.SUCCESS && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }

    suspend fun searchNotes(keyword: String, page: Int, size: Int = 20): Result<PageData<NoteResponse>> {
        return try {
            val response = noteApi.searchNotes(keyword, page, size)
            if (response.code == ErrorCode.SUCCESS && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }
}
