package com.trade.zt_scroll_edit_sample

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ScrollEditPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = PAGE_COUNT

    override fun createFragment(position: Int): Fragment {
        return ScrollEditFormFragment.newInstance(position)
    }

    companion object {
        const val PAGE_COUNT = 2
    }
}
