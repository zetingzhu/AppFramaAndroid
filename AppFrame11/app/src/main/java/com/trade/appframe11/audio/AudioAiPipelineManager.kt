package com.trade.appframe11.audio

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.TeeAudioProcessor

/**
 * 管线总控 —— 串联音频 AI 四层管线：
 *
 *   采集 (TeeAudioProcessor)
 *     ↓
 *   预处理 (AudioResampler)
 *     ↓
 *   推理 (MediaPipeAudioInference)
 *     ↓
 *   表现 (SubtitleOverlayView)
 *
 * 使用环形缓冲区积攒足够长度的音频窗口再送入推理。
 */
@UnstableApi
class AudioAiPipelineManager(private val context: Context) {

    // -------- 四层组件 --------
    private val pcmSink = PcmTeeAudioProcessor()
    private val resampler = AudioResampler()
    private var inference: MediaPipeAudioInference? = null
    private var subtitleView: SubtitleOverlayView? = null

    // -------- 推理线程 --------
    private var inferenceThread: HandlerThread? = null
    private var inferenceHandler: Handler? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // -------- 环形缓冲区 --------
    private val audioBuffer = FloatArrayRingBuffer(BUFFER_SIZE)

    /**
     * 初始化管线。
     * @param subtitleOverlay 用于显示推理结果的字幕视图。
     */
    fun start(subtitleOverlay: SubtitleOverlayView) {
        this.subtitleView = subtitleOverlay

        // 1) 启动推理后台线程
        inferenceThread = HandlerThread("AudioAI-Inference").also { it.start() }
        inferenceHandler = Handler(inferenceThread!!.looper)

        // 2) 初始化推理层（在后台线程）
        inferenceHandler?.post {
            inference = MediaPipeAudioInference(context).also { inf ->
                inf.setOnClassificationListener { results ->
                    // 取 top-1 结果，回到主线程更新 UI
                    val top = results.firstOrNull() ?: return@setOnClassificationListener
                    val text = "${top.label}  (${String.format("%.1f%%", top.score * 100)})"
                    mainHandler.post {
                        subtitleView?.updateSubtitle(text)
                    }
                }
                inf.initialize()
            }
        }

        // 3) 注册 PCM 数据回调 → 预处理 → 推理
        pcmSink.setOnPcmDataListener { pcmData, sampleRateHz, channelCount, _ ->
            val resampled = resampler.process(pcmData, sampleRateHz, channelCount)
                ?: return@setOnPcmDataListener

            // 写入环形缓冲区
            audioBuffer.write(resampled)

            // 当缓冲区积攒了足够窗口大小的样本时，取出送入推理
            if (audioBuffer.available() >= INFERENCE_WINDOW_SAMPLES) {
                val window = audioBuffer.read(INFERENCE_WINDOW_SAMPLES)
                if (window != null) {
                    inferenceHandler?.post {
                        inference?.classify(window)
                    }
                }
            }
        }

        Log.d(TAG, "管线已启动")
    }

    /**
     * 创建包含 TeeAudioProcessor 的 [RenderersFactory]，
     * 调用方用此工厂来构建 ExoPlayer 实例。
     */
    fun buildRenderersFactory(): RenderersFactory {
        return DefaultRenderersFactory(context)
            .setEnableAudioTrackPlaybackParams(true)
            .experimentalSetAudioOffloadPreferences(
                DefaultRenderersFactory.DEFAULT_AUDIO_OFFLOAD_PREFERENCES
            )
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
    }

    /**
     * 获取 TeeAudioProcessor 实例，用于注入 ExoPlayer 的音频处理链。
     */
    fun getTeeAudioProcessor(): TeeAudioProcessor {
        return TeeAudioProcessor(pcmSink)
    }

    /** 停止管线，释放所有资源。 */
    fun stop() {
        pcmSink.setOnPcmDataListener(null)

        inferenceHandler?.post {
            inference?.close()
            inference = null
        }
        inferenceThread?.quitSafely()
        inferenceThread = null
        inferenceHandler = null

        mainHandler.post {
            subtitleView?.updateSubtitle(null)
        }
        subtitleView = null
        audioBuffer.clear()

        Log.d(TAG, "管线已停止")
    }

    // ===================== 环形缓冲区 =====================

    /**
     * 简单的 Float 环形缓冲区，用于积攒音频窗口。
     * 线程安全（synchronized）。
     */
    private class FloatArrayRingBuffer(private val capacity: Int) {
        private val buffer = FloatArray(capacity)
        private var writePos = 0
        private var readPos = 0
        private var count = 0

        @Synchronized
        fun write(data: FloatArray) {
            for (sample in data) {
                buffer[writePos % capacity] = sample
                writePos++
                if (count < capacity) count++ else readPos++
            }
        }

        @Synchronized
        fun available(): Int = count

        @Synchronized
        fun read(length: Int): FloatArray? {
            if (count < length) return null
            val result = FloatArray(length)
            for (i in 0 until length) {
                result[i] = buffer[readPos % capacity]
                readPos++
            }
            count -= length
            return result
        }

        @Synchronized
        fun clear() {
            writePos = 0
            readPos = 0
            count = 0
        }
    }

    companion object {
        private const val TAG = "AudioAiPipeline"

        /** 环形缓冲区大小 ≈ 10秒 @16kHz */
        private const val BUFFER_SIZE = 16000 * 10

        /** 推理窗口 ≈ 1秒 @16kHz (15600 samples 是 YAMNet 的典型输入长度) */
        private const val INFERENCE_WINDOW_SAMPLES = 15600
    }
}
