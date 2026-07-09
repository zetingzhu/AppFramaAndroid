package com.trade.zt_sketchmcp

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * OverdueActivity
 * Displays the "Vencido" (Overdue Order Detail) screen based on the Figma design.
 */
class OverdueActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_overdue)

        // Handle edge-to-edge system bar insets gracefully
        val root = findViewById<android.view.View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            insets
        }

        // Back button navigation
        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }

        // Pay now button action
        findViewById<ConstraintLayout>(R.id.card_pay_now).setOnClickListener {
            Toast.makeText(this, "Procesando: Pagar ahora", Toast.LENGTH_SHORT).show()
        }

        // Payment extension button action
        findViewById<ConstraintLayout>(R.id.card_extension).setOnClickListener {
            Toast.makeText(this, "Procesando: Prórroga de pago", Toast.LENGTH_SHORT).show()
        }
    }
}
