package com.trade.zt_dialogfragmentwindow.demo

import com.trade.zt_dialogfragmentwindow.base.BaseBindingChildDialog
import com.trade.zt_dialogfragmentwindow.base.DialogHostManager
import com.trade.zt_dialogfragmentwindow.base.DialogSizeConfig
import com.trade.zt_dialogfragmentwindow.databinding.DialogTestABinding

/**
 * 测试弹框 A（绿色主题）
 * - 可从自身再打开 B，并指定 B 置顶 / 压底
 */
class TestDialogA(
    private val layerHint: String = "层级：当前为弹框 A"
) : BaseBindingChildDialog<DialogTestABinding>(DialogTestABinding::inflate) {

    /** 弹框 A：宽度为屏幕 90% */
    override val sizeConfig = DialogSizeConfig.widthRatio(0.9f)

    override fun onInitView() {
        binding.tvLayerHint.text = layerHint
        binding.btnOpenBTop.setOnClickListener {
            val fm = host?.parentFragmentManager ?: return@setOnClickListener
            DialogHostManager.show(
                fm,
                TestDialogB(layerHint = "层级：B 置顶（应盖住 A）"),
                pushToBottom = false
            )
        }
        binding.btnOpenBBottom.setOnClickListener {
            val fm = host?.parentFragmentManager ?: return@setOnClickListener
            DialogHostManager.show(
                fm,
                TestDialogB(layerHint = "层级：B 压底（应被 A 盖住）"),
                pushToBottom = true
            )
        }
        binding.btnClose.setOnClickListener { dismiss() }
    }
}
