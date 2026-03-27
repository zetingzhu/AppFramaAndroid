package com.trade.zt_porterduffxfermode_sample.weiget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.FrameLayout
import com.trade.zt_porterduffxfermode_sample.R

/**
 * @author: zeting
 * @date: 2025/12/17
 * 支持不同的角度擦除
 */
class RoomCutLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val maskPath = Path()

    // 自定义切角的大小（单位：px）
    var cutSize = 10f
        set(value) {
            field = value
            invalidate()
        }

    // 擦除类型枚举
    enum class CutType {
        TOP_LEFT,       // 仅擦除左上角
        BOTTOM_RIGHT,   // 仅擦除右下角
        BOTH,           // 同时擦除左上角和右下角
        NONE            // 啥都不裁剪
    }

    // 当前擦除类型
    var cutType = CutType.NONE
        get() = field
        set(value) {
            field = value
            invalidate()
        }

    init {
        // 解析自定义属性
        attrs?.let { attrSet ->
            val typedArray = context.obtainStyledAttributes(attrSet, R.styleable.RoomCutLayout)
            try {
                // 读取切角大小
                cutSize = typedArray.getDimension(R.styleable.RoomCutLayout_cutSize, cutSize)

                // 读取擦除类型
                val cutTypeIndex = typedArray.getInt(R.styleable.RoomCutLayout_cutType, -1)
                if (cutTypeIndex != -1) {
                    cutType = when (cutTypeIndex) {
                        0 -> CutType.NONE
                        1 -> CutType.TOP_LEFT
                        2 -> CutType.BOTTOM_RIGHT
                        3 -> CutType.BOTH
                        else -> CutType.NONE
                    }
                }
            } finally {
                typedArray.recycle()
            }
        }

        // 关键：为了支持透明合成，建议关闭硬件加速或使用 Layer
        setLayerType(LAYER_TYPE_HARDWARE, null)

        // 设置混合模式：目标图像（背景）中重叠的部分会被擦除
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }

    override fun dispatchDraw(canvas: Canvas) {
        // 创建一个图层，以便 PorterDuff 模式只作用于当前 View 内容，而不影响 Activity 背景
        val saveLayer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // 1. 绘制正常的背景和子 View
        super.dispatchDraw(canvas)

        // 2. 根据选择的擦除类型绘制相应的切角
        when (cutType) {
            CutType.TOP_LEFT -> drawTopLeftCut(canvas)
            CutType.BOTTOM_RIGHT -> drawBottomRightCut(canvas)
            CutType.BOTH -> {
                drawTopLeftCut(canvas)
                drawBottomRightCut(canvas)
            }
            else -> {

            }
        }

        // 恢复图层
        canvas.restoreToCount(saveLayer)
    }

    /**
     * 绘制左上角切角
     */
    private fun drawTopLeftCut(canvas: Canvas) {
        maskPath.reset()
        maskPath.moveTo(0f, 0f) // 左上角
        maskPath.lineTo(cutSize, 0f) // 顶部右侧
        maskPath.lineTo(0f, height.toFloat()) // 左侧下方
        maskPath.close()
        canvas.drawPath(maskPath, maskPaint)
    }

    /**
     * 绘制右下角切角
     */
    private fun drawBottomRightCut(canvas: Canvas) {
        maskPath.reset()
        maskPath.moveTo(width.toFloat(), height.toFloat()) // 右下角
        maskPath.lineTo(width.toFloat() - cutSize, height.toFloat()) // 底部左侧
        maskPath.lineTo(width.toFloat(), 0f) // 右侧上方
        maskPath.close()
        canvas.drawPath(maskPath, maskPaint)
    }
}