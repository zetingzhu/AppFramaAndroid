package com.trade.zt_drawableblur

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RealtimeBlurActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_realtime_blur)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Use the same adapter from BlurBannerActivity
        recyclerView.adapter = BlurBannerActivity.SimpleAdapter(
            (1..50).map { "Realtime List Item #$it - I blur automatically!" }
        )
    }
}
