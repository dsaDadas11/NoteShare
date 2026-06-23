package com.example.noteshare.feature.feed.data

import com.example.noteshare.core.common.ErrorCode
import com.example.noteshare.core.common.Result
import com.example.noteshare.core.network.PageData
import com.example.noteshare.feature.feed.domain.model.CommentResponse
import com.example.noteshare.feature.feed.domain.model.CreateCommentRequest
import com.example.noteshare.feature.feed.domain.model.NoteDetailResponse
import javax.inject.Inject

class NoteDetailRepository @Inject constructor(
    private val noteApi: NoteApi
) {
    suspend fun getNoteDetail(id: Long): Result<NoteDetailResponse> {
        return try {
            val response = noteApi.getNoteDetail(id)
            if (response.code == ErrorCode.SUCCESS && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }

    suspend fun likeNote(id: Long): Result<Unit> {
        return try {
            val response = noteApi.likeNote(id)
            if (response.code == ErrorCode.SUCCESS) {
                Result.Success(Unit)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }

    suspend fun unlikeNote(id: Long): Result<Unit> {
        return try {
            val response = noteApi.unlikeNote(id)
            if (response.code == ErrorCode.SUCCESS) {
                Result.Success(Unit)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }

    suspend fun deleteNote(id: Long): Result<Unit> {
        return try {
            val response = noteApi.deleteNote(id)
            if (response.code == ErrorCode.SUCCESS) {
                Result.Success(Unit)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }

    suspend fun getComments(id: Long, page: Int, size: Int = 20): Result<PageData<CommentResponse>> {
        return try {
            val response = noteApi.getComments(id, page, size)
            if (response.code == ErrorCode.SUCCESS && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }

    suspend fun createComment(id: Long, content: String, parentId: Long? = null, replyToAuthor: String? = null): Result<CommentResponse> {
        return try {
            val response = noteApi.createComment(id, CreateCommentRequest(content, parentId, replyToAuthor))
            if (response.code == ErrorCode.SUCCESS && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }

    suspend fun deleteComment(id: Long, commentId: Long): Result<Unit> {
        return try {
            val response = noteApi.deleteComment(id, commentId)
            if (response.code == ErrorCode.SUCCESS) {
                Result.Success(Unit)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }

    suspend fun getCommentReplies(noteId: Long, commentId: Long): Result<List<CommentResponse>> {
        return try {
            val response = noteApi.getCommentReplies(noteId, commentId)
            if (response.code == ErrorCode.SUCCESS && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }

    suspend fun likeComment(noteId: Long, commentId: Long): Result<Unit> {
        return try {
            val response = noteApi.likeComment(noteId, commentId)
            if (response.code == ErrorCode.SUCCESS) {
                Result.Success(Unit)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }

    suspend fun unlikeComment(noteId: Long, commentId: Long): Result<Unit> {
        return try {
            val response = noteApi.unlikeComment(noteId, commentId)
            if (response.code == ErrorCode.SUCCESS) {
                Result.Success(Unit)
            } else {
                Result.Error(response.code, response.message)
            }
        } catch (e: Exception) {
            Result.Error(ErrorCode.NETWORK_ERROR, "网络请求失败: ${e.message}")
        }
    }
}
