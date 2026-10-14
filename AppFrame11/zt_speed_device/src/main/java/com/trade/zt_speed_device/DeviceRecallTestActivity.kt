package com.trade.zt_speed_device

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import java.util.concurrent.Executors

/**
 * Device Recall / Play Integrity：获取 token，并可 decode 为 verdict JSON。
 */
class DeviceRecallTestActivity : ComponentActivity() {

    private lateinit var demo: DeviceRecallDemo
    private val ioExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        demo = DeviceRecallDemo(this)
        enableEdgeToEdge()
        setContent {
            AppFrame11Theme {
                DeviceRecallScreen(
                    demo = demo,
                    packageName = packageName,
                    decodeOnBg = { token, accessToken, onDone ->
                        ioExecutor.execute {
                            val result = IntegrityTokenDecoder.decode(
                                packageName = packageName,
                                integrityToken = token,
                                accessToken = accessToken
                            )
                            mainHandler.post { onDone(result) }
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        ioExecutor.shutdownNow()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceRecallScreen(
    demo: DeviceRecallDemo,
    packageName: String,
    decodeOnBg: (
        token: String,
        accessToken: String,
        onDone: (IntegrityTokenDecoder.DecodeResult) -> Unit
    ) -> Unit
) {
    var cloudProjectText by remember { mutableStateOf("655759993") }
    var tokenText by remember { mutableStateOf("") }
    var accessTokenText by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("先获取 token，再解码为 JSON") }
    var statusText by remember { mutableStateOf("package=$packageName") }

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

            OutlinedTextField(
                value = tokenText,
                onValueChange = { tokenText = it.trim() },
                label = { Text(stringResource(R.string.device_recall_integrity_token)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                minLines = 2,
                maxLines = 4
            )

            OutlinedTextField(
                value = accessTokenText,
                onValueChange = { accessTokenText = it.trim() },
                label = { Text(stringResource(R.string.device_recall_access_token)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy
            )

            Button(
                onClick = {
                    val projectNumber = cloudProjectText.toLongOrNull()
                    if (projectNumber == null || projectNumber <= 0L) {
                        statusText = "请填写有效的 Cloud Project Number"
                        return@Button
                    }
                    busy = true
                    statusText = "获取 token..."
                    demo.requestIntegrityToken(projectNumber)
                        .addOnSuccessListener { token ->
                            busy = false
                            tokenText = token
                            statusText = "OK token length=${token.length}，已填入第二个输入框"
                        }
                        .addOnFailureListener { e ->
                            busy = false
                            tokenText = ""
                            statusText = "FAIL: ${e.javaClass.simpleName}: ${e.message}"
                        }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.device_recall_get_token))
            }

            Button(
                onClick = {
                    if (tokenText.isBlank()) {
                        statusText = "请先获取或填写 Integrity Token"
                        return@Button
                    }
                    if (accessTokenText.isBlank()) {
                        statusText = "请填写带 playintegrity scope 的 OAuth Access Token"
                        return@Button
                    }
                    busy = true
                    statusText = "decodeIntegrityToken..."
                    decodeOnBg(tokenText, accessTokenText) { result ->
                        busy = false
                        if (result.success) {
                            resultText = result.json.orEmpty()
                            statusText = "OK 已解码为 JSON"
                        } else {
                            statusText = "FAIL: ${result.errorMessage}"
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.device_recall_decode_json))
            }

            Text(text = statusText)

            Text(
                text = resultText,
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
