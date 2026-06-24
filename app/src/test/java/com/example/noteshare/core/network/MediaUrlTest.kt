package com.example.noteshare.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * MediaUrl (resolveMediaUrl) 单元测试
 * 覆盖：绝对URL不变、相对路径转换、空URL、已包含baseUrl的URL、各协议前缀处理
 */
class MediaUrlTest {

    private val testBaseUrl = "http://127.0.0.1:8200"

    /**
     * 绝对URL以 http:// 开头，应原样返回
     */
    @Test
    fun resolveMediaUrl_httpUrl_unchanged() {
        val url = "http://cdn.example.com/image.jpg"
        val result = resolveMediaUrl(url, testBaseUrl)
        assertEquals(url, result)
    }

    /**
     * 绝对URL以 https:// 开头，应原样返回
     */
    @Test
    fun resolveMediaUrl_httpsUrl_unchanged() {
        val url = "https://cdn.example.com/image.jpg"
        val result = resolveMediaUrl(url, testBaseUrl)
        assertEquals(url, result)
    }

    /**
     * 绝对URL以 content:// 开头，应原样返回
     */
    @Test
    fun resolveMediaUrl_contentUrl_unchanged() {
        val url = "content://media/external/images/123"
        val result = resolveMediaUrl(url, testBaseUrl)
        assertEquals(url, result)
    }

    /**
     * 绝对URL以 file:// 开头，应原样返回
     */
    @Test
    fun resolveMediaUrl_fileUrl_unchanged() {
        val url = "file:///sdcard/photo.jpg"
        val result = resolveMediaUrl(url, testBaseUrl)
        assertEquals(url, result)
    }

    /**
     * 绝对URL以 android.resource:// 开头，应原样返回
     */
    @Test
    fun resolveMediaUrl_androidResourceUrl_unchanged() {
        val url = "android.resource://com.example.noteshare/raw/video"
        val result = resolveMediaUrl(url, testBaseUrl)
        assertEquals(url, result)
    }

    /**
     * 相对路径以 / 开头，应拼接baseUrl
     */
    @Test
    fun resolveMediaUrl_relativePathWithSlash_prependsBaseUrl() {
        val path = "/uploads/image.png"
        val result = resolveMediaUrl(path, testBaseUrl)
        assertEquals("$testBaseUrl/uploads/image.png", result)
    }

    /**
     * 相对路径不以 / 开头，应拼接baseUrl并加 /
     */
    @Test
    fun resolveMediaUrl_relativePathWithoutSlash_prependsBaseUrlWithSlash() {
        val path = "uploads/image.png"
        val result = resolveMediaUrl(path, testBaseUrl)
        assertEquals("$testBaseUrl/uploads/image.png", result)
    }

    /**
     * null 输入应返回 null
     */
    @Test
    fun resolveMediaUrl_null_returnsNull() {
        val result = resolveMediaUrl(null, testBaseUrl)
        assertNull(result)
    }

    /**
     * 空字符串输入应返回 null
     */
    @Test
    fun resolveMediaUrl_emptyString_returnsNull() {
        val result = resolveMediaUrl("", testBaseUrl)
        assertNull(result)
    }

    /**
     * 纯空白字符串输入应返回 null（trim后为空）
     */
    @Test
    fun resolveMediaUrl_blankString_returnsNull() {
        val result = resolveMediaUrl("   ", testBaseUrl)
        assertNull(result)
    }

    /**
     * 已包含baseUrl的完整URL应原样返回（因为以 http:// 开头）
     */
    @Test
    fun resolveMediaUrl_fullBaseUrl_unchanged() {
        val url = "http://127.0.0.1:8200/uploads/photo.jpg"
        val result = resolveMediaUrl(url, testBaseUrl)
        assertEquals(url, result)
    }

    /**
     * 带前导空格的相对路径，trim后应正确拼接baseUrl
     */
    @Test
    fun resolveMediaUrl_pathWithLeadingSpaces_trimmedAndResolved() {
        val path = "  /uploads/image.png  "
        val result = resolveMediaUrl(path, testBaseUrl)
        assertEquals("$testBaseUrl/uploads/image.png", result)
    }

    /**
     * HTTPS协议的URL（大写）应原样返回（大小写不敏感）
     */
    @Test
    fun resolveMediaUrl_httpsUrlUpperCase_unchanged() {
        val url = "HTTPS://CDN.EXAMPLE.COM/IMG.JPG"
        val result = resolveMediaUrl(url, testBaseUrl)
        assertEquals(url, result)
    }

    /**
     * HTTP协议的URL（大写）应原样返回（大小写不敏感）
     */
    @Test
    fun resolveMediaUrl_httpUrlUpperCase_unchanged() {
        val url = "HTTP://CDN.EXAMPLE.COM/IMG.JPG"
        val result = resolveMediaUrl(url, testBaseUrl)
        assertEquals(url, result)
    }

    /**
     * 仅包含斜杠 "/" 的路径（非绝对路径但等于根），应拼接baseUrl
     */
    @Test
    fun resolveMediaUrl_onlySlash_prependsBaseUrl() {
        val path = "/"
        val result = resolveMediaUrl(path, testBaseUrl)
        assertEquals("$testBaseUrl/", result)
    }

    /**
     * 以 "http" 开头但不是有效协议前缀的路径（如 "httpfoo/bar"），
     * 既不是 http:// 也不是 https://，应视为相对路径
     */
    @Test
    fun resolveMediaUrl_httpWithoutProtocol_isRelativePath() {
        val path = "httpfoo/bar.jpg"
        val result = resolveMediaUrl(path, testBaseUrl)
        assertEquals("$testBaseUrl/httpfoo/bar.jpg", result)
    }
}
