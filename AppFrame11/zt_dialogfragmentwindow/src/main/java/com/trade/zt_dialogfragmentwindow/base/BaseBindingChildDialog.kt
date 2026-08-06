package com.trade.zt_dialogfragmentwindow.base

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.viewbinding.ViewBinding

/**
 * 基于 ViewBinding 的子弹框基类。
 *
 * 子类通过重写 [sizeConfig] 动态控制内容宽度，例如：
 * ```
 * override val sizeConfig = DialogSizeConfig.fullWidth()
 * override val sizeConfig = DialogSizeConfig.widthRatio(0.9f)
 * ```
 */
abstract class BaseBindingChildDialog<VB : ViewBinding>(
    private val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB
) : IChildDialog {

    protected var host: DialogHostFragment? = null
        private set

    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding
            ?: throw IllegalStateException("Binding is only valid between onCreateView and onDestroyView")

    private var _rootView: View? = null
    override val rootView: View?
        get() = _rootView

    /** 默认 0.9 屏宽；子类按需覆盖 */
    override val sizeConfig: DialogSizeConfig
        get() = DialogSizeConfig.widthRatio(0.9f)

    override fun attachHost(host: DialogHostFragment) {
        this.host = host
    }

    override fun onCreateView(context: Context, container: ViewGroup): View {
        val inflater = LayoutInflater.from(context)
        _binding = bindingInflater.invoke(inflater, container, false)
        _rootView = binding.root
        return binding.root
    }

    override fun onViewCreated(view: View) {
        applyDialogSize()
        onInitView()
    }

    /**
     * 按 [sizeConfig] 调整内容卡片宽高。
     * 遮罩层 root 保持 match_parent，只改内容区域。
     */
    protected fun applyDialogSize() {
        val content = dialogContentView() ?: return
        val config = sizeConfig
        val metrics = content.resources.displayMetrics
        val lp = content.layoutParams ?: FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        config.widthRatio?.let { ratio ->
            lp.width = (metrics.widthPixels * ratio.coerceIn(0.1f, 1f)).toInt()
        }
        config.heightRatio?.let { ratio ->
            lp.height = (metrics.heightPixels * ratio.coerceIn(0.1f, 1f)).toInt()
        }
        if (lp is FrameLayout.LayoutParams) {
            lp.gravity = config.gravity
        }
        content.layoutParams = lp
    }

    /** 子类重写此方法，直接使用 binding 操作控件 */
    abstract fun onInitView()

    override fun onDestroyView() {
        _binding = null
        _rootView = null
    }

    override fun dismiss() {
        host?.dismissChild(this)
    }
}
