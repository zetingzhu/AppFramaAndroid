package com.trade.zt_sketchmcp.utils

/**
 * Stub implementation of SPUtils to allow successful compilation of the network interceptor.
 */
object SPUtils {
    const val USER_TOKEN = "USER_TOKEN"
    const val CUR_UID = "CUR_UID"
    const val USER_ID = "USER_ID"
    const val APP_SSID = "APP_SSID"

    fun get(key: String, default: String): String {
        return default.takeIf { it.isNotEmpty() } ?: "dummy_value"
    }
}
