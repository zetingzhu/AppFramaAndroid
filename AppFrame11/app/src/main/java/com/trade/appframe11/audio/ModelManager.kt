package com.trade.appframe11.audio

import android.content.Context
import android.util.Log
import com.trade.appframe11.audio.engines.AsrEngineType
import java.io.File
import java.io.FileOutputStream

/**
 * 模型文件管理器 —— 支持多引擎模型管理。
 *
 * 每个引擎的模型文件放在 assets 的对应子目录中：
 *   assets/sherpa-models/   → Sherpa-ONNX
 *   assets/whisper-models/  → Whisper
 *   assets/mediapipe-models/ → MediaPipe
 *   assets/vosk-models/     → Vosk
 *
 * 首次使用时自动解压到 filesDir 的对应目录。
 */
object ModelManager {

    private const val TAG = "ModelManager"

    /** 获取引擎对应的 assets 子目录名 */
    private fun getAssetsDir(engineType: AsrEngineType): String = when (engineType) {
        AsrEngineType.SHERPA_ONNX -> "sherpa-models"
        AsrEngineType.VOSK -> "vosk-models"
    }

    /**
     * 确保指定引擎的模型文件已从 assets 解压到 filesDir。
     *
     * @return 解压后的模型目录绝对路径
     */
    fun ensureModelFiles(context: Context, engineType: AsrEngineType): String {
        val assetsDir = getAssetsDir(engineType)
        val targetDir = File(context.filesDir, assetsDir)

        if (targetDir.exists() && (targetDir.listFiles()?.isNotEmpty() == true)) {
            Log.d(TAG, "[${engineType.displayName}] 模型文件已存在: ${targetDir.absolutePath}")
            return targetDir.absolutePath
        }

        // 检查 assets 中是否有对应目录
        val entries = context.assets.list(assetsDir)
        if (entries.isNullOrEmpty()) {
            Log.w(TAG, "[${engineType.displayName}] assets/$assetsDir 目录为空或不存在")
            targetDir.mkdirs()
            return targetDir.absolutePath
        }

        targetDir.mkdirs()
        copyAssetsDir(context, assetsDir, targetDir)
        Log.d(TAG, "[${engineType.displayName}] 模型文件解压完成: ${targetDir.absolutePath}")
        return targetDir.absolutePath
    }

    /**
     * 递归拷贝 assets 子目录到文件系统。
     */
    private fun copyAssetsDir(context: Context, assetsPath: String, targetDir: File) {
        val assetManager = context.assets
        val entries = assetManager.list(assetsPath) ?: return

        if (entries.isEmpty()) {
            copyAssetFile(context, assetsPath, targetDir)
            return
        }

        for (entry in entries) {
            val subAssetPath = "$assetsPath/$entry"
            val subEntries = assetManager.list(subAssetPath)

            if (subEntries != null && subEntries.isNotEmpty()) {
                val subDir = File(targetDir, entry)
                subDir.mkdirs()
                copyAssetsDir(context, subAssetPath, subDir)
            } else {
                copyAssetFile(context, subAssetPath, targetDir)
            }
        }
    }

    private fun copyAssetFile(context: Context, assetPath: String, targetDir: File) {
        val fileName = assetPath.substringAfterLast('/')
        val outFile = File(targetDir, fileName)

        try {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "拷贝失败: $assetPath", e)
        }
    }
}
