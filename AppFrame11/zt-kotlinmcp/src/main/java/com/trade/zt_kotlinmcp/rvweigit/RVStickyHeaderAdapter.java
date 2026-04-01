package com.trade.zt_kotlinmcp.rvweigit;

import androidx.recyclerview.widget.RecyclerView;

/**
 * @author: zeting
 * @date: 2024/1/26
 * RecycleView 固定吸顶适配器
 */
public abstract class RVStickyHeaderAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    /**
     * 显示滚动头部信息
     *
     * @param holder
     * @param headPos
     * @param showHead
     */
    public abstract void onBindViewHolderHead(VH holder, int headPos, boolean showHead);

    /**
     * 判断当前头部id是否固定
     *
     * @param pos
     * @return
     */
    public abstract boolean isStickyHeaderBoo(int pos);

    /**
     * 获取当前 position 所属分组的 Header 位置。
     * 子类必须覆写此方法，返回向上回溯到的最近 Header 的 adapter position。
     * 这样同一分组内的所有 item 都映射到同一个 Header position，
     * 避免 RVStickyHeaderLayout 在每个 item 滚过时都重建悬停布局。
     *
     * @param pos 当前 firstVisibleItem 的 adapter position
     * @return 该 position 所属分组的 Header position，找不到返回 -1
     */
    public int getStickyHeaderPosition(int pos) {
        return pos;
    }

    /**
     * 获取当前分组之后的下一个分组 Header 位置。
     * 用于 calculateOffset 计算推挤偏移量，只在真正的下一组 Header 接近时才产生推挤效果。
     *
     * @param currentHeaderPos 当前吸顶的 Header position
     * @return 下一个 Header 的 adapter position，找不到返回 -1
     */
    public int getNextStickyHeaderPosition(int currentHeaderPos) {
        return currentHeaderPos + 1;
    }
}
