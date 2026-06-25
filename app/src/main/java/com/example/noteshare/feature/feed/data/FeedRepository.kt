package com.example.noteshare.feature.feed.data

import com.example.noteshare.core.common.Result
import com.example.noteshare.core.common.safeApiCall
import com.example.noteshare.core.network.PageData
import com.example.noteshare.feature.feed.domain.model.NoteResponse
import javax.inject.Inject

class FeedRepository @Inject constructor(
    private val noteApi: NoteApi
) {
    suspend fun getNotes(page: Int, size: Int = 20): Result<PageData<NoteResponse>> =
        safeApiCall("网络请求失败") { noteApi.getNotes(page, size) }

    suspend fun searchNotes(keyword: String, page: Int, size: Int = 20): Result<PageData<NoteResponse>> =
        safeApiCall("网络请求失败") { noteApi.searchNotes(keyword, page, size) }
}
