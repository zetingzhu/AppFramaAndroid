package com.trade.zt_sketchmcp

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.TranslateAnimation
import androidx.fragment.app.DialogFragment
import com.trade.zt_sketchmcp.databinding.DialogDisclosureBinding

/**
 * Disclosure Statement Bottom Sheet Dialog
 *
 * A full-screen dialog that displays a disclosure statement with:
 * - Gradient header with logo
 * - Scrollable disclosure text content
 * - Checkbox agreement confirmation
 * - Reject and Accept action buttons
 *
 * Based on Figma design: 西班牙金融项目-web, Node: 11987:4735
 */
class DisclosureDialogFragment : DialogFragment() {

    private var _binding: DialogDisclosureBinding? = null
    private val binding get() = _binding!!

    private var isChecked = false

    /** Callback for when the user accepts the disclosure */
    var onAcceptListener: (() -> Unit)? = null

    /** Callback for when the user rejects the disclosure */
    var onRejectListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Translucent_NoTitleBar)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // Make the dialog full-width at the bottom
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setGravity(Gravity.BOTTOM)
            // Dim the background
            setDimAmount(0f) // We handle our own overlay in the layout
        }
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDisclosureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCheckbox()
        setupButtons()
        setupOverlayDismiss()
        animateEntrance()
    }

    /**
     * Setup the circular checkbox toggle behavior.
     * Switches between unchecked (white circle) and checked (green circle) states.
     */
    private fun setupCheckbox() {
        binding.checkboxCircle.setOnClickListener {
            isChecked = !isChecked
            updateCheckboxState()
        }

        // Also make the agreement text clickable to toggle checkbox
        binding.tvAgreementText.setOnClickListener {
            isChecked = !isChecked
            updateCheckboxState()
        }
    }

    /**
     * Update the checkbox visual state and the accept button enabled state.
     */
    private fun updateCheckboxState() {
        if (isChecked) {
            binding.checkboxCircle.setBackgroundResource(R.drawable.bg_checkbox_circle_checked)
        } else {
            binding.checkboxCircle.setBackgroundResource(R.drawable.bg_checkbox_circle)
        }
        // Optionally update accept button state based on checkbox
        updateAcceptButtonState()
    }

    /**
     * Update the accept button appearance based on whether the checkbox is checked.
     */
    private fun updateAcceptButtonState() {
        binding.btnAccept.alpha = if (isChecked) 1.0f else 0.5f
        binding.btnAccept.isEnabled = isChecked
    }

    /**
     * Setup reject and accept button click handlers.
     */
    private fun setupButtons() {
        // Initially disable accept button until checkbox is checked
        updateAcceptButtonState()

        binding.btnReject.setOnClickListener {
            onRejectListener?.invoke()
            animateExit {
                dismissAllowingStateLoss()
            }
        }

        binding.btnAccept.setOnClickListener {
            if (isChecked) {
                onAcceptListener?.invoke()
                animateExit {
                    dismissAllowingStateLoss()
                }
            }
        }
    }

    /**
     * Dismiss dialog when tapping the dark overlay area above the bottom sheet.
     */
    private fun setupOverlayDismiss() {
        binding.dialogRoot.setOnClickListener {
            animateExit {
                dismissAllowingStateLoss()
            }
        }

        // Prevent clicks on the bottom sheet from dismissing
        binding.bottomSheetContainer.setOnClickListener {
            // Consume click - do nothing
        }
    }

    /**
     * Animate the bottom sheet sliding up from the bottom.
     */
    private fun animateEntrance() {
        binding.bottomSheetContainer.post {
            val slideUp = TranslateAnimation(
                0f, 0f,
                binding.bottomSheetContainer.height.toFloat(), 0f
            ).apply {
                duration = 350
                interpolator = AccelerateDecelerateInterpolator()
                fillAfter = true
            }
            binding.bottomSheetContainer.startAnimation(slideUp)
        }
    }

    /**
     * Animate the bottom sheet sliding down before dismissing.
     */
    private fun animateExit(onComplete: () -> Unit) {
        val slideDown = TranslateAnimation(
            0f, 0f,
            0f, binding.bottomSheetContainer.height.toFloat()
        ).apply {
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
            fillAfter = true
        }
        binding.bottomSheetContainer.startAnimation(slideDown)
        binding.bottomSheetContainer.postDelayed({
            onComplete()
        }, 260)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DisclosureDialogFragment"

        /**
         * Create a new instance of the disclosure dialog.
         */
        fun newInstance(): DisclosureDialogFragment {
            return DisclosureDialogFragment()
        }
    }
}
