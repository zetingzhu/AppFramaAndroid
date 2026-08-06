package com.trade.zt_dialogfragmentwindow.demo

import com.trade.zt_dialogfragmentwindow.base.BaseBindingChildDialog
import com.trade.zt_dialogfragmentwindow.base.DialogHostManager
import com.trade.zt_dialogfragmentwindow.base.DialogSizeConfig
import com.trade.zt_dialogfragmentwindow.databinding.DialogTestBBinding

/**
 * 测试弹框 B（橙色主题）
 * - 可再打开 A 置顶，验证多处逻辑连续叠层
 */
class TestDialogB(
    private val layerHint: String = "层级：当前为弹框 B"
) : BaseBindingChildDialog<DialogTestBBinding>(DialogTestBBinding::inflate) {

    /** 弹框 B：宽度全屏，对比 A 的 0.9 */
    override val sizeConfig = DialogSizeConfig.fullWidth()

    override fun onInitView() {
        binding.tvLayerHint.text = layerHint
        binding.btnOpenATop.setOnClickListener {
            val fm = host?.parentFragmentManager ?: return@setOnClickListener
            DialogHostManager.show(
                fm,
                TestDialogA(layerHint = "层级：A 再次置顶（应盖住 B）"),
                pushToBottom = false
            )
        }
        binding.btnClose.setOnClickListener { dismiss() }
    }
}
