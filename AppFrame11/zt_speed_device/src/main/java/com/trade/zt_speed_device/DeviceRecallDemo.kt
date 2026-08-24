package com.trade.zt_speed_device

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import java.security.MessageDigest

/**
 * Play Integrity + Device Recall 客户端演示。
 *
 * Device Recall 使用方式（官方）：
 * 1. 客户端：Standard 请求先 [prepareIntegrityToken]（warmup，会刷新 deviceRecall），
 *    再 [request] 拿到 integrity token。
 * 2. 服务端：decodeIntegrityToken，从 verdict 的 `deviceRecall` 读取
 *    bitFirst / bitSecond / bitThird 与 writeDates。
 * 3. 服务端：在 token 有效期内（最长约 14 天）调用
 *    `POST .../deviceRecall:write` 写入三个自定义 bit。
 *
 * 客户端拿不到明文 recall 位；本类只演示客户端侧 warmup + 取 token。
 */
class DeviceRecallDemo(context: Context) {

    private val appContext = context.applicationContext
    private val manager: StandardIntegrityManager =
        IntegrityManagerFactory.createStandard(appContext)

    @Volatile
    private var tokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider? = null

    fun prepare(cloudProjectNumber: Long): Task<StandardIntegrityManager.StandardIntegrityTokenProvider> {
        Log.d(TAG, "prepareIntegrityToken cloudProjectNumber=$cloudProjectNumber")
        return manager.prepareIntegrityToken(
            StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(cloudProjectNumber)
                .build()
        ).addOnSuccessListener { provider ->
            tokenProvider = provider
            Log.d(TAG, "prepareIntegrityToken success (warmup / deviceRecall refresh)")
        }.addOnFailureListener { e ->
            Log.e(TAG, "prepareIntegrityToken failed", e)
        }
    }

    fun requestToken(
        actionPayload: String = "device_recall_demo"
    ): Task<StandardIntegrityManager.StandardIntegrityToken>? {
        val provider = tokenProvider
        if (provider == null) {
            Log.w(TAG, "requestToken skipped: call prepare() first")
            return null
        }
        val requestHash = sha256Hex(actionPayload)
        Log.d(TAG, "request integrity token requestHash=$requestHash payload=$actionPayload")
        return provider.request(
            StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
                .setRequestHash(requestHash)
                .build()
        ).addOnSuccessListener { token ->
            Log.d(TAG, "integrity token length=${token.token().length}")
        }.addOnFailureListener { e ->
            Log.e(TAG, "requestToken failed", e)
        }
    }

    fun hasProvider(): Boolean = tokenProvider != null

    companion object {
        private const val TAG = "DeviceRecallDemo"

        /** 服务端 decode 后 deviceRecall 字段示例（仅说明结构，非真实响应）。 */
        const val SAMPLE_DEVICE_RECALL_VERDICT = """
"deviceRecall": {
  "values": {
    "bitFirst": false,
    "bitSecond": false,
    "bitThird": false
  },
  "writeDates": {
    "yyyymmFirst": null,
    "yyyymmSecond": null,
    "yyyymmThird": null
  }
}
"""

        /** 服务端写入 Device Recall 的 HTTP 示意（需 OAuth / 服务账号）。 */
        const val SAMPLE_WRITE_API = """
POST https://playintegrity.googleapis.com/v1/{packageName}/deviceRecall:write
Authorization: Bearer <service-account-access-token>
Content-Type: application/json

{
  "integrity_token": "<token from client>",
  "new_values": {
    "bit_first": true,
    "bit_second": false,
    "bit_third": true
  }
}
"""

        fun sha256Hex(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { b -> "%02x".format(b) }
        }
    }
}
