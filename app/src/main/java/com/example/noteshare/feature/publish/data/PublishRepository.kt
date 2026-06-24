package com.example.noteshare.feature.publish.data

import android.content.Context
import android.net.Uri
import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.core.common.safeApiCall
import com.example.noteshare.core.network.UploadApi
import com.example.noteshare.core.network.uploadFile
import com.example.noteshare.core.network.uploadVideo
import com.example.noteshare.feature.feed.data.NoteApi
import com.example.noteshare.feature.feed.domain.model.CreateNoteRequest
import com.example.noteshare.feature.feed.domain.model.NoteResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PublishRepository @Inject constructor(
    private val uploadApi: UploadApi,
    private val noteApi: NoteApi,
    @ApplicationContext private val context: Context
) {
    suspend fun uploadImage(uri: Uri): Result<String> = uploadFile(context, uploadApi, uri, "upload")

    suspend fun uploadVideo(uri: Uri): Result<String> = uploadVideo(context, uploadApi, uri, "video")

    suspend fun createNote(title: String, content: String, imageUrls: List<String>, videoUrl: String? = null): Result<NoteResponse> = safeApiCall("发布失败") {
        noteApi.createNote(CreateNoteRequest(title, content, imageUrls, videoUrl))
    }
}
