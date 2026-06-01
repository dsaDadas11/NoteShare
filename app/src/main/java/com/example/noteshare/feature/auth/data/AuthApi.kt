package com.example.noteshare.feature.auth.data

import com.example.noteshare.core.network.ApiResponse
import com.example.noteshare.feature.auth.domain.model.LoginRequest
import com.example.noteshare.feature.auth.domain.model.LoginResponse
import com.example.noteshare.feature.auth.domain.model.RegisterRequest
import com.example.noteshare.feature.auth.domain.model.UserResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<UserResponse>

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>
}
