package com.trade.zt_sketchmcp

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class FundsPanelActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_funds_panel)

        // Find the back button and finish the activity if clicked
        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }
    }
}
