package com.trade.zt_webviewcap

import android.app.AlertDialog
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

class XmlCaptureActivity : ComponentActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_xml_capture)

        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl(WebViewCaptureHelper.DEFAULT_URL)

        findViewById<Button>(R.id.btnCapture).setOnClickListener {
            onCaptureClicked()
        }
    }

    private fun onCaptureClicked() {
        when (val outcome = WebViewCaptureHelper.capture(webView, this)) {
            is WebViewCaptureHelper.CaptureOutcome.Failure -> {
                Toast.makeText(this, outcome.message, Toast.LENGTH_SHORT).show()
            }
            is WebViewCaptureHelper.CaptureOutcome.Success -> {
                val path = outcome.result.filePath
                if (path != null) {
                    Toast.makeText(this, getString(R.string.capture_saved, path), Toast.LENGTH_LONG)
                        .show()
                } else {
                    Toast.makeText(this, R.string.capture_save_failed, Toast.LENGTH_SHORT).show()
                }
                showPreview(outcome.result.bitmap)
            }
        }
    }

    private fun showPreview(bitmap: android.graphics.Bitmap) {
        val imageView = ImageView(this).apply {
            setImageBitmap(bitmap)
            adjustViewBounds = true
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.capture_preview_title)
            .setView(imageView)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
