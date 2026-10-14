package com.trade.zt_speed_device

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.UUID

/**
 * Key Attestation：优先 StrongBox，失败降级 TEE，返回 [KeyAttestResult] 供上报服务端。
 *
 * @see <a href="https://developer.android.com/privacy-and-security/security-key-attestation">Key attestation</a>
 */
object KeyAttestationHelper {

    const val KEY_ALIAS = "zt_hw_attested_ec"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    fun isStrongBoxAvailable(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
    }

    /**
     * 单一入口：优先 StrongBox → 降级 TEE。
     * @param serverChallenge 服务端下发的一次性 nonce（≤128 字节）；为空则本地生成
     */
    fun generateAttestedKeyPairPreferStrongBox(
        context: Context,
        serverChallenge: ByteArray? = null
    ): KeyAttestResult {
        val challenge = serverChallenge?.takeIf { it.isNotEmpty() } ?: defaultChallenge()

        if (isStrongBoxAvailable(context)) {
            val strongBox = generateAttestedKeyPair(context, useStrongBox = true, challenge)
            if (strongBox.success) return strongBox
        }

        return generateAttestedKeyPair(context, useStrongBox = false, challenge)
    }

    private fun generateAttestedKeyPair(
        context: Context,
        useStrongBox: Boolean,
        challenge: ByteArray
    ): KeyAttestResult {
        return try {
            deleteKeyIfExists()

            if (useStrongBox && !isStrongBoxAvailable(context)) {
                return KeyAttestResult.failure("StrongBox unavailable")
            }

            val purposes = KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            val builder = KeyGenParameterSpec.Builder(KEY_ALIAS, purposes)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setAttestationChallenge(challenge)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && useStrongBox) {
                builder.setIsStrongBoxBacked(true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setDevicePropertiesAttestationIncluded(true)
            }

            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
            try {
                kpg.initialize(builder.build())
                kpg.generateKeyPair()
            } catch (first: Exception) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) throw first
                // 部分机型不支持 device properties attestation，去掉后重试
                deleteKeyIfExists()
                val retry = KeyGenParameterSpec.Builder(KEY_ALIAS, purposes)
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setAttestationChallenge(challenge)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && useStrongBox) {
                    retry.setIsStrongBoxBacked(true)
                }
                kpg.initialize(retry.build())
                kpg.generateKeyPair()
            }

            val chain = loadCertificateChain()
            if (chain.isEmpty()) {
                return KeyAttestResult.failure("Empty certificate chain")
            }

            KeyAttestResult(
                success = true,
                challengeBase64 = Base64.encodeToString(challenge, Base64.NO_WRAP),
                certificateDerBase64Chain = chain.map {
                    Base64.encodeToString(it.encoded, Base64.NO_WRAP)
                },
                backend = if (useStrongBox) "StrongBox" else "TEE"
            )
        } catch (e: Exception) {
            KeyAttestResult.failure(e.message ?: e.javaClass.simpleName)
        }
    }

    fun deleteKeyIfExists() {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (ks.containsAlias(KEY_ALIAS)) {
            ks.deleteEntry(KEY_ALIAS)
        }
    }

    private fun defaultChallenge(): ByteArray {
        return "zt-attestation-${UUID.randomUUID()}".toByteArray(Charsets.UTF_8)
    }

    private fun loadCertificateChain(): List<X509Certificate> {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val chain = ks.getCertificateChain(KEY_ALIAS) ?: return emptyList()
        return chain.map { it as X509Certificate }
    }
}
