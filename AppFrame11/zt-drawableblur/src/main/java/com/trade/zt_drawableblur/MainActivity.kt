package com.trade.zt_drawableblur

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.trade.zt_drawableblur.ui.theme.AppFrame11Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppFrame11Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        Greeting(name = "Android")
        Button(onClick = {
            context.startActivity(Intent(context, BlurBannerActivity::class.java))
        }) {
            Text(stringResource(R.string.go_to_blur_banner_demo))
        }
        Button(onClick = {
            context.startActivity(Intent(context, RealtimeBlurActivity::class.java))
        }) {
            Text(stringResource(R.string.go_to_realtime_blur_demo))
        }
        Button(onClick = {
            context.startActivity(Intent(context, BlurryActivity::class.java))
        }) {
            Text(stringResource(R.string.go_to_blurry_library_demo))
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AppFrame11Theme {
        Greeting("Android")
    }
}
