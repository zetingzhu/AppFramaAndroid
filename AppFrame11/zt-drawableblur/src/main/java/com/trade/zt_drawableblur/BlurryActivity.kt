package com.trade.zt_drawableblur

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import jp.wasabeef.blurry.Blurry

class BlurryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var blurImageView: ImageView
    private lateinit var blurContainer: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blurry)

        recyclerView = findViewById(R.id.recycler_view)
        blurImageView = findViewById(R.id.blur_image_view)
        blurContainer = findViewById(R.id.blur_container)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = BlurBannerActivity.SimpleAdapter(
            (1..50).map { "Blurry Library Item #$it - Smooth & Fast" }
        )

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateBlur()
            }
        })

        recyclerView.post { updateBlur() }
    }

    private fun updateBlur() {
        if (recyclerView.width <= 0 || recyclerView.height <= 0) return

        // 停止使用 async() 以避免高频滚动下的 Bitmap 回收冲突
        // 增加采样率 (sampling) 可以极大提升同步模糊的速度，减少主线程压力
        
        blurContainer.visibility = View.INVISIBLE
        
        try {
            Blurry.with(this)
                .radius(10)      // 略微减小半径
                .sampling(4)     // 增加采样率（4倍下采样），渲染速度提升显著
                .capture(recyclerView)
                .into(blurImageView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
            
        blurContainer.visibility = View.VISIBLE
    }
}
