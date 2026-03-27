package com.trade.zt_porterduffxfermode_sample.weiget


import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * @author: zeting
 * @date: 2025/10/31
 * 将视图均分3分，中间高亮，上下带个阴影
 */
class RoomThreePartOverlayView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 1. 画笔和颜色定义
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 2. 掏空模式 (PorterDuff.Mode.CLEAR 用于清除像素)
    private val xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

    // 3. 视图的分割线坐标
    private val highlightRect = RectF()
    private var segmentHeight = 0f

    init {
        // 配置半透明遮罩画笔: 50% 透明的黑色 (#80000000)
        overlayPaint.color = Color.parseColor("#80000000")
    }

    // 在尺寸变化时计算 1/3 的高度和中间掏空区域
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // 计算 1/3 的高度
        segmentHeight = h / 3f

        // 计算中间掏空（高亮）区域的矩形
        highlightRect.set(
                0f,                   // 左 (Left)
                segmentHeight,        // 顶 (Top) = 1/3 处
                w.toFloat(),          // 右 (Right)
                segmentHeight * 2     // 底 (Bottom) = 2/3 处
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. 创建一个新的图层来绘制 (这是使用 PorterDuffXfermode 的关键)
        val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // 2. 绘制整个半透明遮罩层 (作为底色)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // 3. 设置 PorterDuffXfermode: 使用 CLEAR 模式在新的图层上挖空一个矩形
        overlayPaint.xfermode = xfermode

        // 4. 绘制掏空区域 (覆盖中间 1/3 区域)
        canvas.drawRect(highlightRect, overlayPaint)

        // 5. 恢复画笔，并合并图层
        overlayPaint.xfermode = null
        canvas.restoreToCount(layerId)
    }
}