package com.trade.zt_liveupdate

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.trade.zt_liveupdate.ui.theme.AppFrame11Theme

/**
 * Live Update 通知演示 Activity
 *
 * 模拟外卖/网约车跟踪场景，演示 Android 16 实时更新通知的各个阶段：
 *  1. 正在准备  →  2. 骑手已出发  →  3. 即将送达  →  4. 已送达（取消通知）
 */
class MainActivity : ComponentActivity() {

    private lateinit var liveUpdateHelper: LiveUpdateNotificationHelper

    // 请求通知权限 (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "通知权限已授予 ✅", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "通知权限被拒绝 ❌，无法显示通知", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        liveUpdateHelper = LiveUpdateNotificationHelper(this)

        // 请求通知权限 (Android 13+)
        requestNotificationPermission()

        setContent {
            AppFrame11Theme {
                LiveUpdateDemoScreen(
                    onShowLiveUpdate = { step -> showLiveUpdateForStep(step) },
                    onCancelLiveUpdate = { liveUpdateHelper.cancelLiveUpdate() },
                    canPostPromoted = liveUpdateHelper.canPostPromotedNotifications(this),
                    isApi36 = Build.VERSION.SDK_INT >= 36
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * 按步骤显示不同的实时更新通知
     */
    private fun showLiveUpdateForStep(step: Int) {
        when (step) {
            1 -> liveUpdateHelper.showLiveUpdate(
                title = "🍜 正在准备您的订单",
                bigText = "商家正在精心准备您的订单，预计5分钟后骑手取餐。\n" +
                        "订单包含：宫保鸡丁 x1、米饭 x2、可乐 x1",
                shortCriticalText = "准备中",
                subText = "美团外卖"
            )

            2 -> liveUpdateHelper.showLiveUpdate(
                title = "🛵 骑手已取餐出发",
                bigText = "骑手 小王 已取餐并正在配送中。\n" +
                        "当前距您约 2.3 公里，预计 8 分钟送达。",
                shortCriticalText = "8 分钟",
                targetTimeMillis = System.currentTimeMillis() + 8 * 60 * 1000, // 8分钟后到达
                useChronometer = true,
                isCountDown = true, // 这是【倒计时】
                subText = "美团外卖 · 配送中"
            )

            3 -> liveUpdateHelper.showLiveUpdate(
                title = "📍 骑手即将到达",
                bigText = "骑手 小王 已到达您附近，请准备取餐！\n" +
                        "如需联系骑手，请点击通知。",
                shortCriticalText = "到达中",
                targetTimeMillis = System.currentTimeMillis() + 2 * 60 * 1000, // 2分钟后到达
                useChronometer = true,
                isCountDown = true, // 这是【倒计时】
                subText = "美团外卖 · 即将送达"
            )

            4 -> liveUpdateHelper.showLiveUpdate(
                title = "🏃 正在跑步中",
                bigText = "已跑步 3.2 公里 · 配速 5'30\"/km\n" +
                        "心率 145 bpm · 消耗 210 千卡",
                shortCriticalText = "3.2km",
                useChronometer = true,
                targetTimeMillis = System.currentTimeMillis(), // 从当前时间开始计时
                isCountDown = false, // 这是【正向计时】（时长增加）
                subText = "运动跟踪"
            )

            5 -> liveUpdateHelper.showLiveUpdate(
                title = "📞 正在通话中",
                bigText = "与 张三 通话中\n通话时长由状态芯片中的计时器显示",
                shortCriticalText = "通话中",
                useChronometer = true,
                targetTimeMillis = System.currentTimeMillis(), // 通话开始时间
                isCountDown = false, // 这是【正向计时】（通话时长）
                subText = "电话"
            )
        }
    }
}

// ========================= Compose UI =========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveUpdateDemoScreen(
    onShowLiveUpdate: (Int) -> Unit,
    onCancelLiveUpdate: () -> Unit,
    canPostPromoted: Boolean,
    isApi36: Boolean
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var isActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Live Update 通知演示",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 状态信息卡片
            StatusInfoCard(isApi36 = isApi36, canPostPromoted = canPostPromoted)

            // 当前状态指示器
            if (isActive) {
                ActiveStatusIndicator(step = currentStep)
            }

            // === 场景一：外卖跟踪 ===
            SectionHeader(title = "🍜 场景一：外卖跟踪")

            DeliveryStepButton(
                step = 1,
                label = "1. 正在准备订单",
                description = "商家开始准备，显示实时更新通知",
                isCurrentStep = currentStep == 1 && isActive,
                onClick = {
                    currentStep = 1
                    isActive = true
                    onShowLiveUpdate(1)
                }
            )

            DeliveryStepButton(
                step = 2,
                label = "2. 骑手已出发",
                description = "骑手取餐出发，显示 ETA 倒计时",
                isCurrentStep = currentStep == 2 && isActive,
                onClick = {
                    currentStep = 2
                    isActive = true
                    onShowLiveUpdate(2)
                }
            )

            DeliveryStepButton(
                step = 3,
                label = "3. 即将送达",
                description = "骑手到达附近，紧急状态",
                isCurrentStep = currentStep == 3 && isActive,
                onClick = {
                    currentStep = 3
                    isActive = true
                    onShowLiveUpdate(3)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // === 场景二：运动跟踪 ===
            SectionHeader(title = "🏃 场景二：运动跟踪")

            DeliveryStepButton(
                step = 4,
                label = "开始跑步跟踪",
                description = "使用计时器模式，显示运动数据",
                isCurrentStep = currentStep == 4 && isActive,
                onClick = {
                    currentStep = 4
                    isActive = true
                    onShowLiveUpdate(4)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // === 场景三：通话 ===
            SectionHeader(title = "📞 场景三：通话")

            DeliveryStepButton(
                step = 5,
                label = "模拟通话中",
                description = "持续通话，计时器计数",
                isCurrentStep = currentStep == 5 && isActive,
                onClick = {
                    currentStep = 5
                    isActive = true
                    onShowLiveUpdate(5)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 取消按钮
            OutlinedButton(
                onClick = {
                    onCancelLiveUpdate()
                    isActive = false
                    currentStep = 0
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                enabled = isActive
            ) {
                Text(
                    "❌ 取消实时更新通知",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 说明文档卡片
            DocumentationCard()
        }
    }
}

@Composable
fun StatusInfoCard(isApi36: Boolean, canPostPromoted: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isApi36)
                Color(0xFFE8F5E9) // 绿色背景
            else
                Color(0xFFFFF3E0) // 橙色背景
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isApi36) "✅ 当前设备支持 Live Update (API 36+)"
                else "⚠️ 当前设备 API < 36，将使用回退方案",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isApi36) Color(0xFF2E7D32) else Color(0xFFE65100)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "API Level: ${Build.VERSION.SDK_INT} | " +
                        "推广通知: ${if (canPostPromoted) "可用" else "不可用"}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ActiveStatusIndicator(step: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1B5E20).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 呼吸灯效果
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50).copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "实时更新通知活跃中 · 步骤 $step",
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun DeliveryStepButton(
    step: Int,
    label: String,
    description: String,
    isCurrentStep: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isCurrentStep)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface,
        label = "bgColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 步骤编号圆圈
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentStep)
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF4CAF50),
                                    Color(0xFF2E7D32)
                                )
                            )
                        else
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$step",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isCurrentStep) {
                Text(
                    text = "活跃",
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun DocumentationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📖 关于 Live Update 通知",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = """
                    实时更新通知 (Live Update) 是 Android 16 (API 36) 新增功能。

                    核心要求：
                    • 使用标准样式 (BigTextStyle 等)
                    • 声明 POST_PROMOTED_NOTIFICATIONS 权限
                    • 调用 setRequestPromotedOngoing(true)
                    • 设置 setOngoing(true)
                    • 必须有 contentTitle

                    使用场景：
                    • 正在进行的导航
                    • 外卖/网约车跟踪
                    • 正在进行的通话
                    • 运动跟踪

                    不适用场景：
                    • 广告/促销
                    • 聊天消息
                    • 应用快捷方式
                """.trimIndent(),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}