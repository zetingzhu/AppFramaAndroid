package com.trade.zt_blurviewsample.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.trade.zt_blurviewsample.R
import com.trade.zt_blurviewsample.view.BoundedDragFrameLayout
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.BlurViewFacade
import kotlin.math.roundToInt

class BlurViewDimezisActivity : AppCompatActivity() {

    companion object {
        private const val BLUR_RADIUS_MIN = 4f
        private const val BLUR_RADIUS_DEFAULT = 20f
        private const val SEEK_MAX_PROGRESS = 21

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, BlurViewDimezisActivity::class.java))
        }
    }

    private lateinit var blurFacade: BlurViewFacade

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_blur_view_dimezis)

        val main = findViewById<FrameLayout>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val blurTarget = findViewById<BlurTarget>(R.id.blurTarget)
        val blurView = findViewById<BlurView>(R.id.blurView)
        val seekBar = findViewById<SeekBar>(R.id.blurRadiusSeekBar)
        val radiusLabel = findViewById<TextView>(R.id.blurRadiusLabel)

        val initialRadius = BLUR_RADIUS_DEFAULT
        seekBar.max = SEEK_MAX_PROGRESS
        seekBar.progress = radiusToProgress(initialRadius)
        updateRadiusLabel(radiusLabel, initialRadius)

        val windowBackground = window.decorView.background

        blurView.post {
            blurFacade = blurView.setupWith(blurTarget)
                .setFrameClearDrawable(windowBackground)
                .setBlurRadius(initialRadius)
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar?, progress: Int, fromUser: Boolean) {
                val radius = progressToRadius(progress)
                updateRadiusLabel(radiusLabel, radius)
                if (fromUser && ::blurFacade.isInitialized) {
                    blurFacade.setBlurRadius(radius)
                }
            }

            override fun onStartTrackingTouch(seek: SeekBar?) = Unit

            override fun onStopTrackingTouch(seek: SeekBar?) {
                val radius = progressToRadius(seek?.progress ?: 0)
                if (::blurFacade.isInitialized) {
                    blurFacade.setBlurRadius(radius)
                }
            }
        })

        val dragHost = findViewById<BoundedDragFrameLayout>(R.id.frostDragHost)
        dragHost.dragBoundsView = main
    }

    private fun progressToRadius(progress: Int): Float {
        return BLUR_RADIUS_MIN + progress
    }

    private fun radiusToProgress(radius: Float): Int {
        return (radius - BLUR_RADIUS_MIN).roundToInt()
            .coerceIn(0, SEEK_MAX_PROGRESS)
    }

    private fun updateRadiusLabel(label: TextView, radius: Float) {
        label.text = getString(R.string.blur_radius_label, radius.roundToInt())
    }
}