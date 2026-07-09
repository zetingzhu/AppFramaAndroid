package com.trade.zt_sketchmcp

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * SubmitSuccessActivity
 * Displays the "Enviado con éxito" (Submit Successful) screen based on the Figma design.
 */
class SubmitSuccessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_submit_success)

        // Handle edge-to-edge system bar insets gracefully
        val root = findViewById<android.view.View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            // Let layout handle status bar styling, but we can consume insets
            insets
        }

        // Back button click action
        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }

        // Confirm button click action
        findViewById<FrameLayout>(R.id.btn_understood).setOnClickListener {
            Toast.makeText(this, "¡Entendido!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
