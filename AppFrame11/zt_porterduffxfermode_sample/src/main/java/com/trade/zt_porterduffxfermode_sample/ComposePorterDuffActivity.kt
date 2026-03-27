package com.trade.zt_porterduffxfermode_sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.trade.zt_porterduffxfermode_sample.ui.theme.AppFrame11Theme

/**
 * Compose 版本的 PorterDuffXfermode 演示页面。
 * 完全使用 Jetpack Compose 构建 UI 列表，但由于 Compose 尚未提供
 * 原生的 PorterDuff 复杂离屏绘制封装，所以这里通过 [AndroidView] 
 * 桥接传统的自定义 View ([PorterDuffDemoCanvasView]) 来完成绘制。
 */
class ComposePorterDuffActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppFrame11Theme {
                Scaffold { innerPadding ->
                    PorterDuffModeListScreen(
                        modifier = Modifier
                            .padding(innerPadding)
                            .navigationBarsPadding()
                    )
                }
            }
        }
    }
}

/**
 * 展示混合模式的滚动列表
 */
@Composable
fun PorterDuffModeListScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SharedData.PORTER_DUFF_MODE_ITEMS 是定义在 SharedData.kt 中的公共枚举列表
        items(SharedData.PORTER_DUFF_MODE_ITEMS) { item ->
            PorterDuffModeCard(item)
        }
    }
}

/**
 * 单个混合模式卡片组件
 * 包含：模式标题、详细描述、效果说明，以及一个承载绘图的画布。
 */
@Composable
fun PorterDuffModeCard(item: PorterDuffModeItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 模式标题，如 "CLEAR"
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            // 原理说明
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            // 视觉效果总结
            Text(
                text = "效果: ${item.effect}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp)
            )
            // 桥接传统自定义 View 进行绘制
            PorterDuffCanvas(
                mode = item.mode,
                srcAlpha = item.srcAlpha,
                useGradient = item.useGradient,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(top = 16.dp)
            )
        }
    }
}

/**
 * 将传统的 Android View ([PorterDuffDemoCanvasView]) 桥接到 Compose 中。
 * 
 * @param mode 当前需要演示的 PorterDuff.Mode
 * @param srcAlpha 源图形的透明度
 * @param useGradient 是否使用线性渐变绘制源图形
 */
@Composable
fun PorterDuffCanvas(mode: android.graphics.PorterDuff.Mode, srcAlpha: Float, useGradient: Boolean, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        // factory：在 Compose 初始化该 View 时被调用，负责实例化 View
        factory = { context ->
            PorterDuffDemoCanvasView(context).apply {
                this.mode = mode
                this.srcAlpha = srcAlpha
                this.useGradient = useGradient
            }
        },
        // update：在 Compose 发生重组 (Recomposition) 时被调用，用于更新 View 的属性
        update = { view -> 
            view.mode = mode 
            view.srcAlpha = srcAlpha
            view.useGradient = useGradient
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PorterDuffModeListPreview() {
    AppFrame11Theme {
        Surface {
            PorterDuffModeListScreen()
        }
    }
}
