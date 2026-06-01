package com.example.noteshare.feature.profile.data

import com.example.noteshare.core.network.ApiResponse
import com.example.noteshare.core.network.PageData
import com.example.noteshare.feature.feed.domain.model.NoteResponse
import com.example.noteshare.feature.profile.domain.model.UpdateProfileRequest
import com.example.noteshare.feature.profile.domain.model.UserProfileResponse
import retrofit2.http.*

interface UserApi {
    @GET("/api/users/me")
    suspend fun getMyProfile(): ApiResponse<UserProfileResponse>
    
    @GET("/api/users/{id}")
    suspend fun getUserProfile(@Path("id") id: Long): ApiResponse<UserProfileResponse>
    
    @PUT("/api/users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ApiResponse<UserProfileResponse>
    
    @POST("/api/users/{id}/follow")
    suspend fun followUser(@Path("id") id: Long): ApiResponse<Unit>
    
    @DELETE("/api/users/{id}/follow")
    suspend fun unfollowUser(@Path("id") id: Long): ApiResponse<Unit>

    @GET("/api/users/{id}/notes")
    suspend fun getUserNotes(
        @Path("id") id: Long, 
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResponse<PageData<NoteResponse>>
}
