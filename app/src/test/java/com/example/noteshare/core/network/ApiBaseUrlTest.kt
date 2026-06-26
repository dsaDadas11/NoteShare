package com.example.noteshare.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiBaseUrlTest {

    @Test
    fun resolveApiBaseUrl_loopbackOnEmulator_usesEmulatorHost() {
        val result = resolveApiBaseUrl(
            configuredBaseUrl = "http://127.0.0.1:8200/",
            isEmulator = true
        )
        assertEquals("http://10.0.2.2:8200/", result)
    }

    @Test
    fun resolveApiBaseUrl_localhostOnEmulator_usesEmulatorHost() {
        val result = resolveApiBaseUrl(
            configuredBaseUrl = "http://localhost:8200/",
            isEmulator = true
        )
        assertEquals("http://10.0.2.2:8200/", result)
    }

    @Test
    fun resolveApiBaseUrl_loopbackOnDevice_unchanged() {
        val configured = "http://127.0.0.1:8200/"
        val result = resolveApiBaseUrl(configuredBaseUrl = configured, isEmulator = false)
        assertEquals(configured, result)
    }

    @Test
    fun resolveApiBaseUrl_lanIpOnEmulator_unchanged() {
        val configured = "http://192.168.1.10:8200/"
        val result = resolveApiBaseUrl(configuredBaseUrl = configured, isEmulator = true)
        assertEquals(configured, result)
    }
}
