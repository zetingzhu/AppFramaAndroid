package com.trade.zt_porterduffxfermode_sample

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Kotlin 版本的传统 View 体系演示页面。
 * 与 Java 版本类似，使用 XML 布局构建外层列表，
 * 但核心画布使用的是 Kotlin 编写的 [PorterDuffDemoCanvasView]。
 */
class KotlinViewPorterDuffActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 加载包含 ScrollView 的主布局
        setContentView(R.layout.activity_traditional)

        val tvHeader = findViewById<TextView>(R.id.tv_header)
        tvHeader.text = "PorterDuffXfermode 示例列车 (Kotlin View)"

        val container = findViewById<LinearLayout>(R.id.container)
        val inflater = LayoutInflater.from(this)

        // 遍历所有模式，动态向 ScrollView 中的 LinearLayout 添加列表项
        SharedData.PORTER_DUFF_MODE_ITEMS.forEach { item ->
            // 加载通用的列表项布局
            val itemView = inflater.inflate(R.layout.item_traditional, container, false)
            
            // 绑定数据到 TextView
            itemView.findViewById<TextView>(R.id.tv_title).text = item.title
            itemView.findViewById<TextView>(R.id.tv_description).text = item.description
            itemView.findViewById<TextView>(R.id.tv_effect).text = "效果说明：${item.effect}"
            
            val canvasContainer = itemView.findViewById<FrameLayout>(R.id.canvas_container)
            
            // 实例化 Kotlin 版本的自定义 View 并设置模式
            val demoView = PorterDuffDemoCanvasView(this).apply {
                mode = item.mode
                srcAlpha = item.srcAlpha
                useGradient = item.useGradient
            }
            
            // 添加到占位容器
            canvasContainer.addView(demoView)
            
            // 添加到外层滑动列表容器
            container.addView(itemView)
        }
    }
}
