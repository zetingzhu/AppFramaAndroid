package com.trade.zt_dialogfragmentwindow.base

import android.view.Gravity
import android.view.ViewGroup

/**
 * 单个子弹框的尺寸配置，由各弹框自行决定。
 *
 * @param widthRatio 内容宽度占屏幕宽度比例，`1f` 全屏，`0.9f` 为 90%；`null` 表示不改宽度（沿用 XML）
 * @param heightRatio 内容高度占屏幕高度比例；`null` 表示不改高度（通常 wrap_content）
 * @param gravity 内容在遮罩层中的位置，默认居中
 */
data class DialogSizeConfig(
    val widthRatio: Float? = 0.9f,
    val heightRatio: Float? = null,
    val gravity: Int = Gravity.CENTER
) {
    companion object {
        /** 宽度全屏 */
        fun fullWidth(gravity: Int = Gravity.CENTER) = DialogSizeConfig(
            widthRatio = 1f,
            gravity = gravity
        )

        /** 全屏宽 + 贴底（底部弹层） */
        fun bottomSheet() = DialogSizeConfig(
            widthRatio = 1f,
            gravity = Gravity.BOTTOM
        )

        /** 宽度为屏幕比例，默认 0.9 */
        fun widthRatio(ratio: Float, gravity: Int = Gravity.CENTER) = DialogSizeConfig(
            widthRatio = ratio.coerceIn(0.1f, 1f),
            gravity = gravity
        )

        /** 完全沿用布局 XML，不动态改尺寸 */
        fun fromXml() = DialogSizeConfig(
            widthRatio = null,
            heightRatio = null
        )
    }
}
