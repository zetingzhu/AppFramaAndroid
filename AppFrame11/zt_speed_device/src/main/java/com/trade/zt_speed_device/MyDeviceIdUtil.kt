package com.trade.zt_speed_device

import android.app.ActivityManager
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Locale
import java.util.TimeZone

object MyDeviceIdUtil {

    private const val TAG = "MediaStoreBackupUtil"

    // 文件名不要用 '.' 开头，部分机型 MediaStore 索引/查询会异常
    private const val FILE_NAME = "sys_config_cache.dat"
    private val RELATIVE_PATH = "${Environment.DIRECTORY_DOWNLOADS}/.system_app/"
    private const val LEGACY_DIR_NAME = ".system_app"

    /**
     * 保存数据到 MediaStore (Downloads 目录)，并尽量同步写入公共 Downloads 物理文件。
     */
    fun saveData(content: String): Boolean {
        if (content.isBlank()) {
            return false
        }
        val context = appContext() ?: return false
        return saveData(context, content)
    }

    /**
     * 从 MediaStore / 公共 Downloads 物理文件读取（尽量跨重装保留）。
     */
    fun readData(): String? {
        val context = appContext() ?: return null
        return readData(context)
    }

    /**
     * 保存数据到 MediaStore (Downloads 目录)，并尽量同步写入公共 Downloads 物理文件。
     */
    fun saveData(context: Context?, content: String): Boolean {
        if (context == null || content.isBlank()) {
            return false
        }
        val appContext = context.applicationContext ?: return false
        val resolver = appContext.contentResolver ?: return false

        val existingContent = readData(appContext)
        if (existingContent == content) {
            return true
        }

        var mediaStoreSaved = false
        val existingUri = findExistingFileUri(appContext)
        mediaStoreSaved = if (existingUri != null) {
            writeContent(resolver, existingUri, content)
        } else {
            insertNewFile(resolver, content)
        }

        val legacySaved = saveToLegacyFile(content)
        Log.d(TAG, "saveData mediaStore=$mediaStoreSaved legacy=$legacySaved")
        return mediaStoreSaved || legacySaved
    }

    /**
     * 从 MediaStore / 公共 Downloads 物理文件读取（尽量跨重装保留）。
     */
    fun readData(context: Context?): String? {
        if (context == null) {
            return null
        }
        val appContext = context.applicationContext ?: return null

        readFromMediaStore(appContext)?.let {
            Log.d(TAG, "readData from MediaStore")
            return it
        }

        readFromLegacyFile()?.let {
            Log.d(TAG, "readData from legacy file")
            // 物理文件还在但 MediaStore 索引丢失时，尝试重新挂回 MediaStore，便于后续读写
            saveData(appContext, it)
            return it
        }

        Log.d(TAG, "readData empty")
        return null
    }

    private fun appContext(): Context? {
        return try {
            MyApplication.getInstance()?.applicationContext
        } catch (e: Exception) {
            Log.e(TAG, "appContext failed", e)
            null
        }
    }

