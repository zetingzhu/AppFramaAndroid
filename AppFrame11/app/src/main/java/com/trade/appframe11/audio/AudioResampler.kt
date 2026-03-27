package com.trade.appframe11.audio



/**
 * 预处理层 —— 音频重采样 & 声道合并。
 *
 * 将 ExoPlayer 采集到的原始 PCM（通常 44.1kHz / 48kHz 立体声 16-bit）
 * 转换为 MediaPipe 模型所需的 **16 kHz 单声道 Float32** 格式。
 *
 * 算法：
 * 1. 声道合并 —— 双声道取左右平均 → 单声道
 * 2. 线性插值重采样 → 目标采样率 (默认 16000)
 */
class AudioResampler(
    private val targetSampleRate: Int = TARGET_SAMPLE_RATE
) {

    /**
     * 处理一帧 PCM 数据。
     *
     * @param pcmData        原始 PCM 字节（Little-Endian 16-bit）
     * @param srcSampleRate  源采样率
     * @param srcChannels    源声道数
     * @return 16 kHz 单声道 Float32 数组（范围 -1.0 .. 1.0），若输入无效返回 null
     */
    fun process(pcmData: ByteArray, srcSampleRate: Int, srcChannels: Int): FloatArray? {
        if (pcmData.isEmpty() || srcSampleRate <= 0 || srcChannels <= 0) return null

        // Step 1: bytes → 16-bit shorts
        val shortCount = pcmData.size / 2
        if (shortCount == 0) return null
        val shorts = ShortArray(shortCount)
        for (i in 0 until shortCount) {
            val lo = pcmData[i * 2].toInt() and 0xFF
            val hi = pcmData[i * 2 + 1].toInt()
            shorts[i] = ((hi shl 8) or lo).toShort()
        }

        // Step 2: 声道合并 → 单声道
        val monoSamples = if (srcChannels >= 2) {
            mixToMono(shorts, srcChannels)
        } else {
            shorts
        }

        // Step 3: 重采样 → targetSampleRate
        val resampled = if (srcSampleRate != targetSampleRate) {
            linearResample(monoSamples, srcSampleRate, targetSampleRate)
        } else {
            monoSamples
        }

        // Step 4: Short → Float32 (归一化到 -1.0 .. 1.0)
        val floatArray = FloatArray(resampled.size) { resampled[it].toFloat() / Short.MAX_VALUE }

        return floatArray
    }

    /** 多声道混合为单声道：每 [channels] 个样本取平均值 */
    private fun mixToMono(samples: ShortArray, channels: Int): ShortArray {
        val frameCount = samples.size / channels
        val mono = ShortArray(frameCount)
        for (i in 0 until frameCount) {
            var sum = 0L
            for (ch in 0 until channels) {
                sum += samples[i * channels + ch]
            }
            mono[i] = (sum / channels).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return mono
    }

    /** 线性插值重采样 */
    private fun linearResample(input: ShortArray, srcRate: Int, dstRate: Int): ShortArray {
        if (input.isEmpty()) return input
        val ratio = srcRate.toDouble() / dstRate.toDouble()
        val outLen = (input.size / ratio).toInt()
        if (outLen <= 0) return ShortArray(0)
        val output = ShortArray(outLen)
        for (i in 0 until outLen) {
            val srcIdx = i * ratio
            val idx0 = srcIdx.toInt().coerceAtMost(input.size - 1)
            val idx1 = (idx0 + 1).coerceAtMost(input.size - 1)
            val frac = srcIdx - idx0
            val value = input[idx0] * (1.0 - frac) + input[idx1] * frac
            output[i] = value.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    companion object {
        private const val TAG = "AudioResampler"
        const val TARGET_SAMPLE_RATE = 16000
    }
}
