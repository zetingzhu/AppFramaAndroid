package com.trade.zt_kotlinmcp.ui

/**
 * 城市列表数据模型区区分
 */
sealed class CityItem {
    abstract val text: String

    data class Header(override val text: String) : CityItem()
    data class Content(override val text: String, val id: String, val iconRes: Int? = null) : CityItem()
}
