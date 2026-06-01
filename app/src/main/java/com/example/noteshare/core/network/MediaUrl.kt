package com.example.noteshare.core.network

import com.example.noteshare.BuildConfig

fun resolveMediaUrl(url: String?): String? {
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

    val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
    return if (value.startsWith("/")) {
        baseUrl + value
    } else {
        "$baseUrl/$value"
    }
}
