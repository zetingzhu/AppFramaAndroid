package com.trade.zt_dialogfragmentwindow.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment

/**
 * 弹框宿主：一个窗口内可叠放多个 [IChildDialog]。
 * - pushToBottom = false：新弹框加在最上层
 * - pushToBottom = true：新弹框插入最底层（被已有弹框盖住）
 */
class DialogHostFragment : DialogFragment() {

    private var containerLayout: FrameLayout? = null
    private val pendingChildren = mutableListOf<Pair<IChildDialog, Boolean>>()
    private val activeChildren = mutableListOf<IChildDialog>()

    val isDismissed: Boolean
        get() = isRemoving || isDetached || isStateSaved

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val layout = FrameLayout(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        this.containerLayout = layout
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pendingChildren.forEach { (child, pushToBottom) ->
            realShowChild(child, pushToBottom)
        }
        pendingChildren.clear()
    }

    fun showChild(child: IChildDialog, pushToBottom: Boolean) {
        child.attachHost(this)
        val container = containerLayout
        if (container != null && isAdded) {
            realShowChild(child, pushToBottom)
        } else {
            pendingChildren.add(Pair(child, pushToBottom))
        }
    }

    private fun realShowChild(child: IChildDialog, pushToBottom: Boolean) {
        val container = containerLayout ?: return
        val childView = child.onCreateView(requireContext(), container)
        childView.isClickable = true

        if (pushToBottom) {
            container.addView(childView, 0)
            activeChildren.add(0, child)
        } else {
            container.addView(childView)
            activeChildren.add(child)
        }

        child.onViewCreated(childView)
    }

    fun dismissChild(child: IChildDialog) {
        val container = containerLayout
        if (container != null && activeChildren.contains(child)) {
            child.rootView?.let { container.removeView(it) }
            child.onDestroyView()
            activeChildren.remove(child)
        }

        if (activeChildren.isEmpty() && pendingChildren.isEmpty()) {
            dismissAllowingStateLoss()
        }
    }

    /** 当前活跃子弹框数量，便于测试断言 */
    fun getActiveChildCount(): Int = activeChildren.size
}
