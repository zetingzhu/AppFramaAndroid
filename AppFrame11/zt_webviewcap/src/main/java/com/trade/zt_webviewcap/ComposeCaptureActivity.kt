package com.trade.zt_webviewcap

import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.trade.zt_webviewcap.ui.theme.AppFrame11Theme

class ComposeCaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppFrame11Theme {
                ComposeCaptureScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeCaptureScreen() {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.compose_capture_title)) },
                actions = {
                    Button(
                        onClick = {
                            val webView = webViewRef
                            if (webView == null) {
                                Toast.makeText(
                                    context,
                                    R.string.capture_webview_not_ready,
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            when (val outcome = WebViewCaptureHelper.capture(webView, context)) {
                                is WebViewCaptureHelper.CaptureOutcome.Failure -> {
                                    Toast.makeText(context, outcome.message, Toast.LENGTH_SHORT)
                                        .show()
                                }
                                is WebViewCaptureHelper.CaptureOutcome.Success -> {
                                    val path = outcome.result.filePath
                                    if (path != null) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.capture_saved, path),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            R.string.capture_save_failed,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    previewBitmap = outcome.result.bitmap
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(stringResource(R.string.action_capture))
                    }
                }
            )
        }
    ) { innerPadding ->
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    webViewClient = WebViewClient()
                    loadUrl(WebViewCaptureHelper.DEFAULT_URL)
                    webViewRef = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onRelease = { it.destroy() }
        )
    }

    previewBitmap?.let { bitmap ->
        AlertDialog(
            onDismissRequest = { previewBitmap = null },
            title = { Text(stringResource(R.string.capture_preview_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { previewBitmap = null }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}
