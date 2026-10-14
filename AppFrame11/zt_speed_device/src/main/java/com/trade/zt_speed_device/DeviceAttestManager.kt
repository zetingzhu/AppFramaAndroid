package com.trade.zt_speed_device

import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks

/**
 * 设备验真管理：优先 Play Integrity；设备不支持或请求失败时降级 Key Attestation。
 */
class DeviceAttestManager(context: Context) {

    private val appContext = context.applicationContext
    private val integrity = DeviceRecallDemo(appContext)

    /**
     * @param cloudProjectNumber Play Integrity 用的 Cloud 项目号
     * @param serverChallenge Key Attestation 用的服务端 nonce；Play Integrity 失败降级时使用
     * @param actionPayload Play Integrity requestHash 业务载荷
     */
    fun requestAttestation(
        cloudProjectNumber: Long = DEFAULT_CLOUD_PROJECT_NUMBER,
        serverChallenge: ByteArray? = null,
        actionPayload: String = "attest_${System.currentTimeMillis()}"
    ): Task<DeviceAttestResult> {
        if (!isPlayIntegritySupported(appContext)) {
            return Tasks.forResult(attestWithKey(serverChallenge))
        }

        return integrity.requestIntegrityToken(cloudProjectNumber, actionPayload)
            .continueWith { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    if (!token.isNullOrEmpty()) {
                        DeviceAttestResult(
                            success = true,
                            channel = DeviceAttestChannel.PLAY_INTEGRITY,
                            integrityToken = token
                        )
                    } else {
                        attestWithKey(serverChallenge)
                    }
                } else {
                    // Play Integrity 调用失败，降级 Key Attestation
                    attestWithKey(serverChallenge)
                }
            }
    }

    private fun attestWithKey(serverChallenge: ByteArray?): DeviceAttestResult {
        val keyResult = KeyAttestationHelper.generateAttestedKeyPairPreferStrongBox(
            appContext,
            serverChallenge
        )
        return if (keyResult.success) {
            DeviceAttestResult(
                success = true,
                channel = DeviceAttestChannel.KEY_ATTESTATION,
                keyAttest = keyResult
            )
        } else {
            DeviceAttestResult.failure(
                keyResult.errorMessage ?: "Key attestation failed"
            )
        }
    }

    companion object {
        const val DEFAULT_CLOUD_PROJECT_NUMBER = 655759993L

        /** 是否具备调用 Play Integrity 的基本条件（GMS + Play 商店） */
        fun isPlayIntegritySupported(context: Context): Boolean {
            return hasPackage(context, "com.google.android.gms") &&
                hasPackage(context, "com.android.vending")
        }

        private fun hasPackage(context: Context, packageName: String): Boolean {
            return try {
                context.packageManager.getPackageInfo(packageName, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }
}