    private fun getDownloadsCollectionUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }
    }

    private fun readFromMediaStore(context: Context): String? {
        val resolver = context.contentResolver ?: return null
        val uri = findExistingFileUri(context) ?: return null
        return readContent(resolver, uri)
    }

    private fun findExistingFileUri(context: Context): Uri? {
        findExistingFileUri(context, getDownloadsCollectionUri())?.let { return it }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            findExistingFileUri(context, MediaStore.Files.getContentUri("external"))?.let { return it }
        }
        return null
    }

    private fun findExistingFileUri(context: Context, collection: Uri): Uri? {
        val resolver = context.contentResolver ?: return null
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH
        )

        findBySelection(
            resolver,
            collection,
            projection,
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf(FILE_NAME)
        )?.let { return it }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            findBySelection(
                resolver,
                collection,
                projection,
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
                arrayOf(FILE_NAME, "%$LEGACY_DIR_NAME%")
            )?.let { return it }

            // 兼容旧版本可能写入的隐藏文件名
            findBySelection(
                resolver,
                collection,
                projection,
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
                arrayOf(".sys_config_cache.dat", "%$LEGACY_DIR_NAME%")
            )?.let { return it }
        }
        return null
    }

    private fun findBySelection(
        resolver: ContentResolver,
        collection: Uri,
        projection: Array<String>,
        selection: String,
        selectionArgs: Array<String>
    ): Uri? {
        return try {
            resolver.query(collection, projection, selection, selectionArgs, "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")
                ?.use { cursor ->
                    val idColumn = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                    if (idColumn < 0) {
                        return null
                    }
                    while (cursor.moveToNext()) {
                        return ContentUris.withAppendedId(collection, cursor.getLong(idColumn))
                    }
                    null
                }
        } catch (e: Exception) {
            Log.e(TAG, "findBySelection failed: $selection", e)
            null
        }
    }

    private fun insertNewFile(resolver: ContentResolver, content: String): Boolean {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, FILE_NAME)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = getDownloadsCollectionUri()
        var uri: Uri? = null
        return try {
            uri = resolver.insert(collection, contentValues) ?: return false
            if (!writeContent(resolver, uri, content)) {
                deleteFileQuietly(resolver, uri)
                return false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "insertNewFile failed", e)
            uri?.let { deleteFileQuietly(resolver, it) }
            false
        }
    }

    private fun readContent(resolver: ContentResolver, uri: Uri): String? {
        return try {
            resolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText().takeIf { it.isNotEmpty() }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "readContent failed", e)
            null
        }
    }

    private fun writeContent(
        resolver: ContentResolver,
        uri: Uri,
        content: String
    ): Boolean {
        return try {
            resolver.openOutputStream(uri, "wt")?.use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
                outputStream.flush()
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "writeContent failed", e)
            false
        }
    }

    private fun deleteFileQuietly(
        resolver: ContentResolver,
        uri: Uri
    ) {
        try {
            resolver.delete(uri, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "deleteFileQuietly failed", e)
        }
    }

    /**
     * 公共 Downloads 物理文件：Android 11+ 卸载时 MediaStore 记录会被系统删除，
     * 但部分机型物理文件可能保留；重装后优先尝试从此处恢复。
     */
    private fun getLegacyBackupFile(): File? {
        return try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                ?: return null
            File(downloadDir, "$LEGACY_DIR_NAME${File.separator}$FILE_NAME")
        } catch (e: Exception) {
            Log.e(TAG, "getLegacyBackupFile failed", e)
            null
        }
    }

    private fun saveToLegacyFile(content: String): Boolean {
        val file = getLegacyBackupFile() ?: return false
        return try {
            val parent = file.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            file.writeText(content, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            Log.e(TAG, "saveToLegacyFile failed", e)
            false
        }
    }

    private fun readFromLegacyFile(): String? {
        val file = getLegacyBackupFile() ?: return null
        if (!file.exists() || !file.canRead()) {
            // 兼容旧隐藏文件名
            val legacyHidden = file.parentFile?.let { File(it, ".sys_config_cache.dat") }
            if (legacyHidden == null || !legacyHidden.exists() || !legacyHidden.canRead()) {
                return null
            }
            return readLegacyFileContent(legacyHidden)
        }
        return readLegacyFileContent(file)
    }

    private fun readLegacyFileContent(file: File): String? {
        return try {
            file.readText(Charsets.UTF_8).takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e(TAG, "readLegacyFileContent failed", e)
            null
        }
    }

    /**
     * 获取所有免权限的设备信息，返回 Map 结构
     */
    fun getAllDeviceInfoMap(context: Context): Map<String, Any?> {
        val info = mutableMapOf<String, Any?>()

        // ==========================================
        // 1. 基础唯一标识 (Identifiers)
        // ==========================================
        // Settings.Secure.ANDROID_ID: 64位十六进制字符串，恢复出厂设置或应用签名/密钥更改时可能会重置
        info["android_id"] = getAndroidId(context)

        // ==========================================
        // 2. 硬件基因与构建信息 (Hardware Specs)
        // ==========================================
        info["brand"] = Build.BRAND                 // 设备品牌 (例如: "Xiaomi", "google", "Huawei")
        info["manufacturer"] = Build.MANUFACTURER   // 设备生产厂商 (例如: "Xiaomi", "HUAWEI")
        info["model"] = Build.MODEL                 // 手机型号/营销名称 (例如: "23127PN0CC", "Pixel 8")
        info["product"] = Build.PRODUCT             // 产品内部代号 (例如: "shennong", "husky")
        info["hardware"] = Build.HARDWARE           // 芯片/硬件平台 (例如: "qcom", "tensor")
        info["board"] = Build.BOARD                 // 主板名称 (例如: "kalama")
        info["device"] = Build.DEVICE               // 工业设计设备名 (例如: "vermeer")
        info["supported_abis"] = Build.SUPPORTED_ABIS.toList() // 支持的 CPU 架构指令集列表 (例如: ["arm64-v8a", "armeabi-v7a"])

        // ==========================================
        // 3. 系统与固件版本信息 (OS Version)
        // ==========================================
        info["os_release"] = Build.VERSION.RELEASE  // Android 系统版本号 (例如: "14", "15")
        info["sdk_int"] = Build.VERSION.SDK_INT      // 系统 API Level (例如: 34, 35)
        info["security_patch"] = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "Unknown" // 安全补丁日期 (例如: "2026-05-05")
        info["display_build_id"] = Build.DISPLAY    // 厂商系统版本展示 ID/ROM 固件版本号 (例如: "UKQ1.230804.001")
        info["fingerprint"] = Build.FINGERPRINT      // 唯一标识此 Build 固件的指纹签名串 (包含品牌、型号、版本等信息的组合长串)

        // ==========================================
        // 4. 屏幕与显示指标 (Display Metrics)
        // ==========================================
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        info["screen_width_px"] = displayMetrics.widthPixels   // 屏幕绝对像素宽度 (像素 px)
        info["screen_height_px"] = displayMetrics.heightPixels // 屏幕绝对像素高度 (像素 px)
        info["density_dpi"] = displayMetrics.densityDpi       // 屏幕像素密度 DPI (例如: 480 dpi)
        info["density"] = displayMetrics.density               // dp 与 px 的转换系数 (例如: 3.0，代表 1dp = 3px)

        // ==========================================
        // 5. 内部存储容量 (ROM)
        // ==========================================
        val path = Environment.getDataDirectory()
        val statFs = StatFs(path.path)
        val blockSize = statFs.blockSizeLong
        val totalBlocks = statFs.blockCountLong
        val availableBlocks = statFs.availableBlocksLong

        info["rom_total_bytes"] = totalBlocks * blockSize           // 内部存储 (ROM) 总大小 (单位: 字节 Byte)
        info["rom_available_bytes"] = availableBlocks * blockSize   // 内部存储 (ROM) 剩余可用空间 (单位: 字节 Byte)

        // ==========================================
        // 6. 运行内存 (RAM)
        // ==========================================
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        info["ram_total_bytes"] = memoryInfo.totalMem       // 物理运行内存 (RAM) 总容量 (单位: 字节 Byte)
        info["ram_available_bytes"] = memoryInfo.availMem   // 当前系统剩余可用运行内存 (单位: 字节 Byte)
        info["ram_low_memory"] = memoryInfo.lowMemory       // 当前系统是否处于低内存(Low Memory)警告状态 (Boolean)

        // ==========================================
        // 7. 区域、语言与时区 (Locale & TimeZone)
        // ==========================================
        val locale = Locale.getDefault()
        info["language"] = locale.language                  // 当前系统设置的语言代码 (例如: "zh", "en")
        info["country"] = locale.country                    // 当前系统设置的国家/地区代码 (例如: "CN", "US")
        info["locale_tag"] = locale.toLanguageTag()         // 标准语言标签 (例如: "zh-CN", "en-US")

        val timeZone = TimeZone.getDefault()
        info["timezone_id"] = timeZone.id                   // 时区标识符 (例如: "Asia/Shanghai")
        info["timezone_display_name"] = timeZone.getDisplayName(false, TimeZone.SHORT) // 时区简称 (例如: "GMT+08:00")
        info["timezone_raw_offset"] = timeZone.rawOffset    // 与标准 UTC 时间的毫秒偏移量 (例如: 28800000 毫秒 = +8小时)

        // ==========================================
        // 8. 电池状态 (Battery Status)
        // ==========================================
        val batteryIntent = context.registerReceiver(null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugType = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1

        info["battery_level_percent"] = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()) else -1 // 当前剩余电量百分比 (0.0 ~ 100.0)
        info["battery_is_charging"] = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL // 当前是否在充电状态 (Boolean)
        info["battery_plug_type"] = when (plugType) {      // 当前供电/充电来源方式 ("AC"-交流电直充, "USB"-USB数据线, "Wireless"-无线充电, "Battery"-未插线)
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Battery"
        }

        // ==========================================
        // 9. 网络基本连接类型 (Network Status)
        // ==========================================
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(activeNetwork)

        info["is_wifi"] = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            ?: false         // 当前是否连接了 Wi-Fi 网络 (Boolean)
        info["is_cellular"] = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            ?: false // 当前是否连接了移动蜂窝网络 (Boolean)
        info["is_vpn"] = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            ?: false           // 当前网络流量是否正经过 VPN 代理 (Boolean)

        return info
    }

    /**
     * 获取格式化后的 JSON 字符串，并包含字段含义的描述映射
     */
    fun getAllDeviceInfoJson(context: Context): String {
        val rawData = getAllDeviceInfoMap(context)

        // 字段中文名称与含义描述的映射，方便直观查看
        val fieldDescriptions = mapOf(
            "android_id" to "Android ID (系统防刷/标识符)",
            "brand" to "设备品牌",
            "manufacturer" to "生产厂商",
            "model" to "手机型号",
            "product" to "产品内部代号",
            "hardware" to "芯片/硬件平台",
            "board" to "主板名称",
            "device" to "设备工业设计名",
            "supported_abis" to "支持的 CPU 架构指令集",
            "os_release" to "Android 系统版本号",
            "sdk_int" to "系统 API Level",
            "security_patch" to "安全补丁日期",
            "display_build_id" to "固件/系统构建版本 ID",
            "fingerprint" to "唯一固件指纹签名串",
            "screen_width_px" to "屏幕像素宽度 (px)",
            "screen_height_px" to "屏幕像素高度 (px)",
            "density_dpi" to "屏幕像素密度 (DPI)",
            "density" to "dp与px转换系数",
            "rom_total_bytes" to "内部存储(ROM)总空间 (字节)",
            "rom_available_bytes" to "内部存储(ROM)可用空间 (字节)",
            "ram_total_bytes" to "运行内存(RAM)总容量 (字节)",
            "ram_available_bytes" to "当前可用运行内存 (字节)",
            "ram_low_memory" to "系统是否处于低内存状态",
            "language" to "系统语言代码",
            "country" to "国家/地区代码",
            "locale_tag" to "标准语言标签",
            "timezone_id" to "时区 ID",
            "timezone_display_name" to "时区显示名称",
            "timezone_raw_offset" to "UTC 时区毫秒偏移量",
            "battery_level_percent" to "当前电池电量百分比",
            "battery_is_charging" to "当前是否正在充电",
            "battery_plug_type" to "电源连接类型",
            "is_wifi" to "是否连接了 Wi-Fi",
            "is_cellular" to "是否连接了移动网络",
            "is_vpn" to "是否开启了 VPN"
        )

        val jsonResult = JSONObject()
        jsonResult.put("field_descriptions", JSONObject(fieldDescriptions)) // 中文映射字段说明
        jsonResult.put("device_data", JSONObject(rawData))                  // 实际提取到的设备数据

        return jsonResult.toString(4)
    }

    private fun getAndroidId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}