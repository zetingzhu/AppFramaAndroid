package com.trade.zt_kotlinmcp.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * 右侧字母 A-Z 及 # 的自绘索引条
 * 用于跟城市列表 RecyclerView 联动，实现点击、滑动查阅、首字母高亮。
 */
class AlphabetIndexView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 绘制 A-Z 和 #
    private var alphabet: Array<String> = Array(26) { (it + 65).toChar().toString() } + arrayOf("#")
    
    // 是否响应点击与滑动事件 (用以处理无字母排序语言时禁用)
    private var isInteractive = true
    
    // 当前选中的字母下标（初始化默认高亮顶部的 '#', 如果是从A开始可以改成0）
    private var selectedIndex = -1

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }

    private val bounds = Rect()

    // 点击字母的监听器 (将触发 RecyclerView 滑动)
    var onIndexChangeListener: ((String) -> Unit)? = null

    /**
     * 针对无字母排序语言的降级处理接口
     */
    fun setData(letters: List<String>) {
        if (letters.isEmpty()) {
            alphabet = arrayOf("#")
            isInteractive = false
        } else {
            alphabet = letters.toTypedArray()
            isInteractive = true
        }
        selectedIndex = if (isInteractive) -1 else 0
        invalidate()
    }

    /**
     * 由外部（如 RecyclerView 的 OnScrollListener）调用，通知当前高亮的字母变更
     */
    fun setSelectedIndex(index: Int) {
        if (selectedIndex != index && index in alphabet.indices) {
            selectedIndex = index
            invalidate()
        }
    }

    fun setSelectedIndexByLetter(letter: String) {
        val index = alphabet.indexOf(letter)
        if (index != -1) {
            setSelectedIndex(index)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val singleHeight = height.toFloat() / alphabet.size
        
        for (i in alphabet.indices) {
            // "选中字母高亮且呈蓝色，其他置暗"
            if (i == selectedIndex) {
                paint.color = Color.parseColor("#3B5998") // 高亮蓝
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            } else {
                paint.color = Color.parseColor("#888888") // 暗灰色
                paint.typeface = android.graphics.Typeface.DEFAULT
            }
            
            val xPos = width / 2f
            paint.getTextBounds(alphabet[i], 0, alphabet[i].length, bounds)
            val yPos = singleHeight * i + singleHeight / 2 + bounds.height() / 2

            canvas.drawText(alphabet[i], xPos, yPos, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isInteractive) return false

        return when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val y = event.y
                val oldIndex = selectedIndex
                var newIndex = (y / height * alphabet.size).toInt()
                
                if (newIndex < 0) newIndex = 0
                if (newIndex >= alphabet.size) newIndex = alphabet.size - 1

                if (oldIndex != newIndex) {
                    selectedIndex = newIndex
                    onIndexChangeListener?.invoke(alphabet[newIndex])
                    invalidate()
                }
                true
            }
            MotionEvent.ACTION_UP -> {
                // 如果需要的话，可以在手指抬起时移除一些额外视觉反馈，但选中状态仍然保留
                true
            }
            else -> super.onTouchEvent(event)
        }
    }
}
