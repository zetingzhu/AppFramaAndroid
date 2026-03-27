package com.trade.zt_porterduffxfermode_sample.weiget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * @author: zeting
 * @date: 2026/3/16
 * 对内部视图顶部做一个透明渐变效果
 */
class TopTransparentFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    private var gradientShader: LinearGradient? = null

    // 渐变高度 (20dp)
    private val gradientHeight = 20 * context.resources.displayMetrics.density

    /**
     * 控制渐变是否开启
     */
    var isGradientEnabled: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                invalidate() // 属性改变时触发重绘
            }
        }

    init {
        // 开启硬件加速下的离屏缓冲
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 屏幕尺寸变化时重置 Shader
        gradientShader = LinearGradient(
            0f, 0f, 0f, gradientHeight,
            intArrayOf(0x00000000, 0xFF000000.toInt()), // 从全透明到全不透明
            null,
            Shader.TileMode.CLAMP
        )
    }

    override fun dispatchDraw(canvas: Canvas) {
        // 如果未开启渐变，直接执行原生绘制流程，不走离屏缓冲，节省性能
        if (!isGradientEnabled) {
            super.dispatchDraw(canvas)
            return
        }

        // 1. 开启离屏缓冲（保存图层）
        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // 2. 正常绘制子 View 内容
        super.dispatchDraw(canvas)

        // 3. 应用渐变遮罩
        maskPaint.xfermode = xfermode
        maskPaint.shader = gradientShader

        // 绘制顶部 20dp 的遮罩矩形
        canvas.drawRect(0f, 0f, width.toFloat(), gradientHeight, maskPaint)

        // 4. 清除状态并恢复图层
        maskPaint.xfermode = null
        maskPaint.shader = null
        canvas.restoreToCount(saveCount)
    }
}