package com.trade.zt_blurviewsample.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout

/**
 * 仅能包含 **一个** 直接子 View；按住拖动时在 [dragBoundsView] 内平移，不超出上下左右边界。
 */
class BoundedDragFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    var dragBoundsView: View? = null
    var onDragMoved: (() -> Unit)? = null
    var onDragStarted: (() -> Unit)? = null
    var onDragEnded: (() -> Unit)? = null

    private var downRawX = 0f
    private var downRawY = 0f
    private var startTranslationX = 0f
    private var startTranslationY = 0f
    private var dragging = false
    private var dragGestureActive = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        if (childCount > 1) {
            removeView(child)
            error("BoundedDragFrameLayout 只能包含一个子 View")
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = ev.rawX
                downRawY = ev.rawY
                startTranslationX = translationX
                startTranslationY = translationY
                dragging = false
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = ev.rawX - downRawX
                val dy = ev.rawY - downRawY
                if (!dragging && (dx * dx + dy * dy) > touchSlop * touchSlop) {
                    dragging = true
                }
                if (dragging) {
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                startTranslationX = translationX
                startTranslationY = translationY
                dragging = true
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!dragGestureActive) {
                    dragGestureActive = true
                    onDragStarted?.invoke()
                }
                translationX = startTranslationX + (event.rawX - downRawX)
                translationY = startTranslationY + (event.rawY - downRawY)
                clampInsideBounds()
                onDragMoved?.invoke()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                clampInsideBounds()
                onDragMoved?.invoke()
                if (dragGestureActive) {
                    onDragEnded?.invoke()
                }
                dragGestureActive = false
                dragging = false
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun clampInsideBounds() {
        val bounds = dragBoundsView ?: (parent as? View) ?: return
        if (width == 0 || height == 0 || bounds.width == 0 || bounds.height == 0) {
            return
        }

        val padL = bounds.paddingLeft.toFloat()
        val padT = bounds.paddingTop.toFloat()
        val padR = bounds.paddingRight.toFloat()
        val padB = bounds.paddingBottom.toFloat()

        val left = this.left + translationX
        val top = this.top + translationY
        val maxLeft = bounds.width - padR - width
        val maxTop = bounds.height - padB - height

        val clampedLeft = left.coerceIn(padL, maxLeft.toFloat())
        val clampedTop = top.coerceIn(padT, maxTop.toFloat())
        translationX += clampedLeft - left
        translationY += clampedTop - top
    }
}