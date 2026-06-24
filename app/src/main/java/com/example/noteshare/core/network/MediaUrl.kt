package com.example.noteshare.core.network

import com.example.noteshare.BuildConfig

/**
 * 将相对媒体路径解析为完整URL
 * @param url 原始URL（可能是相对路径或绝对URL）
 * @param baseUrl 可选的基础URL，默认使用BuildConfig.BASE_URL
 * @return 完整的URL，如果输入为空则返回null
 */
fun resolveMediaUrl(url: String?, baseUrl: String = BuildConfig.BASE_URL): String? {
    val value = url?.trim()
    if (value.isNullOrEmpty()) {
        return null
    }

    if (
        value.startsWith("http://", ignoreCase = true) ||
        value.startsWith("https://", ignoreCase = true) ||
        value.startsWith("content://", ignoreCase = true) ||
        value.startsWith("file://", ignoreCase = true) ||
        value.startsWith("android.resource://", ignoreCase = true)
    ) {
        return value
    }

    val trimmedBaseUrl = baseUrl.trimEnd('/')
    return if (value.startsWith("/")) {
        trimmedBaseUrl + value
    } else {
        "$trimmedBaseUrl/$value"
    }
}
