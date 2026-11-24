package com.trade.zt_blurviewsample.util

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.widget.ImageView

/**
 * 在 Banner 内对「与背后同源的」背景层做 [RenderEffect] 高斯模糊，再叠半透明白即为磨砂。
 * API 31 以下仅保留半透明层，不应用模糊。
 */
object RenderEffectFrostHelper {

    private const val BLUR_RADIUS_X = 12f
    private const val BLUR_RADIUS_Y = 12f

    fun applyBlurToBannerLayer(blurLayer: ImageView) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            blurLayer.setRenderEffect(null)
            return
        }
        val effect = RenderEffect.createBlurEffect(
            BLUR_RADIUS_X,
            BLUR_RADIUS_Y,
            Shader.TileMode.CLAMP
        )
        blurLayer.setRenderEffect(effect)
    }
}