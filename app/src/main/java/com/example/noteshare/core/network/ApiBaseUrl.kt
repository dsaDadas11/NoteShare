package com.example.noteshare.core.network

import android.os.Build
import com.example.noteshare.BuildConfig

/**
 * Resolves the API/media base URL for the current runtime.
 * Emulator cannot reach host loopback via 127.0.0.1; use 10.0.2.2 instead.
 */
fun resolveApiBaseUrl(
    configuredBaseUrl: String = BuildConfig.BASE_URL,
    isEmulator: Boolean = isAndroidEmulator()
): String {
    val hostLoopback = configuredBaseUrl.startsWith("http://127.0.0.1:8200/") ||
        configuredBaseUrl.startsWith("http://localhost:8200/")
    return if (hostLoopback && isEmulator) {
        "http://10.0.2.2:8200/"
    } else {
        configuredBaseUrl
    }
}

internal fun isAndroidEmulator(): Boolean {
    val fingerprint = Build.FINGERPRINT.lowercase()
    val model = Build.MODEL.lowercase()
    val manufacturer = Build.MANUFACTURER.lowercase()
    val brand = Build.BRAND.lowercase()
    val device = Build.DEVICE.lowercase()
    val product = Build.PRODUCT.lowercase()
    return fingerprint.startsWith("generic") ||
        fingerprint.contains("emulator") ||
        model.contains("emulator") ||
        model.contains("android sdk built for") ||
        manufacturer.contains("genymotion") ||
        (brand.startsWith("generic") && device.startsWith("generic")) ||
        product.contains("sdk_gphone") ||
        product.contains("emulator") ||
        product.contains("simulator")
}
