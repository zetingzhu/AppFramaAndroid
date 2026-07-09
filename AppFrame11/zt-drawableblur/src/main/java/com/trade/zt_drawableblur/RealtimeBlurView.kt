package com.trade.zt_drawableblur

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver

/**
 * A self-contained view that blurs its underlying content.
 * It automatically finds the decor view and renders it into a blurred bitmap.
 */
class RealtimeBlurView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mDecorView: View? = null
    private var mBitmapToBlur: Bitmap? = null
    private var mCanvasToBlur: Canvas? = null
    private val mPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val mSampling = 4f
    private var mIsRendering = false

    private val mPreDrawListener = ViewTreeObserver.OnPreDrawListener {
        if (visibility == VISIBLE && width > 0 && height > 0) {
            updateBlur()
        }
        true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mDecorView = (context as? Activity)?.window?.decorView
        mDecorView?.viewTreeObserver?.addOnPreDrawListener(mPreDrawListener)
    }

    override fun onDetachedFromWindow() {
        mDecorView?.viewTreeObserver?.removeOnPreDrawListener(mPreDrawListener)
        mBitmapToBlur?.recycle()
        mBitmapToBlur = null
        super.onDetachedFromWindow()
    }

    private fun updateBlur() {
        val decor = mDecorView ?: return
        if (mIsRendering) return

        // Initialize or recreate bitmap if size changed
        val scaledWidth = (width / mSampling).toInt()
        val scaledHeight = (height / mSampling).toInt()
        
        if (mBitmapToBlur == null || mBitmapToBlur?.width != scaledWidth || mBitmapToBlur?.height != scaledHeight) {
            mBitmapToBlur?.recycle()
            mBitmapToBlur = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
            mCanvasToBlur = Canvas(mBitmapToBlur!!)
            
            // Apply RenderEffect for API 31+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRenderEffect(RenderEffect.createBlurEffect(15f, 15f, Shader.TileMode.CLAMP))
            }
        }

        val locations = IntArray(2)
        getLocationOnScreen(locations)
        val decorLoc = IntArray(2)
        decor.getLocationOnScreen(decorLoc)

        mCanvasToBlur?.save()
        mCanvasToBlur?.scale(1f / mSampling, 1f / mSampling)
        mCanvasToBlur?.translate(
            (decorLoc[0] - locations[0]).toFloat(),
            (decorLoc[1] - locations[1]).toFloat()
        )

        mIsRendering = true
        // Temporarily hide ourselves to avoid recursive drawing
        val oldVisibility = visibility
        visibility = INVISIBLE
        
        try {
            decor.draw(mCanvasToBlur!!)
        } catch (e: Exception) {
            // Ignore potential drawing errors
        }
        
        visibility = oldVisibility
        mIsRendering = false
        
        mCanvasToBlur?.restore()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mBitmapToBlur?.let {
            canvas.save()
            canvas.scale(mSampling, mSampling)
            canvas.drawBitmap(it, 0f, 0f, mPaint)
            canvas.restore()
        }
        // Tint overlay
        canvas.drawColor(0x44FFFFFF)
    }
}
