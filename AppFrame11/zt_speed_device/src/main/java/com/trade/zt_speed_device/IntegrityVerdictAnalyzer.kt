package com.trade.zt_speed_device

import com.google.android.play.core.integrity.model.IntegrityDialogTypeCode
import org.json.JSONArray
import org.json.JSONObject

/**
 * 按 [修复措施对话框](https://developer.android.com/google/play/integrity/remediation)
 * 规则，从服务端 decode 后的 verdict JSON 推断应展示的对话框类型。
 */
object IntegrityVerdictAnalyzer {

    data class Analysis(
        val licensingVerdict: String?,
        val appRecognitionVerdict: String?,
        val deviceRecognitionVerdicts: List<String>,
        val recommendedDialogTypeCode: Int?,
        val recommendedDialogName: String?,
        val dialogStyleHint: String,
        val reasons: List<String>
    )

    fun analyze(rawJson: String): Analysis {
        val root = JSONObject(rawJson)
        val payload = root.optJSONObject("tokenPayloadExternal") ?: root

        val licensing = payload.optJSONObject("accountDetails")
            ?.optString("appLicensingVerdict")
            ?.takeIf { it.isNotBlank() }

        val appRecognition = payload.optJSONObject("appIntegrity")
            ?.optString("appRecognitionVerdict")
            ?.takeIf { it.isNotBlank() }

        val deviceVerdicts = payload.optJSONObject("deviceIntegrity")
            ?.optJSONArray("deviceRecognitionVerdict")
            .toStringList()

        val reasons = mutableListOf<String>()
        var typeCode: Int? = null
        var typeName: String? = null
        var styleHint = "无需弹框：当前判定未命中文档中的修复场景。"

        val meetsDevice = deviceVerdicts.contains("MEETS_DEVICE_INTEGRITY")
        val meetsStrong = deviceVerdicts.contains("MEETS_STRONG_INTEGRITY")

        when {
            licensing == "UNLICENSED" || appRecognition == "UNRECOGNIZED_VERSION" -> {
                // 官网：GET_LICENSED 同时覆盖未授权 + 篡改/未识别版本
                typeCode = IntegrityDialogTypeCode.GET_LICENSED
                typeName = "GET_LICENSED"
                reasons += if (appRecognition == "UNRECOGNIZED_VERSION") {
                    "appRecognitionVerdict=UNRECOGNIZED_VERSION → GET_LICENSED(1)"
                } else {
                    "appLicensingVerdict=UNLICENSED → GET_LICENSED(1)"
                }
                styleHint = STYLE_GET_LICENSED
            }
            !meetsDevice -> {
                typeCode = IntegrityDialogTypeCode.GET_INTEGRITY
                typeName = "GET_INTEGRITY"
                reasons += "deviceRecognitionVerdict 不含 MEETS_DEVICE_INTEGRITY → GET_INTEGRITY(4)"
                styleHint = STYLE_GET_INTEGRITY
            }
            !meetsStrong && deviceVerdicts.isNotEmpty() -> {
                // 仅当业务关心 STRONG 时才弹；这里给出可选建议
                reasons += "已 MEETS_DEVICE_INTEGRITY；若需 STRONG 可用 GET_STRONG_INTEGRITY(5)"
                styleHint = STYLE_GET_STRONG
            }
            else -> {
                reasons += "未发现需修复的判定字段"
            }
        }

        if (licensing == "UNEVALUATED") {
            reasons += "appLicensingVerdict=UNEVALUATED（未评估许可，通常不能单靠它触发 GET_LICENSED）"
        }

        return Analysis(
            licensingVerdict = licensing,
            appRecognitionVerdict = appRecognition,
            deviceRecognitionVerdicts = deviceVerdicts,
            recommendedDialogTypeCode = typeCode,
            recommendedDialogName = typeName,
            dialogStyleHint = styleHint,
            reasons = reasons
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                optString(i)?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }
    }

    private val STYLE_GET_LICENSED = """
弹框样式（官网 GET_LICENSED / 图 1）：
- 由 Google Play 系统对话框展示，应用无法自定义 UI
- 引导用户从 Google Play 获取正版应用
- 未授权：授予 Play 许可，便于后续更新
- 篡改/UNRECOGNIZED_VERSION：引导安装未修改的 Play 版本
完成后期望：LICENSED + PLAY_RECOGNIZED
""".trimIndent()

    private val STYLE_GET_INTEGRITY = """
弹框样式（官网 GET_INTEGRITY / 图 4）：
- Play 系统连续引导流，自动检测多项问题
- 可覆盖设备完整性、许可/应用完整性、可修复客户端异常
- 应用侧只调 showDialog，样式由 Play 决定
""".trimIndent()

    private val STYLE_GET_STRONG = """
弹框样式（官网 GET_STRONG_INTEGRITY / 图 5）：
- 在 GET_INTEGRITY 基础上还处理 STRONG 完整性与 Play Protect
- 可能提示更新 Play 服务、开启保护机制、卸载有害应用
""".trimIndent()
}
