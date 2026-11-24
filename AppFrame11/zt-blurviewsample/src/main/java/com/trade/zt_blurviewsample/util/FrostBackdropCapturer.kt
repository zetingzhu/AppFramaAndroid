package com.trade.zt_blurviewsample.util

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import android.widget.ImageView
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 抓取磨砂条正下方窗口像素，再交给 [RenderEffectFrostHelper] 做模糊。
 */
class FrostBackdropCapturer(
    private val window: Window,
    private val frostHost: View,
    private val blurLayer: ImageView,
) {
    private val capturing = AtomicBoolean(false)
    private var pending = false

    fun capture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        if (!capturing.compareAndSet(false, true)) {
            pending = true
            return
        }
        val width = frostHost.width
        val height = frostHost.height
        if (width <= 0 || height <= 0) {
            capturing.set(false)
            return
        }

        val loc = IntArray(2)
        frostHost.getLocationInWindow(loc)
        val rect = Rect(loc[0], loc[1], loc[0] + width, loc[1] + height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val hostVisibility = frostHost.visibility
        frostHost.visibility = View.INVISIBLE

        PixelCopy.request(window, rect, bitmap, { result ->
            frostHost.visibility = hostVisibility
            if (result == PixelCopy.SUCCESS) {
                blurLayer.setImageBitmap(bitmap)
                RenderEffectFrostHelper.applyBlurToBannerLayer(blurLayer)
            }
            capturing.set(false)
            if (pending) {
                pending = false
                frostHost.post { capture() }
            }
        }, Handler(Looper.getMainLooper()))
    }
}