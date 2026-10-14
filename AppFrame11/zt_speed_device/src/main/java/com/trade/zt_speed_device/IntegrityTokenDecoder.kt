package com.trade.zt_speed_device

import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * 调用 Google Play Integrity [decodeIntegrityToken] 把加密 token 解成 verdict JSON。
 * 需要带 `https://www.googleapis.com/auth/playintegrity` 的 OAuth Access Token
 *（正式环境应放在服务端，此处仅便于本地调试）。
 */
object IntegrityTokenDecoder {

    data class DecodeResult(
        val success: Boolean,
        val json: String?,
        val errorMessage: String? = null
    )

    fun decode(
        packageName: String,
        integrityToken: String,
        accessToken: String
    ): DecodeResult {
        if (integrityToken.isBlank()) {
            return DecodeResult(false, null, "token 为空")
        }
        if (accessToken.isBlank()) {
            return DecodeResult(false, null, "Access Token 为空")
        }

        return try {
            val url = URL(
                "https://playintegrity.googleapis.com/v1/$packageName:decodeIntegrityToken"
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 30_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            val body = JSONObject().put("integrity_token", integrityToken).toString()
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val response = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()

            if (code in 200..299) {
                // 尽量 pretty-print
                val pretty = try {
                    JSONObject(response).toString(2)
                } catch (_: Exception) {
                    response
                }
                DecodeResult(true, pretty)
            } else {
                DecodeResult(false, null, "HTTP $code: $response")
            }
        } catch (e: Exception) {
            DecodeResult(false, null, "${e.javaClass.simpleName}: ${e.message}")
        }
    }
}
