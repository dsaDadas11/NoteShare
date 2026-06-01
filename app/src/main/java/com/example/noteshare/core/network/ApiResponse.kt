package com.example.noteshare.core.network

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T? = null,
    val requestId: String? = null
)

@Serializable
data class PageData<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val total: Long,
    val hasMore: Boolean
)
