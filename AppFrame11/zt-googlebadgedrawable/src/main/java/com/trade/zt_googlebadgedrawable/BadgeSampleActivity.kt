package com.trade.zt_googlebadgedrawable

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.trade.zt_googlebadgedrawable.databinding.ActivityBadgeSampleBinding

/**
 * Sample activity demonstrating various ways to use BadgeDrawable.
 */
class BadgeSampleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBadgeSampleBinding
    private var badgeCount = 0

    @com.google.android.material.badge.ExperimentalBadgeUtils
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBadgeSampleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle window insets to prevent UI from being under the status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density
            val padding = (16 * density).toInt()
            v.setPadding(
                systemBars.left + padding,
                systemBars.top + padding,
                systemBars.right + padding,
                systemBars.bottom + padding
            )
            insets
        }

        setupArbitraryViewBadge()
        setupTabLayoutBadge()
        setupBottomNavBadge()
    }

    /**
     * Usage 1: Attaching BadgeDrawable to an arbitrary view using BadgeUtils.
     */
    @com.google.android.material.badge.ExperimentalBadgeUtils
    private fun setupArbitraryViewBadge() {
        val badgeDrawable = BadgeDrawable.create(this).apply {
            number = badgeCount
            isVisible = true
            // Customization
            backgroundColor = getColor(android.R.color.holo_red_dark)
            badgeTextColor = getColor(android.R.color.white)

            // horizontalOffset: 负值向右移出边界 (对于 TOP_END)
            horizontalOffset = 0
            // verticalOffset: 负值向上移出边界 (对于 TOP_END)
            verticalOffset = 0
        }

        // Must be called after the anchor view is laid out or inside post()
        binding.badgeAnchorContainer.post {
            BadgeUtils.attachBadgeDrawable(
                badgeDrawable,
                binding.badgeAnchorContainer, null
            )
        }
        val badgeDrawable2 = BadgeDrawable.create(this).apply {
            number = badgeCount
            isVisible = true
            // Customization
            backgroundColor = getColor(android.R.color.holo_blue_dark)
            badgeTextColor = getColor(android.R.color.white)

            val density = resources.displayMetrics.density
            // horizontalOffset: 负值向右移出边界 (对于 TOP_END)
            horizontalOffset = (10 * density).toInt()
            // verticalOffset: 负值向上移出边界 (对于 TOP_END)
            verticalOffset = (10 * density).toInt()
        }
        binding.badgeAnchorContainer2.post {
            BadgeUtils.attachBadgeDrawable(
                badgeDrawable2,
                binding.badgeAnchorContainer2, null
            )
        }
        val badgeDrawable3 = BadgeDrawable.create(this).apply {
            number = badgeCount
            isVisible = true
            // Customization
            backgroundColor = getColor(android.R.color.holo_purple)
            badgeTextColor = getColor(android.R.color.white)

            val density = resources.displayMetrics.density
            // horizontalOffset: 负值向右移出边界 (对于 TOP_END)
            horizontalOffset = (-10 * density).toInt()
            // verticalOffset: 负值向上移出边界 (对于 TOP_END)
            verticalOffset = (-10 * density).toInt()
        }
        binding.badgeAnchorContainer3.post {
            BadgeUtils.attachBadgeDrawable(
                badgeDrawable3,
                binding.badgeAnchorContainer3, null
            )
        }
        binding.btnUpdateBadge.setOnClickListener {
            badgeCount++
            badgeDrawable.number = badgeCount
        }
    }

    /**
     * Usage 2: Attaching BadgeDrawable to TabLayout tabs.
     */
    private fun setupTabLayoutBadge() {
        // Get or create badge for the first tab
        val tab = binding.tabLayout.getTabAt(0)
        tab?.let {
            val badge = it.getOrCreateBadge()
            badge.number = 5
            badge.isVisible = true
        }

        // Dot badge (no number) for the second tab
        binding.tabLayout.getTabAt(1)?.getOrCreateBadge()?.apply {
            isVisible = true
        }

        // Custom badge for the third tab
        binding.tabLayout.getTabAt(2)?.getOrCreateBadge()?.apply {
            text = "NEW" // M3 feature
            isVisible = true
        }
    }

    /**
     * Usage 3: Attaching BadgeDrawable to BottomNavigationView menu items.
     */
    private fun setupBottomNavBadge() {
        // Notification count on "Alerts" item
        val navBadge = binding.bottomNavigation.getOrCreateBadge(R.id.nav_notifications)
        navBadge.number = 10
        navBadge.isVisible = true

        // Dot badge on "Profile" item
        binding.bottomNavigation.getOrCreateBadge(R.id.nav_profile).apply {
            isVisible = true
            backgroundColor = getColor(android.R.color.holo_blue_light)
        }
    }
}