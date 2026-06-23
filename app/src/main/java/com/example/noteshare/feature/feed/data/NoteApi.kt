package com.example.noteshare.feature.feed.data

import com.example.noteshare.core.network.ApiResponse
import com.example.noteshare.core.network.PageData
import com.example.noteshare.feature.feed.domain.model.NoteResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NoteApi {
    @GET("/api/notes")
    suspend fun getNotes(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResponse<PageData<NoteResponse>>

    @GET("/api/notes/search")
    suspend fun searchNotes(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResponse<PageData<NoteResponse>>

    @GET("/api/notes/{id}")
    suspend fun getNoteDetail(@retrofit2.http.Path("id") id: Long): ApiResponse<com.example.noteshare.feature.feed.domain.model.NoteDetailResponse>

    @retrofit2.http.DELETE("/api/notes/{id}")
    suspend fun deleteNote(@retrofit2.http.Path("id") id: Long): ApiResponse<Unit>

    @retrofit2.http.POST("/api/notes/{id}/like")
    suspend fun likeNote(@retrofit2.http.Path("id") id: Long): ApiResponse<Unit>

    @retrofit2.http.DELETE("/api/notes/{id}/like")
    suspend fun unlikeNote(@retrofit2.http.Path("id") id: Long): ApiResponse<Unit>

    @GET("/api/notes/{id}/comments")
    suspend fun getComments(
        @retrofit2.http.Path("id") id: Long, 
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResponse<PageData<com.example.noteshare.feature.feed.domain.model.CommentResponse>>

    @retrofit2.http.POST("/api/notes/{id}/comments")
    suspend fun createComment(
        @retrofit2.http.Path("id") id: Long, 
        @retrofit2.http.Body request: com.example.noteshare.feature.feed.domain.model.CreateCommentRequest
    ): ApiResponse<com.example.noteshare.feature.feed.domain.model.CommentResponse>

    @retrofit2.http.DELETE("/api/notes/{id}/comments/{commentId}")
    suspend fun deleteComment(
        @retrofit2.http.Path("id") id: Long,
        @retrofit2.http.Path("commentId") commentId: Long
    ): ApiResponse<Unit>

    @GET("/api/notes/{id}/comments/{commentId}/replies")
    suspend fun getCommentReplies(
        @retrofit2.http.Path("id") id: Long,
        @retrofit2.http.Path("commentId") commentId: Long
    ): ApiResponse<List<com.example.noteshare.feature.feed.domain.model.CommentResponse>>

    @retrofit2.http.POST("/api/notes/{id}/comments/{commentId}/like")
    suspend fun likeComment(
        @retrofit2.http.Path("id") id: Long,
        @retrofit2.http.Path("commentId") commentId: Long
    ): ApiResponse<Unit>

    @retrofit2.http.DELETE("/api/notes/{id}/comments/{commentId}/like")
    suspend fun unlikeComment(
        @retrofit2.http.Path("id") id: Long,
        @retrofit2.http.Path("commentId") commentId: Long
    ): ApiResponse<Unit>

    @retrofit2.http.POST("/api/notes")
    suspend fun createNote(
        @retrofit2.http.Body request: com.example.noteshare.feature.feed.domain.model.CreateNoteRequest
    ): ApiResponse<com.example.noteshare.feature.feed.domain.model.NoteResponse>
}
