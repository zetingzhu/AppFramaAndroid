package com.trade.zt_webviewcap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.WebView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 调用 [WebView.capturePicture] 转 Bitmap，并可选写入缓存目录。
 * 注意：capturePicture 已废弃，本模块刻意用于演示。
 */
object WebViewCaptureHelper {

    const val DEFAULT_URL = "https://www.baidu.com"

    data class Result(
        val bitmap: Bitmap,
        val filePath: String?
    )

    sealed class CaptureOutcome {
        data class Success(val result: Result) : CaptureOutcome()
        data class Failure(val message: String) : CaptureOutcome()
    }

    @Suppress("DEPRECATION")
    fun capture(webView: WebView, context: Context): CaptureOutcome {
        val picture = try {
            webView.capturePicture()
        } catch (e: Exception) {
            return CaptureOutcome.Failure(e.message ?: "capturePicture 失败")
        }

        val width = picture.width
        val height = picture.height
        if (width <= 0 || height <= 0) {
            return CaptureOutcome.Failure("暂不可截图：页面尚未完成布局")
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        picture.draw(canvas)

        val path = savePng(context, bitmap)
        return CaptureOutcome.Success(Result(bitmap = bitmap, filePath = path))
    }

    private fun savePng(context: Context, bitmap: Bitmap): String? {
        return try {
            val dir = File(context.cacheDir, "webview_capture").apply { mkdirs() }
            val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "capture_$name.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}
