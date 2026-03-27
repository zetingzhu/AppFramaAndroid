package com.trade.zt_porterduffxfermode_sample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trade.zt_porterduffxfermode_sample.ui.theme.AppFrame11Theme

/**
 * 应用主入口 Activity。
 * 提供三个按钮，分别跳转到用 Compose、纯 Java View 和 Kotlin 自定义 View 
 * 实现的 PorterDuffXfermode 示例页面。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppFrame11Theme {
                Scaffold { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .navigationBarsPadding()
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "请选择实现方式",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                        
                        // 跳转到完全使用 Compose 编写的页面
                        Button(
                            onClick = { startActivity(Intent(this@MainActivity, ComposePorterDuffActivity::class.java)) },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text("Compose 实现")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 跳转到使用传统 XML 布局 + 纯 Java 自定义 View 实现的页面
                        Button(
                            onClick = { startActivity(Intent(this@MainActivity, JavaPorterDuffActivity::class.java)) },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text("Java 实现")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 跳转到使用传统 XML 布局 + Kotlin 编写的自定义 View 实现的页面
                        Button(
                            onClick = { startActivity(Intent(this@MainActivity, KotlinViewPorterDuffActivity::class.java)) },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text("Kotlin View 实现")
                        }
                    }
                }
            }
        }
    }
}
