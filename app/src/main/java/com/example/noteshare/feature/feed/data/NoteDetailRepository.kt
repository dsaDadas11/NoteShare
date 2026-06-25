package com.example.noteshare.feature.feed.data

import com.example.noteshare.core.common.Result
import com.example.noteshare.core.common.safeApiCall
import com.example.noteshare.core.common.safeApiCallUnit
import com.example.noteshare.core.network.PageData
import com.example.noteshare.feature.feed.domain.model.CommentResponse
import com.example.noteshare.feature.feed.domain.model.CreateCommentRequest
import com.example.noteshare.feature.feed.domain.model.NoteDetailResponse
import javax.inject.Inject

class NoteDetailRepository @Inject constructor(
    private val noteApi: NoteApi
) {
    suspend fun getNoteDetail(id: Long): Result<NoteDetailResponse> =
        safeApiCall("网络请求失败") { noteApi.getNoteDetail(id) }

    suspend fun likeNote(id: Long): Result<Unit> =
        safeApiCallUnit("网络请求失败") { noteApi.likeNote(id) }

    suspend fun unlikeNote(id: Long): Result<Unit> =
        safeApiCallUnit("网络请求失败") { noteApi.unlikeNote(id) }

    suspend fun deleteNote(id: Long): Result<Unit> =
        safeApiCallUnit("网络请求失败") { noteApi.deleteNote(id) }

    suspend fun getComments(id: Long, page: Int, size: Int = 20): Result<PageData<CommentResponse>> =
        safeApiCall("网络请求失败") { noteApi.getComments(id, page, size) }

    suspend fun createComment(
        id: Long,
        content: String,
        parentId: Long? = null,
        replyToAuthor: String? = null
    ): Result<CommentResponse> = safeApiCall("网络请求失败") {
        noteApi.createComment(id, CreateCommentRequest(content, parentId, replyToAuthor))
    }

    suspend fun deleteComment(id: Long, commentId: Long): Result<Unit> =
        safeApiCallUnit("网络请求失败") { noteApi.deleteComment(id, commentId) }

    suspend fun getCommentReplies(noteId: Long, commentId: Long): Result<List<CommentResponse>> =
        safeApiCall("网络请求失败") { noteApi.getCommentReplies(noteId, commentId) }

    suspend fun likeComment(noteId: Long, commentId: Long): Result<Unit> =
        safeApiCallUnit("网络请求失败") { noteApi.likeComment(noteId, commentId) }

    suspend fun unlikeComment(noteId: Long, commentId: Long): Result<Unit> =
        safeApiCallUnit("网络请求失败") { noteApi.unlikeComment(noteId, commentId) }
}
