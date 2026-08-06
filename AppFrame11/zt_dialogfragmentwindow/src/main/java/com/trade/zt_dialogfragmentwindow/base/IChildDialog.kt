package com.trade.zt_dialogfragmentwindow.base

import android.content.Context
import android.view.View
import android.view.ViewGroup

/**
 * Host 内子弹框契约。
 * 多个 [IChildDialog] 共享同一个 [DialogHostFragment] 窗口，通过 View 层级控制上下关系。
 */
interface IChildDialog {

    /** 子弹框根 View，关闭时用于从 Host 容器移除 */
    val rootView: View?

    /**
     * 弹框内容尺寸配置。
     * Host 全屏，真正控制视觉宽度的是内容卡片；默认 0.9 屏宽。
     */
    val sizeConfig: DialogSizeConfig
        get() = DialogSizeConfig.widthRatio(0.9f)

    /**
     * 需要被 [sizeConfig] 作用的内容 View。
     * 默认约定：root 为全屏遮罩，第一个子 View 为内容卡片。
     */
    fun dialogContentView(): View? {
        val root = rootView ?: return null
        return if (root is ViewGroup && root.childCount > 0) {
            root.getChildAt(0)
        } else {
            root
        }
    }

    /** 绑定所属 Host */
    fun attachHost(host: DialogHostFragment)

    /** 创建子弹框 View */
    fun onCreateView(context: Context, container: ViewGroup): View

    /** View 已挂载到 Host 容器后回调 */
    fun onViewCreated(view: View)

    /** View 即将从 Host 容器移除时回调 */
    fun onDestroyView()

    /** 关闭自身（从 Host 中移除） */
    fun dismiss()
}
