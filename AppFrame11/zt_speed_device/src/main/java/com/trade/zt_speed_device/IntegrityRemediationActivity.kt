package com.trade.zt_speed_device

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.play.core.integrity.model.IntegrityDialogTypeCode
import com.trade.zt_speed_device.ui.theme.AppFrame11Theme

/**
 * 读取 assets 中服务端 decode 的 verdict JSON，按官网规则推荐并弹出
 * Play Integrity 修复措施对话框（样式由 Google Play 系统对话框决定）。
 *
 * @see <a href="https://developer.android.com/google/play/integrity/remediation">Remediation dialogs</a>
 */
class IntegrityRemediationActivity : ComponentActivity() {

    private lateinit var helper: IntegrityRemediationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        helper = IntegrityRemediationHelper(this)
        val verdictJson = loadVerdictJson()
        enableEdgeToEdge()
        setContent {
            AppFrame11Theme {
                RemediationScreen(helper = helper, verdictJson = verdictJson)
            }
        }
    }

    private fun loadVerdictJson(): String {
        return try {
            assets.open(IntegrityRemediationHelper.ASSET_VERDICT_JSON)
                .bufferedReader()
                .use { it.readText() }
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemediationScreen(
    helper: IntegrityRemediationHelper,
    verdictJson: String
) {
    val analysis = remember(verdictJson) { IntegrityVerdictAnalyzer.analyze(verdictJson) }

    var busy by remember { mutableStateOf(false) }
    var logText by remember {
        mutableStateOf(
            buildString {
                appendLine("=== 服务端 verdict（assets/${IntegrityRemediationHelper.ASSET_VERDICT_JSON}）===")
                appendLine(verdictJson.trim())
                appendLine()
                appendLine("=== 判定分析（官网 remediation）===")
                appendLine("licensing=${analysis.licensingVerdict}")
                appendLine("appRecognition=${analysis.appRecognitionVerdict}")
                appendLine("device=${analysis.deviceRecognitionVerdicts}")
                appendLine(
                    "推荐对话框=${analysis.recommendedDialogName ?: "无"}" +
                        " (typeCode=${analysis.recommendedDialogTypeCode})"
                )
                analysis.reasons.forEach { appendLine("- $it") }
                appendLine()
                appendLine(analysis.dialogStyleHint)
                appendLine()
                appendLine("=== 运行日志 ===")
            }
        )
    }

    fun appendLog(line: String) {
        Log.d(TAG, line)
        logText = logText + line + "\n"
    }

    fun runShowDialog(typeCode: Int, typeName: String) {
        if (busy) return
        busy = true
        val project = IntegrityRemediationHelper.DEFAULT_CLOUD_PROJECT
        appendLog(">> prepare($project) ...")

        fun onTokenReady() {
            val payload = "remediation_demo_${System.currentTimeMillis()}"
            appendLog(">> requestToken ...")
            val task = helper.requestToken(payload)
            if (task == null) {
                busy = false
                appendLog("ERROR: 请先 warmup 成功")
                return
            }
            task.addOnSuccessListener { token ->
                appendLog("OK: token length=${token.token().length}")
                appendLog(">> showDialog($typeName / $typeCode) 样式由 Play 系统弹框展示")
                helper.showRemediationDialog(token, typeCode)
                    .addOnSuccessListener { code ->
                        busy = false
                        appendLog("OK: 对话框关闭 responseCode=$code（可再 warmup+request 验证是否修复）")
                    }
                    .addOnFailureListener { e ->
                        busy = false
                        appendLog("FAIL: showDialog ${e.javaClass.simpleName}: ${e.message}")
                    }
            }.addOnFailureListener { e ->
                busy = false
                appendLog("FAIL: request ${e.javaClass.simpleName}: ${e.message}")
            }
        }

        if (helper.hasProvider()) {
            onTokenReady()
        } else {
            helper.prepare(project)
                .addOnSuccessListener {
                    appendLog("OK: warmup 成功")
                    onTokenReady()
                }
                .addOnFailureListener { e ->
                    busy = false
                    appendLog("FAIL: warmup ${e.javaClass.simpleName}: ${e.message}")
                }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.integrity_remediation_title)) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val code = analysis.recommendedDialogTypeCode
                    val name = analysis.recommendedDialogName
                    if (code == null || name == null) {
                        appendLog("ERROR: 当前 JSON 未推荐对话框")
                        return@Button
                    }
                    runShowDialog(code, name)
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.integrity_remediation_show_recommended))
            }
            Button(
                onClick = {
                    runShowDialog(IntegrityDialogTypeCode.GET_LICENSED, "GET_LICENSED")
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.integrity_remediation_show_get_licensed))
            }
            Button(
                onClick = {
                    runShowDialog(IntegrityDialogTypeCode.GET_INTEGRITY, "GET_INTEGRITY")
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.integrity_remediation_show_get_integrity))
            }

            Text(
                text = logText,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
            )
        }
    }
}

private const val TAG = "IntegrityRemediationUI"
