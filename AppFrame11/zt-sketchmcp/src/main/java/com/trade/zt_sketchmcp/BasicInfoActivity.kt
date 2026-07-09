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
 * BasicInfoActivity
 * Displays the "Información básica" (Basic Information) screen based on the Figma design.
 */
class BasicInfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_basic_info)

        val root = findViewById<android.view.View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Back button click action
        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }

        // Continue button click action
        findViewById<FrameLayout>(R.id.btn_continue).setOnClickListener {
            Toast.makeText(this, "Continuar clicked", Toast.LENGTH_SHORT).show()
            // Proceed to the next step
        }
    }
}
