package com.trade.zt_scroll_edit_sample

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class ScrollEditActivity : AppCompatActivity(), ScrollEditSubmitCallback {

    private val keyboardUtils by lazy { KeyboardUtilV2.getInstance() }

    private var rootLayout: View? = null
    private var viewPager: ViewPager2? = null
    private lateinit var pagerAdapter: ScrollEditPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_scroll_edit)

        rootLayout = findViewById(R.id.root_layout)
        viewPager = findViewById(R.id.view_pager)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)

        pagerAdapter = ScrollEditPagerAdapter(this)
        viewPager?.adapter = pagerAdapter
        viewPager?.offscreenPageLimit = pagerAdapter.itemCount

        TabLayoutMediator(tabLayout, viewPager!!) { tab, position ->
            tab.text = when (position) {
                ScrollEditFormFragment.PAGE_BASIC -> getString(R.string.scroll_edit_tab_basic)
                ScrollEditFormFragment.PAGE_EXTRA -> getString(R.string.scroll_edit_tab_extra)
                else -> ""
            }
        }.attach()

        setupActivityInsets()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        keyboardUtils.handleTouchToHideKeyboard(this, ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun setupActivityInsets() {
        val root = rootLayout ?: return
        root.post {
            keyboardUtils.observeWindowInsets(
                root,
                null,
                object : KeyboardUtilV2.OnWindowInsetsListener {
                    override fun onInsetsChanged(state: KeyboardUtilV2.WindowInsetsState) {
                        // Activity 层只处理 systemBars，底部键盘由 Fragment 各自处理
                        root.setPadding(
                            state.systemBarsLeft,
                            state.systemBarsTop,
                            state.systemBarsRight,
                            state.systemBarsBottom
                        )
                    }
                })
        }
    }

    override fun onDestroy() {
        rootLayout?.let { keyboardUtils.removeWindowInsetsObserver(it) }
        super.onDestroy()
    }

    override fun onSubmitRequested() {
        submitForm()
    }

    private fun submitForm() {
        for (position in 0 until pagerAdapter.itemCount) {
            val fragment = findFormFragment(position) ?: continue
            if (!fragment.validateRequiredFields()) {
                viewPager?.currentItem = position
                return
            }
        }

        val message = buildString {
            for (position in 0 until pagerAdapter.itemCount) {
                val fragment = findFormFragment(position) ?: continue
                fragment.collectSummaryLines().forEach { line ->
                    append(line)
                    append('\n')
                }
            }
        }.trim()

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun findFormFragment(position: Int): ScrollEditFormFragment? {
        val pager = viewPager ?: return null
        val tag = "f${pager.id}$position"
        return supportFragmentManager.findFragmentByTag(tag) as? ScrollEditFormFragment
    }
}
