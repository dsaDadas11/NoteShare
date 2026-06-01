package com.example.noteshare.core.common

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/**
 * 将 ISO 格式的 LocalDateTime 字符串转换为用户友好的显示格式
 * - 1分钟内：刚刚
 * - 1小时内：X分钟前
 * - 24小时内：X小时前
 * - 7天内：X天前
 * - 其他：yyyy-MM-dd HH:mm
 */
fun formatDateTime(isoString: String?): String {
    if (isoString.isNullOrBlank()) return ""
    return try {
        val dateTime = LocalDateTime.parse(isoString)
        val now = LocalDateTime.now()
        val minutes = ChronoUnit.MINUTES.between(dateTime, now)
        val hours = ChronoUnit.HOURS.between(dateTime, now)
        val days = ChronoUnit.DAYS.between(dateTime, now)

        when {
            minutes < 1 -> "刚刚"
            minutes < 60 -> "${minutes}分钟前"
            hours < 24 -> "${hours}小时前"
            days < 7 -> "${days}天前"
            else -> dateTime.format(displayFormatter)
        }
    } catch (e: Exception) {
        isoString
    }
}
