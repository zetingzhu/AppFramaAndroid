package com.trade.appframe11.audio.engines.sherpaonnx

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.EndpointConfig
import com.k2fsa.sherpa.onnx.EndpointRule
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import com.trade.appframe11.audio.ModelManager
import com.trade.appframe11.audio.engines.AsrEngine
import com.trade.appframe11.audio.engines.AsrEngineType

/**
 * Sherpa-ONNX 引擎 —— 流式 ASR，边说边出字。
 *
 * 使用 OnlineRecognizer 实时语音识别，
 * 支持端点检测自动分句。
 */
class SherpaOnnxEngine : AsrEngine {

    override val name = "Sherpa-ONNX"
    override val type = AsrEngineType.SHERPA_ONNX
    override val isStreaming = true

    private var recognizer: OnlineRecognizer? = null
    private var stream: OnlineStream? = null
    private var isReady = false
    private var listener: ((String) -> Unit)? = null
    private var lastText = ""

    override fun initialize(context: Context): Boolean {
        return try {
            val modelDir = ModelManager.ensureModelFiles(context, AsrEngineType.SHERPA_ONNX)

            val config = OnlineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
                modelConfig = OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = "$modelDir/encoder-epoch-99-avg-1.int8.onnx",
                        decoder = "$modelDir/decoder-epoch-99-avg-1.onnx",
                        joiner = "$modelDir/joiner-epoch-99-avg-1.int8.onnx",
                    ),
                    tokens = "$modelDir/tokens.txt",
                    modelType = "zipformer",
                    numThreads = 2,
                    debug = false,
                ),
                endpointConfig = EndpointConfig(
                    rule1 = EndpointRule(false, 2.0f, 0.0f),
                    rule2 = EndpointRule(true, 1.2f, 0.0f),
                    rule3 = EndpointRule(false, 0.0f, 20.0f),
                ),
                enableEndpoint = true,
                decodingMethod = "greedy_search",
            )

            recognizer = OnlineRecognizer(config = config)
            stream = recognizer!!.createStream()
            isReady = true
            Log.d(TAG, "Sherpa-ONNX 初始化成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Sherpa-ONNX 初始化失败", e)
            isReady = false
            false
        }
    }

    override fun feedAudio(samples: FloatArray) {
        if (!isReady) return
        val rec = recognizer ?: return
        val s = stream ?: return

        try {
            s.acceptWaveform(samples, sampleRate = 16000)

            while (rec.isReady(s)) {
                rec.decode(s)
            }

            val result = rec.getResult(s)
            val text = result.text.trim().lowercase().replaceFirstChar { it.uppercaseChar() }

            if (text.isNotEmpty() && text != lastText) {
                lastText = text
                listener?.invoke(text)
            }

            if (rec.isEndpoint(s)) {
                if (text.isNotEmpty()) {
                    Log.d(TAG, "端点: $text")
                }
                rec.reset(s)
                lastText = ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "推理异常", e)
        }
    }

    override fun setOnTextListener(listener: ((String) -> Unit)?) {
        this.listener = listener
    }

    override fun release() {
        try {
            stream?.release()
            recognizer?.release()
        } catch (_: Exception) {}
        stream = null
        recognizer = null
        isReady = false
        lastText = ""
        Log.d(TAG, "Sherpa-ONNX 已释放")
    }

    companion object {
        private const val TAG = "SherpaOnnxEngine"
    }
}
