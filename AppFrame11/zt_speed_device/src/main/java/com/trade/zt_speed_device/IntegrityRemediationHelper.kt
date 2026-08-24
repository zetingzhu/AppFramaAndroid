package com.trade.zt_speed_device

import android.app.Activity
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityDialogRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityDialogRequest.StandardIntegrityResponse

/**
 * 官方流程：拿到 [StandardIntegrityToken] 后，按服务端指示的 typeCode 调用
 * [StandardIntegrityManager.showDialog]。
 */
class IntegrityRemediationHelper(private val activity: Activity) {

    private val manager: StandardIntegrityManager =
        IntegrityManagerFactory.createStandard(activity.applicationContext)

    @Volatile
    private var tokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider? = null

    fun prepare(cloudProjectNumber: Long): Task<StandardIntegrityManager.StandardIntegrityTokenProvider> {
        return manager.prepareIntegrityToken(
            StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(cloudProjectNumber)
                .build()
        ).addOnSuccessListener { tokenProvider = it }
            .addOnFailureListener { e -> Log.e(TAG, "prepare failed", e) }
    }

    fun requestToken(payload: String): Task<StandardIntegrityManager.StandardIntegrityToken>? {
        val provider = tokenProvider ?: return null
        val hash = DeviceRecallDemo.sha256Hex(payload)
        return provider.request(
            StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
                .setRequestHash(hash)
                .build()
        )
    }

    fun showRemediationDialog(
        token: StandardIntegrityManager.StandardIntegrityToken,
        typeCode: Int
    ): Task<Int> {
        val dialogRequest = StandardIntegrityDialogRequest.builder()
            .setActivity(activity)
            .setTypeCode(typeCode)
            .setStandardIntegrityResponse(StandardIntegrityResponse.TokenResponse(token))
            .build()
        Log.d(TAG, "showDialog typeCode=$typeCode")
        return manager.showDialog(dialogRequest)
    }

    fun hasProvider(): Boolean = tokenProvider != null

    companion object {
        private const val TAG = "IntegrityRemediation"
        const val DEFAULT_CLOUD_PROJECT = 655759993L
        const val ASSET_VERDICT_JSON = "integrity_verdict_sample.json"
    }
}
