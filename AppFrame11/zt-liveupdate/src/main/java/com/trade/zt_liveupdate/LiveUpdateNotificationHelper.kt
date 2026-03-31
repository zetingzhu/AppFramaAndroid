package com.trade.zt_liveupdate


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Live Update 通知帮助类
 *
 * 实时更新通知 (Live Update) 是 Android 16 (API 36) 引入的新通知类型。
 * 它会被提升显示在通知抽屉顶部、锁屏和状态栏中。
 *
 * 要求：
 * 1. 必须使用标准样式 (BigTextStyle / CallStyle / ProgressStyle / MetricStyle)
 * 2. 必须在 Manifest 中声明 POST_PROMOTED_NOTIFICATIONS 权限
 * 3. 必须使用 EXTRA_REQUEST_PROMOTED_ONGOING 请求提升
 * 4. 必须设置 setOngoing(true) (FLAG_ONGOING_EVENT)
 * 5. 必须设置 contentTitle
 * 6. 不能使用 customContentView (RemoteViews)
 * 7. 不能是群组摘要通知
 * 8. 不能设置 setColorized(true)
 * 9. 通知渠道不能是 IMPORTANCE_MIN
 *
 * 全部使用 NotificationCompat 以保证向后兼容；
 * Live Update 特有的 API 通过 notification.extras 设置。
 */
class LiveUpdateNotificationHelper(private val context: Context) {

    companion object {
        private const val TAG = "LiveUpdateHelper"
        const val CHANNEL_ID = "live_update_channel"
        const val CHANNEL_NAME = "实时更新通知"
        const val NOTIFICATION_ID = 1001

        // Android 16 Notification extras 常量
        private const val EXTRA_REQUEST_PROMOTED_ONGOING =
            "android.requestPromotedOngoing"
        private const val EXTRA_SHORT_CRITICAL_TEXT =
            "android.shortCriticalText"
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    /**
     * 创建通知渠道 —— 注意不能使用 IMPORTANCE_MIN, 否则不能成为 Live Update
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH  // 高优先级，保证实时更新通知可以被提升
            ).apply {
                description = "用于显示实时更新通知（如外卖跟踪、导航、通话等）"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 发送一个实时更新通知
     *
     * @param title 通知标题（必填）
     * @param bigText 长文本内容
     * @param shortCriticalText 状态条状标签文本（最长约7个字符效果最好），例如 "5 分钟" "到达中"
     * @param targetTimeMillis 预计到达时间戳（倒计时）或开始时间戳（正计时），毫秒
     * @param useChronometer 是否开启数字变动的计时器模式
     * @param isCountDown 是否为倒计时（true=倒计时，false=正计时）
     * @param subText 辅助文本
     */
    fun showLiveUpdate(
        title: String,
        bigText: String,
        shortCriticalText: String? = null,
        targetTimeMillis: Long? = null,
        useChronometer: Boolean = false,
        isCountDown: Boolean = false,
        subText: String? = null
    ) {
        // 创建 PendingIntent，点击通知时打开 MainActivity
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 取消固定的操作 (用户可通过此操作关闭实时更新，而不影响后台任务)
        val dismissIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent("com.trade.zt_liveupdate.DISMISS_LIVE_UPDATE"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 使用 NotificationCompat.Builder 构建通知（向后兼容）
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            // 必须：设置标题
            .setContentTitle(title)
            .setContentText(bigText)
            // 必须：设置小图标
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            // 必须：设置为 ongoing（持续通知）
            .setOngoing(true)
            .setRequestPromotedOngoing(true) // 关键：请求提升优先级
            // 高优先级
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // 必须：使用标准样式 (BigTextStyle)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bigText)
                    .setBigContentTitle(title)
            )
            // 设置点击意图
            .setContentIntent(contentIntent)
            // 不自动取消
            .setAutoCancel(false)
            // 不能设置 colorized (Live Update 要求)
            .setColorized(false)
            // 添加取消固定操作（文档推荐）
            .addAction(
                NotificationCompat.Action.Builder(
                    null,
                    "取消跟踪",
                    dismissIntent
                ).build()
            )

        // 设置基准时间 (决定是从哪个时间开始算起)
        if (targetTimeMillis != null) {
            builder.setWhen(targetTimeMillis)
            builder.setShowWhen(true)
        }

        // 计时器动态跳字模式
        if (useChronometer) {
            builder.setUsesChronometer(true)
            if (targetTimeMillis != null) {
                builder.setChronometerCountDown(isCountDown)
            }
        }

        // 辅助文本
        if (subText != null) {
            builder.setSubText(subText)
        }

        val notification = builder.build()

        // ========== Android 16+ (API 36) Live Update 特有设置 ==========
        // 通过 notification.extras 设置 Live Update 标志
        if (Build.VERSION.SDK_INT >= 36) {
            // 请求提升为实时更新通知
            notification.extras.putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)

            // 状态条状标签文本 (Status Chip)
            if (shortCriticalText != null) {
                notification.extras.putCharSequence(EXTRA_SHORT_CRITICAL_TEXT, shortCriticalText)
            }

            Log.d(TAG, "Live Update extras set for API 36+")
        } else {
            Log.d(TAG, "Fallback mode: standard ongoing notification (API < 36)")
        }

        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Notification posted successfully (API ${Build.VERSION.SDK_INT})")
    }

    /**
     * 取消实时更新通知
     */
    fun cancelLiveUpdate() {
        notificationManager.cancel(NOTIFICATION_ID)
        Log.d(TAG, "Live Update notification cancelled")
    }

    /**
     * 检查是否可以发布推广通知 (API 36+)
     * 通过反射调用 NotificationManager.canPostPromotedNotifications()
     */
    fun canPostPromotedNotifications(context: Context): Boolean {
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.VANILLA_ICE_CREAM) { // 即 API 35
                // 只有在 Android 15+ 上才能调用此方法
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val canPost = notificationManager.canPostPromotedNotifications()
                if (canPost) {
                    // 执行发送“提升类”通知的逻辑
                    return true
                }
            } else {
                // Android 15 以下版本不支持此特性，走常规通知逻辑
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }


}
