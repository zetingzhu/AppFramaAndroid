package com.trade.zt_sketchmcp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trade.zt_sketchmcp.ui.theme.AppFrame11Theme

data class RouteItem(
    val title: String,
    val color: Color,
    val action: () -> Unit
)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val routes = listOf(
            RouteItem("显示披露声明弹框", Color(0xFFFE421C)) { showDisclosureDialog() },
            RouteItem("显示资金面板", Color(0xFF2F75F0)) { startActivity(Intent(this, FundsPanelActivity::class.java)) },
            RouteItem("显示 Cover 页面", Color(0xFF27AE60)) { startActivity(Intent(this, CoverActivity::class.java)) },
            RouteItem("显示新版资金面板", Color(0xFF1A1C33)) { startActivity(Intent(this, NewFundsPanelActivity::class.java)) },
            RouteItem("填写 About you 表单", Color(0xFFF2994A)) { startActivity(Intent(this, AboutYouActivity::class.java)) }
        )

        setContent {
            AppFrame11Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(routes) { item ->
                            Button(
                                onClick = item.action,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = item.color
                                )
                            ) {
                                Text(
                                    text = item.title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showDisclosureDialog() {
        val dialog = DisclosureDialogFragment.newInstance()
        dialog.onAcceptListener = {
            Toast.makeText(this, "用户已接受", Toast.LENGTH_SHORT).show()
        }
        dialog.onRejectListener = {
            Toast.makeText(this, "用户已拒绝", Toast.LENGTH_SHORT).show()
        }
        dialog.show(supportFragmentManager, DisclosureDialogFragment.TAG)
    }
}