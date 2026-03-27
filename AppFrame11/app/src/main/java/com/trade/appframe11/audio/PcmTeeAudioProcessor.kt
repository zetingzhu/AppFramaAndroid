package com.trade.appframe11.audio

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 音频采集层 —— 基于 ExoPlayer TeeAudioProcessor。
 *
 * 像"三通阀门"一样，在正常音频输出的同时，将原始 PCM 数据拷贝一份
 * 回调给注册的 [OnPcmDataListener]，供下游 AI 模块使用。
 */
@UnstableApi
class PcmTeeAudioProcessor : TeeAudioProcessor.AudioBufferSink {

    /** 当前音频流参数 */
    var sampleRateHz: Int = 0
        private set
    var channelCount: Int = 0
        private set
    var encoding: Int = C.ENCODING_PCM_16BIT
        private set

    private var listener: OnPcmDataListener? = null

    /** 注册 PCM 数据回调 */
    fun setOnPcmDataListener(l: OnPcmDataListener?) {
        listener = l
    }

    // ---------- TeeAudioProcessor.AudioBufferSink ----------

    /**
     * 每当音频格式变化时调用（首次播放 / seek / 切换流）。
     */
    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.sampleRateHz = sampleRateHz
        this.channelCount = channelCount
        this.encoding = encoding
        android.util.Log.d(
            TAG,
            "flush → sampleRate=$sampleRateHz, channels=$channelCount, encoding=$encoding"
        )
    }

    /**
     * 每帧 PCM 数据到达时调用。
     * 将 ByteBuffer 中的原始字节拷贝给 listener。
     */
    override fun handleBuffer(buffer: ByteBuffer) {
        val listener = this.listener ?: return
        // 保存当前 position/limit 以免影响 ExoPlayer 后续处理
        val pos = buffer.position()
        val remaining = buffer.remaining()
        if (remaining <= 0) return

        val copy = ByteArray(remaining)
        buffer.get(copy)
        buffer.position(pos) // 恢复 position

        listener.onPcmData(copy, sampleRateHz, channelCount, encoding)
    }

    // ---------- Listener ----------

    fun interface OnPcmDataListener {
        /**
         * @param pcmData      原始 PCM 字节
         * @param sampleRateHz 采样率（如 44100 / 48000）
         * @param channelCount 声道数（1=单声道, 2=立体声）
         * @param encoding     编码，通常 [C.ENCODING_PCM_16BIT]
         */
        fun onPcmData(pcmData: ByteArray, sampleRateHz: Int, channelCount: Int, encoding: Int)
    }

    companion object {
        private const val TAG = "PcmTeeAudioProcessor"
    }
}
