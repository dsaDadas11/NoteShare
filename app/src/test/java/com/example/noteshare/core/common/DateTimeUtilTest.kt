package com.example.noteshare.core.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * DateTimeUtil 单元测试
 * 覆盖：刚刚、X分钟前、X小时前、X天前、格式化日期、null/空白/无效格式处理
 *
 * 注意：测试中使用固定时间构造ISO字符串，formatDateTime内部调用 LocalDateTime.now()
 * 通过构造特定时间差来触发各分支
 */
class DateTimeUtilTest {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    /**
     * 辅助方法：构造距当前时间N分钟前的ISO字符串
     */
    private fun minutesAgo(minutes: Int): String {
        return LocalDateTime.now().minusMinutes(minutes.toLong()).format(formatter)
    }

    /**
     * 辅助方法：构造距当前时间N小时前的ISO字符串
     */
    private fun hoursAgo(hours: Int): String {
        return LocalDateTime.now().minusHours(hours.toLong()).format(formatter)
    }

    /**
     * 辅助方法：构造距当前时间N天前的ISO字符串
     */
    private fun daysAgo(days: Int): String {
        return LocalDateTime.now().minusDays(days.toLong()).format(formatter)
    }

    /**
     * 刚刚：输入为null时返回空字符串
     */
    @Test
    fun formatDateTime_nullInput_returnsEmptyString() {
        val result = formatDateTime(null)
        assertEquals("", result)
    }

    /**
     * 刚刚：输入为空字符串时返回空字符串
     */
    @Test
    fun formatDateTime_emptyInput_returnsEmptyString() {
        val result = formatDateTime("")
        assertEquals("", result)
    }

    /**
     * 刚刚：输入为纯空白字符时返回空字符串
     */
    @Test
    fun formatDateTime_blankInput_returnsEmptyString() {
        val result = formatDateTime("   ")
        assertEquals("", result)
    }

    /**
     * 刚刚：1分钟以内的时间应返回"刚刚"
     */
    @Test
    fun formatDateTime_30SecondsAgo_returnsGangGang() {
        val time = LocalDateTime.now().minusSeconds(30)
        val isoString = time.format(formatter)
        val result = formatDateTime(isoString)
        assertEquals("刚刚", result)
    }

    /**
     * 边界：刚好0分钟（现在）应返回"刚刚"
     */
    @Test
    fun formatDateTime_now_returnsGangGang() {
        val isoString = LocalDateTime.now().format(formatter)
        val result = formatDateTime(isoString)
        assertEquals("刚刚", result)
    }

    /**
     * X分钟前：1分钟前
     */
    @Test
    fun formatDateTime_1MinuteAgo_returns1MinAgo() {
        val result = formatDateTime(minutesAgo(1))
        assertEquals("1分钟前", result)
    }

    /**
     * X分钟前：5分钟前
     */
    @Test
    fun formatDateTime_5MinutesAgo_returns5MinAgo() {
        val result = formatDateTime(minutesAgo(5))
        assertEquals("5分钟前", result)
    }

    /**
     * X分钟前：59分钟前（分钟上限边界）
     */
    @Test
    fun formatDateTime_59MinutesAgo_returns59MinAgo() {
        val result = formatDateTime(minutesAgo(59))
        assertEquals("59分钟前", result)
    }

    /**
     * X小时前：1小时前
     */
    @Test
    fun formatDateTime_1HourAgo_returns1HourAgo() {
        val result = formatDateTime(hoursAgo(1))
        assertEquals("1小时前", result)
    }

    /**
     * X小时前：12小时前
     */
    @Test
    fun formatDateTime_12HoursAgo_returns12HourAgo() {
        val result = formatDateTime(hoursAgo(12))
        assertEquals("12小时前", result)
    }

    /**
     * X小时前：23小时前（小时上限边界）
     */
    @Test
    fun formatDateTime_23HoursAgo_returns23HourAgo() {
        val result = formatDateTime(hoursAgo(23))
        assertEquals("23小时前", result)
    }

    /**
     * X天前：1天前
     */
    @Test
    fun formatDateTime_1DayAgo_returns1DayAgo() {
        val result = formatDateTime(daysAgo(1))
        assertEquals("1天前", result)
    }

    /**
     * X天前：3天前
     */
    @Test
    fun formatDateTime_3DaysAgo_returns3DayAgo() {
        val result = formatDateTime(daysAgo(3))
        assertEquals("3天前", result)
    }

    /**
     * X天前：6天前（天上限边界）
     */
    @Test
    fun formatDateTime_6DaysAgo_returns6DayAgo() {
        val result = formatDateTime(daysAgo(6))
        assertEquals("6天前", result)
    }

    /**
     * 格式化日期：7天前应返回完整的 yyyy-MM-dd HH:mm 格式
     */
    @Test
    fun formatDateTime_7DaysAgo_returnsFormattedDate() {
        val result = formatDateTime(daysAgo(7))
        // 应匹配 yyyy-MM-dd HH:mm 格式，如 "2025-01-15 10:30"
        val expectedPattern = Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}""")
        assertTrue(
            "Result '$result' should match date pattern yyyy-MM-dd HH:mm",
            expectedPattern.matches(result)
        )
    }

    /**
     * 格式化日期：30天前应返回完整的格式化日期
     */
    @Test
    fun formatDateTime_30DaysAgo_returnsFormattedDate() {
        val result = formatDateTime(daysAgo(30))
        val expectedPattern = Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}""")
        assertTrue(
            "Result '$result' should match date pattern yyyy-MM-dd HH:mm",
            expectedPattern.matches(result)
        )
    }

    /**
     * 无效时间格式：应返回原始字符串
     */
    @Test
    fun formatDateTime_invalidFormat_returnsOriginalString() {
        val invalid = "this is not a date"
        val result = formatDateTime(invalid)
        assertEquals(invalid, result)
    }

    /**
     * 无效时间格式：部分ISO格式（缺少时间部分）
     */
    @Test
    fun formatDateTime_partialIso_returnsOriginalString() {
        val partial = "2025-01-15"
        val result = formatDateTime(partial)
        assertEquals(partial, result)
    }

    /**
     * 无效时间格式：随机字符串
     */
    @Test
    fun formatDateTime_randomString_returnsOriginalString() {
        val random = "abc123def456"
        val result = formatDateTime(random)
        assertEquals(random, result)
    }

    // 辅助断言方法
    private fun assertTrue(message: String, condition: Boolean) {
        org.junit.Assert.assertTrue(message, condition)
    }
}
