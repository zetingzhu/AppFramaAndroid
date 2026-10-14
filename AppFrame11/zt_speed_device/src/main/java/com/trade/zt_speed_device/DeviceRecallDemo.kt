package com.trade.zt_speed_device

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import java.security.MessageDigest

/**
 * Play Integrity Standard 请求工具：一次完成 warmup + request，返回 integrity token 字符串，供上报服务端。
 *
 * Device Recall 的 bit 读写在服务端 decode / deviceRecall:write 完成。
 */
class DeviceRecallDemo(context: Context) {

    private val manager: StandardIntegrityManager =
        IntegrityManagerFactory.createStandard(context.applicationContext)

    /**
     * 合并 prepareIntegrityToken + request，返回最终 token。
     *
     * @param cloudProjectNumber Google Cloud 项目编号
     * @param actionPayload 参与 requestHash 的业务载荷（建议每次唯一）
     */
    fun requestIntegrityToken(
        cloudProjectNumber: Long,
        actionPayload: String = "integrity_${System.currentTimeMillis()}"
    ): Task<String> {
        val prepareRequest = StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
            .setCloudProjectNumber(cloudProjectNumber)
            .build()

        return manager.prepareIntegrityToken(prepareRequest)
            .onSuccessTask { provider ->
                val requestHash = sha256Hex(actionPayload)
                provider.request(
                    StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
                        .setRequestHash(requestHash)
                        .build()
                )
            }
            .onSuccessTask { tokenResponse ->
                com.google.android.gms.tasks.Tasks.forResult(tokenResponse.token())
            }
    }

    companion object {
        fun sha256Hex(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { b -> "%02x".format(b) }
        }
    }
}
