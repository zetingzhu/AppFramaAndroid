package com.trade.zt_shimmerframelayout

import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trade.zt_shimmerframelayout.ui.theme.AppFrame11Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppFrame11Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    EntryScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun EntryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.entry_title))
        Button(
            onClick = { ShimmerSampleAct.start(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.entry_shimmer_sample))
        }

        // 获取系统默认的无衬线字体
        val defaultTypeface = Typeface.DEFAULT
        Text(text = "默认字体一：${defaultTypeface}")

        // 或者通过名称获取
        val sansSerifTypeface = Typeface.create("sans-serif", Typeface.NORMAL)
        Text(text = "默认字体二：${sansSerifTypeface.toString()}")
    }
}
