package com.trade.zt_sketchmcp

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class NewFundsPanelActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_funds_panel)

        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish() // 点击返回按钮关闭当前界面
        }
    }
}
