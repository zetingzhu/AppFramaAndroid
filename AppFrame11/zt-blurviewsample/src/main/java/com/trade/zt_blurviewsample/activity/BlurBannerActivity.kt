package com.trade.zt_blurviewsample.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.trade.zt_blurviewsample.R
import com.trade.zt_blurviewsample.util.FrostBackdropCapturer
import com.trade.zt_blurviewsample.view.BoundedDragFrameLayout

/**
 * RenderEffect + PixelCopy 演示：不适合拖动过程中实时重绘模糊（会闪烁、异步错位）。
 * 拖动时只平移 + 半透明叠层；松手后单次截屏并模糊。跟手实时磨砂见 [BlurViewDimezisActivity]。
 */
class BlurBannerActivity : AppCompatActivity() {

    companion object {
        private const val DRAG_PREVIEW_BLUR_ALPHA = 0.2f

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, BlurBannerActivity::class.java))
        }
    }

    private lateinit var capturer: FrostBackdropCapturer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_blur_banner)

        val main = findViewById<android.view.View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val frostDragHost = findViewById<BoundedDragFrameLayout>(R.id.frostDragHost)
        val blurLayer = findViewById<ImageView>(R.id.blurLayer)
        capturer = FrostBackdropCapturer(window, frostDragHost, blurLayer)
        frostDragHost.dragBoundsView = main

        val refreshBlurBackdrop: () -> Unit = { capturer.capture() }

        frostDragHost.onDragStarted = {
            blurLayer.alpha = DRAG_PREVIEW_BLUR_ALPHA
        }
        frostDragHost.onDragEnded = {
            blurLayer.alpha = 1f
            refreshBlurBackdrop()
        }

        frostDragHost.post(refreshBlurBackdrop)
    }
}