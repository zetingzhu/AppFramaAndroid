package com.trade.zt_dialogfragmentwindow.base

import androidx.fragment.app.FragmentManager

/**
 * 弹框 Host 管理器：同一 [FragmentManager] 共用一个 [DialogHostFragment]，
 * 从而在同一窗口内处理多个子弹框的上下层级。
 */
object DialogHostManager {

    /**
     * @param fm FragmentManager（supportFragmentManager / childFragmentManager）
     * @param child 子弹框
     * @param pushToBottom true: 压到现有弹框下方；false: 置顶
     */
    fun show(
        fm: FragmentManager,
        child: IChildDialog,
        pushToBottom: Boolean = false
    ) {
        val host = getOrCreateHost(fm)
        host.showChild(child, pushToBottom)
    }

    fun dismissAll(fm: FragmentManager) {
        findHost(fm)?.dismiss()
    }

    private fun getOrCreateHost(fm: FragmentManager): DialogHostFragment {
        val tag = "DialogHost_${fm.hashCode()}"
        var host = fm.findFragmentByTag(tag) as? DialogHostFragment
        if (host == null || host.isDismissed) {
            host = DialogHostFragment()
            fm.beginTransaction()
                .add(host, tag)
                .commitNowAllowingStateLoss()
        }
        return host
    }

    private fun findHost(fm: FragmentManager): DialogHostFragment? {
        val tag = "DialogHost_${fm.hashCode()}"
        return fm.findFragmentByTag(tag) as? DialogHostFragment
    }
}
