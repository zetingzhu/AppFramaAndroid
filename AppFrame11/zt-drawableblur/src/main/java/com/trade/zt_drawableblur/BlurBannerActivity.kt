package com.trade.zt_drawableblur

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BlurBannerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var blurBanner: View
    private lateinit var blurImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blur_banner)

        recyclerView = findViewById(R.id.recycler_view)
        blurBanner = findViewById(R.id.blur_banner)
        blurImageView = findViewById(R.id.blur_image_view)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SimpleAdapter((1..50).map { "List Item #$it - Scroll me to see blur effect!" })

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateBlur()
            }
        })

        // Initial blur after layout is ready
        blurBanner.post { updateBlur() }
    }

    private fun updateBlur() {
        if (recyclerView.width <= 0 || recyclerView.height <= 0 || blurBanner.width <= 0 || blurBanner.height <= 0) return

        // Create a small bitmap for performance and initial blur effect
        val sampling = 4
        val bitmap = Bitmap.createBitmap(
            blurBanner.width / sampling,
            blurBanner.height / sampling,
            Bitmap.Config.ARGB_8888,
        )
        
        val canvas = Canvas(bitmap)
        canvas.scale(1f / sampling, 1f / sampling)

        val bannerLoc = IntArray(2)
        blurBanner.getLocationOnScreen(bannerLoc)
        val rvLoc = IntArray(2)
        recyclerView.getLocationOnScreen(rvLoc)

        // Translate to the banner's position relative to RecyclerView
        canvas.translate(
            (rvLoc[0] - bannerLoc[0]).toFloat(),
            (rvLoc[1] - bannerLoc[1]).toFloat()
        )

        // Draw only the RecyclerView. Since blurBanner is a sibling, 
        // it won't be included in this draw call.
        recyclerView.draw(canvas)

        blurImageView.setImageBitmap(bitmap)

        // For Android 12+, apply high-quality Gaussian Blur
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurImageView.setRenderEffect(
                RenderEffect.createBlurEffect(15f, 15f, Shader.TileMode.CLAMP)
            )
        }
    }

    class SimpleAdapter(private val items: List<String>) : RecyclerView.Adapter<SimpleAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = items[position]
            // Add some color to make blur more visible
            if (position % 2 == 0) {
                holder.itemView.setBackgroundColor(0xFFEEEEEE.toInt())
            } else {
                holder.itemView.setBackgroundColor(0xFFFFFFFF.toInt())
            }
        }

        override fun getItemCount() = items.size
    }
}
