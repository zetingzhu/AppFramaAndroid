package com.trade.zt_speed_device

import org.json.JSONArray
import org.json.JSONObject

/**
 * 客户端 Key Attestation 结果（证明材料），用 [toServerPayloadJson] 发给服务端验证。
 */
data class KeyAttestResult(
    val success: Boolean,
    val keyAlias: String = KeyAttestationHelper.KEY_ALIAS,
    val challengeBase64: String? = null,
    /** 证书链 DER Base64，推荐上报 */
    val certificateDerBase64Chain: List<String> = emptyList(),
    /** StrongBox / TEE */
    val backend: String? = null,
    val errorMessage: String? = null
) {
    fun toServerPayloadJson(): String {
        val root = JSONObject()
        root.put("success", success)
        root.put("keyAlias", keyAlias)
        root.put("backend", backend ?: JSONObject.NULL)
        root.put("challengeBase64", challengeBase64 ?: JSONObject.NULL)
        root.put("certificateChainDerBase64", JSONArray(certificateDerBase64Chain))
        if (errorMessage != null) {
            root.put("errorMessage", errorMessage)
        }
        return root.toString(2)
    }

    companion object {
        fun failure(errorMessage: String): KeyAttestResult {
            return KeyAttestResult(success = false, errorMessage = errorMessage)
        }
    }
}
