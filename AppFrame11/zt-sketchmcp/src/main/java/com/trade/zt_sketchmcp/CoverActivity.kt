package com.trade.zt_sketchmcp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.trade.zt_sketchmcp.databinding.ActivityCoverBinding
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Cover Activity
 * Displays the Android UI Kit cover page based on the Figma design.
 * Figma source: Android UI Kit (Community) - Node 1:9 (Cover)
 */
class CoverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCoverBinding
    private val viewModel: CoverViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityCoverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
    }

    private fun observeViewModel() {
        // Safe observation with repeatOnLifecycle
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bankCardState.collect { state ->
                    when (state) {
                        is NetworkState.Idle -> {
                            // Initial state, do nothing or trigger fetch
                            Log.d("CoverActivity", "Idle state. Attempting to fetch.")
                            viewModel.fetchBankCardDetail(12345) // Example ID
                        }
                        is NetworkState.Loading -> {
                            // Show loading indicator
                            Log.d("CoverActivity", "Loading...")
                        }
                        is NetworkState.Success -> {
                            // Update UI with data
                            Log.d("CoverActivity", "Success: ${state.data}")
                        }
                        is NetworkState.Error -> {
                            // Show error message
                            Log.e("CoverActivity", "Error: ${state.exception.message}")
                        }
                    }
                }
            }
        }
    }
}
