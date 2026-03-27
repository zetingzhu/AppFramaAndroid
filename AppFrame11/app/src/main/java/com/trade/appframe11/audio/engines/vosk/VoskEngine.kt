package com.trade.appframe11.audio.engines.vosk

import android.content.Context
import android.util.Log
import com.trade.appframe11.audio.ModelManager
import org.vosk.Model
import org.vosk.Recognizer
import org.json.JSONObject
import com.trade.appframe11.audio.engines.AsrEngine
import com.trade.appframe11.audio.engines.AsrEngineType

/**
 * Vosk 引擎 —— 流式 ASR，基于 Kaldi。
 *
 * 轻量、快速、完全离线。
 * 使用 Vosk 的 Recognizer 逐帧喂入音频，
 * 支持部分结果 (partial) 和最终结果 (final)。
 */
class VoskEngine : AsrEngine {

    override val name = "Vosk"
    override val type = AsrEngineType.VOSK
    override val isStreaming = true

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var isReady = false
    private var listener: ((String) -> Unit)? = null

    override fun initialize(context: Context): Boolean {
        return try {
            val modelDir = ModelManager.ensureModelFiles(context, AsrEngineType.VOSK)

            model = Model(modelDir)
            recognizer = Recognizer(model, 16000f)
            isReady = true
            Log.d(TAG, "Vosk 初始化成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Vosk 初始化失败", e)
            isReady = false
            false
        }
    }

    override fun feedAudio(samples: FloatArray) {
        if (!isReady) return
        val rec = recognizer ?: return

        try {
            // Vosk 需要 short[] 输入，将 float[-1,1] 转为 short
            val shorts = ShortArray(samples.size) {
                (samples[it] * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
            // 转为 byte[]（Little-Endian 16-bit PCM）
            val bytes = ByteArray(shorts.size * 2)
            for (i in shorts.indices) {
                bytes[i * 2] = (shorts[i].toInt() and 0xFF).toByte()
                bytes[i * 2 + 1] = (shorts[i].toInt() shr 8 and 0xFF).toByte()
            }

            if (rec.acceptWaveForm(bytes, bytes.size)) {
                // 有最终结果
                val json = rec.result
                val text = parseVoskText(json)
                if (text.isNotEmpty()) {
                    listener?.invoke(text)
                }
            } else {
                // 部分结果
                val json = rec.partialResult
                val text = parseVoskPartial(json)
                if (text.isNotEmpty()) {
                    listener?.invoke(text)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vosk 推理异常", e)
        }
    }

    override fun setOnTextListener(listener: ((String) -> Unit)?) {
        this.listener = listener
    }

    override fun release() {
        try {
            recognizer?.close()
            model?.close()
        } catch (_: Exception) {}
        recognizer = null
        model = null
        isReady = false
        Log.d(TAG, "Vosk 已释放")
    }

    private fun parseVoskText(json: String): String {
        return try {
            JSONObject(json).optString("text", "").trim()
        } catch (_: Exception) { "" }
    }

    private fun parseVoskPartial(json: String): String {
        return try {
            JSONObject(json).optString("partial", "").trim()
        } catch (_: Exception) { "" }
    }

    companion object {
        private const val TAG = "VoskEngine"
    }
}
