package com.trade.appframe11.audio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.trade.appframe11.R

/**
 * 表现层 —— 字幕叠加视图。
 *
 * 悬浮在播放器上方的半透明条，用于显示 AI 推理结果。
 * 默认定位在父容器底部，支持淡入淡出动画。
 */
class SubtitleOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val subtitleTextView: TextView

    init {
        // 半透明深色背景条
        setBackgroundColor(Color.parseColor("#AA000000"))
        val hPadding = dp(16)
        val vPadding = dp(10)
        setPadding(hPadding, vPadding, hPadding, vPadding)

        subtitleTextView = TextView(context).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            maxLines = 3
            text = ""
        }

        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        }
        addView(subtitleTextView, lp)

        // 初始不可见
        alpha = 0f
        visibility = GONE
    }

    /**
     * 更新字幕文字。
     * 非空时显示（淡入），空/null 时隐藏（淡出）。
     */
    fun updateSubtitle(text: String?) {
        if (text.isNullOrBlank()) {
            fadeOut()
            return
        }
        subtitleTextView.text = text
        fadeIn()
    }

    private fun fadeIn() {
        if (visibility == VISIBLE && alpha == 1f) {
            // 仅更新文字，无需再次动画
            return
        }
        // 取消可能正在进行的动画，防止 fadeIn/fadeOut 冲突
        animate().cancel()
        visibility = VISIBLE
        animate()
            .alpha(1f)
            .setDuration(ANIM_DURATION)
            .setListener(null)
            .start()
    }

    private fun fadeOut() {
        if (visibility == GONE) return
        // 取消可能正在进行的动画，防止 fadeIn/fadeOut 冲突
        animate().cancel()
        animate()
            .alpha(0f)
            .setDuration(ANIM_DURATION)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = GONE
                }
            })
            .start()
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()

    companion object {
        private const val ANIM_DURATION = 250L
    }
}
