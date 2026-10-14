package com.trade.zt_speed_device

import org.json.JSONObject

enum class DeviceAttestChannel {
    PLAY_INTEGRITY,
    KEY_ATTESTATION
}

/**
 * 统一验真结果：优先 Play Integrity token，否则 Key Attestation 证明材料。
 */
data class DeviceAttestResult(
    val success: Boolean,
    val channel: DeviceAttestChannel?,
    /** Play Integrity 时有值 */
    val integrityToken: String? = null,
    /** Key Attestation 时有值 */
    val keyAttest: KeyAttestResult? = null,
    val errorMessage: String? = null
) {
    fun toServerPayloadJson(): String {
        val root = JSONObject()
        root.put("success", success)
        root.put("channel", channel?.name ?: JSONObject.NULL)
        when (channel) {
            DeviceAttestChannel.PLAY_INTEGRITY -> {
                root.put("integrityToken", integrityToken ?: JSONObject.NULL)
            }
            DeviceAttestChannel.KEY_ATTESTATION -> {
                root.put(
                    "keyAttest",
                    if (keyAttest != null) JSONObject(keyAttest.toServerPayloadJson())
                    else JSONObject.NULL
                )
            }
            null -> Unit
        }
        if (errorMessage != null) {
            root.put("errorMessage", errorMessage)
        }
        return root.toString(2)
    }

    companion object {
        fun failure(message: String): DeviceAttestResult {
            return DeviceAttestResult(
                success = false,
                channel = null,
                errorMessage = message
            )
        }
    }
}
