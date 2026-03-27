package com.trade.appframe11.audio

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import com.trade.appframe11.audio.engines.AsrEngine
import com.trade.appframe11.audio.engines.AsrEngineType
import com.trade.appframe11.audio.engines.sherpaonnx.SherpaOnnxEngine
import com.trade.appframe11.audio.engines.vosk.VoskEngine
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 管线总控 —— 串联音频 AI 管线，支持多引擎切换：
 *
 *   采集 (TeeAudioProcessor)
 *     ↓
 *   预处理 (AudioResampler)
 *     ↓
 *   推理 (AsrEngine — 可切换)
 *     ↓
 *   表现 (SubtitleOverlayView)
 */
@UnstableApi
class AudioAiPipelineManager(private val context: Context) {

    // -------- 组件 --------
    private val pcmSink = PcmTeeAudioProcessor()
    private val resampler = AudioResampler()
    private var engine: AsrEngine? = null
    private var subtitleView: SubtitleOverlayView? = null

    // -------- 推理线程 --------
    private var inferenceThread: HandlerThread? = null
    private var inferenceHandler: Handler? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // -------- 背压 --------
    private val isBusy = AtomicBoolean(false)

    // -------- 当前引擎类型 --------
    var currentEngineType: AsrEngineType = AsrEngineType.SHERPA_ONNX
        private set

    /** 引擎切换回调（通知 UI 切换完成或失败） */
    var onEngineChanged: ((AsrEngineType, Boolean) -> Unit)? = null

    /**
     * 启动管线，默认使用 Sherpa-ONNX。
     */
    fun start(subtitleOverlay: SubtitleOverlayView, engineType: AsrEngineType = AsrEngineType.SHERPA_ONNX) {
        this.subtitleView = subtitleOverlay
        this.currentEngineType = engineType

        // 启动推理线程
        inferenceThread = HandlerThread("AudioAI-Inference").also { it.start() }
        inferenceHandler = Handler(inferenceThread!!.looper)

        // 初始化引擎
        inferenceHandler?.post {
            val eng = createEngine(engineType)
            eng.setOnTextListener { text ->
                mainHandler.post { subtitleView?.updateSubtitle(text) }
            }
            val success = eng.initialize(context)
            engine = eng
            Log.d(TAG, "${eng.name} 初始化${if (success) "成功" else "失败"}")
            mainHandler.post { onEngineChanged?.invoke(engineType, success) }
        }

        // 注册 PCM 回调
        registerPcmListener()
        Log.d(TAG, "管线已启动 — 引擎: $engineType")
    }

    /**
     * 切换 ASR 引擎（运行时热切换）。
     */
    fun switchEngine(newType: AsrEngineType) {
        if (newType == currentEngineType) return
        currentEngineType = newType

        inferenceHandler?.post {
            // 释放旧引擎
            engine?.release()
            engine = null

            // 清除字幕
            mainHandler.post { subtitleView?.updateSubtitle(null) }

            // 创建并初始化新引擎
            val eng = createEngine(newType)
            eng.setOnTextListener { text ->
                mainHandler.post { subtitleView?.updateSubtitle(text) }
            }
            val success = eng.initialize(context)
            engine = eng
            Log.d(TAG, "切换引擎 → ${eng.name} ${if (success) "✓" else "✗"}")
            mainHandler.post { onEngineChanged?.invoke(newType, success) }
        }
    }

    fun getTeeAudioProcessor(): TeeAudioProcessor = TeeAudioProcessor(pcmSink)

    fun pause() {
        pcmSink.setOnPcmDataListener(null)
        Log.d(TAG, "管线已暂停")
    }

    fun resume(subtitleOverlay: SubtitleOverlayView) {
        this.subtitleView = subtitleOverlay
        registerPcmListener()
        Log.d(TAG, "管线已恢复")
    }

    fun stop() {
        pcmSink.setOnPcmDataListener(null)

        val thread = inferenceThread
        val handler = inferenceHandler
        if (handler != null && thread != null) {
            handler.post {
                engine?.release()
                engine = null
                thread.quitSafely()
            }
        }
        inferenceThread = null
        inferenceHandler = null

        mainHandler.post { subtitleView?.updateSubtitle(null) }
        subtitleView = null
        Log.d(TAG, "管线已停止")
    }

    // ==================== 内部 ====================

    private fun createEngine(type: AsrEngineType): AsrEngine = when (type) {
        AsrEngineType.SHERPA_ONNX -> SherpaOnnxEngine()
        AsrEngineType.VOSK -> VoskEngine()
    }

    private fun registerPcmListener() {
        pcmSink.setOnPcmDataListener { pcmData, sampleRateHz, channelCount, _ ->
            if (!isBusy.compareAndSet(false, true)) return@setOnPcmDataListener
            inferenceHandler?.post {
                try {
                    val resampled = resampler.process(pcmData, sampleRateHz, channelCount)
                    if (resampled != null) {
                        engine?.feedAudio(resampled)
                    }
                } finally {
                    isBusy.set(false)
                }
            }
        }
    }

    companion object {
        private const val TAG = "AudioAiPipeline"
    }
}
