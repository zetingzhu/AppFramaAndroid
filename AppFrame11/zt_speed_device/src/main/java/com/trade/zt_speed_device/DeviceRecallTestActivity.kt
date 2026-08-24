package com.trade.zt_speed_device

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
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
import com.trade.zt_speed_device.ui.theme.AppFrame11Theme

/**
 * Device Recall（Play Integrity beta）客户端测试页。
 *
 * 流程：填 Cloud Project Number → Warmup → Request Token →
 * 将 token 交给服务端 decode / deviceRecall:write。
 */
class DeviceRecallTestActivity : ComponentActivity() {

    private lateinit var demo: DeviceRecallDemo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demo = DeviceRecallDemo(this)
        enableEdgeToEdge()
        setContent {
            AppFrame11Theme {
                DeviceRecallScreen(demo = demo)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceRecallScreen(demo: DeviceRecallDemo) {
    var cloudProjectText by remember { mutableStateOf("323092438212") }
    var busy by remember { mutableStateOf(false) }
    var logText by remember {
        mutableStateOf(buildString {
            appendLine("Device Recall 测试说明")
            appendLine("1) Play Console 开通 Play Integrity，并申请/开启 Device Recall (beta)")
            appendLine("2) 填入关联的 Google Cloud 项目编号")
            appendLine("3) 点 Warmup（prepareIntegrityToken，会刷新 deviceRecall）")
            appendLine("4) 点 Request Token，把 token 发给服务端 decode")
            appendLine("5) 服务端用 deviceRecall:write 写 bit（客户端不能直接写）")
            appendLine()
            appendLine("--- 服务端 verdict 中 deviceRecall 示例 ---")
            appendLine(DeviceRecallDemo.SAMPLE_DEVICE_RECALL_VERDICT.trimIndent())
            appendLine("--- 服务端 write API 示意 ---")
            appendLine(DeviceRecallDemo.SAMPLE_WRITE_API.trimIndent())
            appendLine("--- 运行日志 ---")
        })
    }

    fun appendLog(line: String) {
        Log.d(TAG, line)
        logText = logText + line + "\n"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.device_recall_title)) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = cloudProjectText,
                onValueChange = { cloudProjectText = it.filter { ch -> ch.isDigit() } },
                label = { Text(stringResource(R.string.device_recall_cloud_project)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val projectNumber = cloudProjectText.toLongOrNull()
                        if (projectNumber == null || projectNumber <= 0L) {
                            appendLog("ERROR: 请填写有效的 Cloud Project Number")
                            return@Button
                        }
                        busy = true
                        appendLog(">> prepareIntegrityToken($projectNumber) ...")
                        demo.prepare(projectNumber)
                            .addOnSuccessListener {
                                busy = false
                                appendLog("OK: warmup 成功，tokenProvider 已就绪（deviceRecall 已刷新）")
                            }
                            .addOnFailureListener { e ->
                                busy = false
                                appendLog("FAIL: warmup ${e.javaClass.simpleName}: ${e.message}")
                            }
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.device_recall_warmup))
                }
                Button(
                    onClick = {
                        if (!demo.hasProvider()) {
                            appendLog("ERROR: 请先 Warmup")
                            return@Button
                        }
                        busy = true
                        val payload = "device_recall_demo_${System.currentTimeMillis()}"
                        appendLog(">> requestToken payload=$payload")
                        val task = demo.requestToken(payload)
                        if (task == null) {
                            busy = false
                            appendLog("ERROR: tokenProvider 为空")
                            return@Button
                        }
                        task.addOnSuccessListener { response ->
                            busy = false
                            val token = response.token()
                            Log.w(TAG, "token:$token")
                            appendLog("OK: 拿到 integrity token, length=${token.length}")
                            appendLog("token(prefix)=${token.take(48)}...")
                            appendLog("下一步：把完整 token POST 到服务端 decodeIntegrityToken，读取 deviceRecall")
                        }.addOnFailureListener { e ->
                            busy = false
                            appendLog("FAIL: request ${e.javaClass.simpleName}: ${e.message}")
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.device_recall_request_token))
                }
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

private const val TAG = "DeviceRecallTest"
