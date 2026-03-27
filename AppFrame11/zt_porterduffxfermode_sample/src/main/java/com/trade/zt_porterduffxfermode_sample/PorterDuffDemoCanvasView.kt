package com.trade.zt_porterduffxfermode_sample

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.view.View

/**
 * 核心自定义 View（基于 Kotlin 编写）。
 * 演示了如何通过 [PorterDuffXfermode] 将两层图形混合在一起。
 * 此类在 Compose 版本 (通过 AndroidView 桥接) 和 KotlinView 版本中均被复用。
 */
class PorterDuffDemoCanvasView(context: Context) : View(context) {
    // 对外暴露的混合模式属性。一旦改变，立刻调用 invalidate() 触发重绘。
    var mode: PorterDuff.Mode = PorterDuff.Mode.SRC_OVER
        set(value) {
            field = value
            invalidate()
        }

    // 源图形的整体透明度 (0.0 - 1.0)
    var srcAlpha: Float = 1.0f
        set(value) {
            field = value
            if (!useGradient) {
                // 更新画笔的 alpha 值 (0 - 255)
                srcPaint.alpha = (value * 255).toInt()
            }
            invalidate()
        }

    // 是否使用线性渐变作为源画笔
    var useGradient: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    // 目标图层 (Destination) 画笔：绘制底部的蓝色圆角矩形
    private val dstPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3F51B5")
    }
    // 源图层 (Source) 画笔：绘制上层的绿色椭圆，这是应用 Xfermode 的画笔
    private val srcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
    }
    // 文字标签画笔：用于在画布上标注 DST 和 SRC 的位置
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 34f
    }
    // 背景画笔：填充画布底色
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F3F5F8")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        
        // 1. 绘制底色，避免背景透明导致混合效果不明显
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // 2. 核心步骤：创建一个新的离屏图层 (Off-screen Layer)
        val layer = canvas.saveLayer(0f, 0f, w, h, null)
        
        val dstRect = RectF(w * 0.14f, h * 0.18f, w * 0.62f, h * 0.74f)
        // 为了演示完美的渐变遮罩(如边缘淡出)，当使用渐变时，让源图形完全覆盖目标图形
        val srcRect = if (useGradient) {
            RectF(dstRect)
        } else {
            RectF(w * 0.38f, h * 0.30f, w * 0.86f, h * 0.88f)
        }
        
        // 判断当前是否是需要 DST 作为渐变的模式
        val isDstGradientMode = useGradient && (mode == PorterDuff.Mode.SRC_IN || mode == PorterDuff.Mode.SRC_OUT)
        // 判断当前是否是需要 SRC 作为渐变的模式
        val isSrcGradientMode = useGradient && !isDstGradientMode
        
        val startColor = Color.parseColor("#4CAF50")
        val endColor = 0x004CAF50 

        // === 处理目标画笔 (DST) ===
        if (isDstGradientMode) {
            // 如果是 SRC_IN/SRC_OUT，渐变需要加在 DST 上
            val dstStartColor = Color.parseColor("#2196F3")
            val dstEndColor = 0x002196F3
            dstPaint.shader = LinearGradient(
                dstRect.left, dstRect.top, 
                dstRect.left, dstRect.bottom, // 垂直方向
                dstStartColor, dstEndColor,
                Shader.TileMode.CLAMP
            )
        } else {
            dstPaint.shader = null
            dstPaint.color = Color.parseColor("#2196F3")
        }

        // === 处理源画笔 (SRC) ===
        if (isSrcGradientMode) {
            // 如果是 DST_IN/DST_OUT/XOR，渐变加在 SRC 上
            srcPaint.shader = LinearGradient(
                srcRect.left, srcRect.top, 
                srcRect.left, srcRect.bottom, // 垂直方向
                startColor, endColor,
                Shader.TileMode.CLAMP
            )
            srcPaint.alpha = 255
        } else {
            srcPaint.shader = null
            srcPaint.color = Color.parseColor("#4CAF50")
            srcPaint.alpha = (srcAlpha * 255).toInt()
        }

        // 3. 先绘制目标图形 (Destination)
        canvas.drawRoundRect(dstRect, 36f, 36f, dstPaint)

        // 4. 为源画笔设置选定的 PorterDuffXfermode
        srcPaint.xfermode = PorterDuffXfermode(mode)
        
        // 5. 绘制源图形 (Source)
        if (useGradient) {
            // 渐变模式下画矩形遮罩
            canvas.drawRect(srcRect, srcPaint)
        } else {
            // 普通模式下画椭圆
            canvas.drawOval(srcRect, srcPaint)
        }
        
        // 6. 清理画笔的 Xfermode 状态
        srcPaint.xfermode = null
        
        // 7. 将离屏图层绘制的内容合成到主画布上
        canvas.restoreToCount(layer)

        // 8. 绘制辅助说明文字
        canvas.drawText("DST", w * 0.16f, h * 0.14f, labelPaint)
        canvas.drawText("SRC", w * 0.72f, h * 0.26f, labelPaint)
    }
}