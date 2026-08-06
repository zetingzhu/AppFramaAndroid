package com.trade.zt_dialogfragmentwindow.demo

import com.trade.zt_dialogfragmentwindow.base.BaseBindingChildDialog
import com.trade.zt_dialogfragmentwindow.base.DialogHostManager
import com.trade.zt_dialogfragmentwindow.base.DialogSizeConfig
import com.trade.zt_dialogfragmentwindow.databinding.DialogTestCBinding

/**
 * 测试弹框 C：全屏宽 + 贴底弹出（底部弹层）
 */
class TestDialogC(
    private val layerHint: String = "层级：底部弹层 C"
) : BaseBindingChildDialog<DialogTestCBinding>(DialogTestCBinding::inflate) {

    override val sizeConfig = DialogSizeConfig.bottomSheet()

    override fun onInitView() {
        binding.tvLayerHint.text = layerHint
        binding.btnOpenA.setOnClickListener {
            val fm = host?.parentFragmentManager ?: return@setOnClickListener
            DialogHostManager.show(
                fm,
                TestDialogA(layerHint = "层级：从底部弹层 C 打开的 A"),
                pushToBottom = false
            )
        }
        binding.btnClose.setOnClickListener { dismiss() }
    }
}
