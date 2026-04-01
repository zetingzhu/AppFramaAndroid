package com.trade.zt_kotlinmcp.ui

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.trade.zt_kotlinmcp.rvweigit.RVStickyHeaderAdapter

class CityAdapter(private var items: List<CityItem>, private val onCityClicked: (String) -> Unit) :
    RVStickyHeaderAdapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CONTENT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is CityItem.Header -> TYPE_HEADER
            is CityItem.Content -> TYPE_CONTENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // 关键修复：RVStickyHeaderLayout 调用 onCreateViewHolder 时，parent 是其内部 FrameLayout，
        // 而非 RecyclerView。检测到这种情况时，无论 viewType 是什么，都创建 HeaderViewHolder，
        // 确保悬停头部始终使用 Header 样式的视图。
        val isStickyContext = parent !is RecyclerView

        return if (isStickyContext || viewType == TYPE_HEADER) {
            val view = TextView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(48, 24, 48, 24)
                setTextColor(Color.parseColor("#3B5998"))
                textSize = 14f
                setBackgroundColor(Color.parseColor("#F5F5F5"))
            }
            HeaderViewHolder(view)
        } else {
            val layout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    120
                )
                setPadding(48, 0, 48, 0)
                gravity = android.view.Gravity.CENTER_VERTICAL
                setBackgroundResource(android.R.drawable.list_selector_background)
            }
            val iconView = android.widget.ImageView(parent.context).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(64, 64).apply {
                    marginEnd = 24
                }
                visibility = View.GONE
            }
            val textView = TextView(parent.context).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                )
                setTextColor(Color.parseColor("#333333"))
                textSize = 16f
            }
            layout.addView(iconView)
            layout.addView(textView)
            ContentViewHolder(layout, iconView, textView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is HeaderViewHolder) {
            holder.textView.text = item.text
        } else if (holder is ContentViewHolder && item is CityItem.Content) {
            holder.textView.text = item.text
            if (item.iconRes != null) {
                holder.iconView.setImageResource(item.iconRes)
                holder.iconView.visibility = View.VISIBLE
            } else {
                holder.iconView.visibility = View.GONE
            }
            holder.itemView.setOnClickListener {
                onCityClicked(item.text)
            }
        }
    }

    // ============= RVStickyHeaderAdapter 实现 =============

    /**
     * 绑定悬停 Header 的数据。
     * RVStickyHeaderLayout 现在传入的 headPos 已经是分组 Header 的 position，
     * 直接取该位置的 Header 文字即可。
     */
    override fun onBindViewHolderHead(holder: RecyclerView.ViewHolder, headPos: Int, showHead: Boolean) {
        if (holder is HeaderViewHolder && headPos in items.indices) {
            val item = items[headPos]
            if (item is CityItem.Header) {
                holder.textView.text = item.text
            }
        }
    }

    /**
     * 判断当前 position 是否需要显示悬停 Header。
     * 只要当前位置有效且存在所属分组的 Header，就显示悬停。
     */
    override fun isStickyHeaderBoo(pos: Int): Boolean {
        if (pos < 0 || pos >= items.size) return false
        return findNearestHeaderPosition(pos) != -1
    }

    /**
     * 返回 pos 所属分组的 Header 的 adapter position。
     * 同一分组内的所有 item 返回同一个值，使 RVStickyHeaderLayout
     * 在同一分组内滚动时不会频繁重建悬停布局。
     */
    override fun getStickyHeaderPosition(pos: Int): Int {
        return findNearestHeaderPosition(pos)
    }

    /**
     * 返回 currentHeaderPos 之后的下一个分组 Header 的 adapter position。
     * 用于悬停头部的推挤偏移计算——只在真正的下一组 Header 接近时才产生推挤动画。
     */
    override fun getNextStickyHeaderPosition(currentHeaderPos: Int): Int {
        for (i in (currentHeaderPos + 1) until items.size) {
            if (items[i] is CityItem.Header) {
                return i
            }
        }
        return -1
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<CityItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    fun getPositionForSection(letter: String): Int {
        return items.indexOfFirst { it is CityItem.Header && it.text == letter }
    }

    fun getSectionForPosition(position: Int): String? {
        if (position < 0 || position >= items.size) return null
        val headerPos = findNearestHeaderPosition(position)
        return if (headerPos != -1) items[headerPos].text else null
    }

    /**
     * 向上回溯找到最近的 Header 的 adapter position
     */
    private fun findNearestHeaderPosition(position: Int): Int {
        if (position < 0 || position >= items.size) return -1
        for (i in position downTo 0) {
            if (items[i] is CityItem.Header) {
                return i
            }
        }
        return -1
    }

    class HeaderViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
    class ContentViewHolder(view: View, val iconView: android.widget.ImageView, val textView: TextView) : RecyclerView.ViewHolder(view)
}
