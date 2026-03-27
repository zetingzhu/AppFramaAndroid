package com.trade.appframe11.audio

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifierResult
import com.google.mediapipe.tasks.components.containers.AudioData
import com.google.mediapipe.tasks.core.BaseOptions

/**
 * 推理层 —— 封装 MediaPipe AudioClassifier（基于 TFLite）。
 *
 * 接收 16 kHz 单声道 Float32 音频数据，运行音频分类推理，
 * 并将 Top-K 结果回调给调用方。
 *
 * 默认模型文件：assets/yamnet.tflite
 * 如果模型文件不存在会优雅降级（仅输出警告日志，不崩溃）。
 */
class MediaPipeAudioInference(
    private val context: Context,
    private val modelAssetPath: String = DEFAULT_MODEL,
    private val maxResults: Int = 5,
    private val scoreThreshold: Float = 0.3f
) {

    private var classifier: AudioClassifier? = null
    private var isReady = false
    private var listener: OnClassificationListener? = null

    fun interface OnClassificationListener {
        fun onResult(results: List<ClassificationItem>)
    }

    data class ClassificationItem(
        val label: String,
        val score: Float
    )

    /** 设置分类结果回调 */
    fun setOnClassificationListener(l: OnClassificationListener?) {
        listener = l
    }

    /** 初始化 MediaPipe AudioClassifier。应在后台线程调用。 */
    fun initialize(): Boolean {
        return try {
            // 检查 assets 中是否存在模型文件
            val assetsList = context.assets.list("") ?: emptyArray()
            if (modelAssetPath !in assetsList) {
                Log.w(TAG, "模型文件 $modelAssetPath 不存在于 assets/ 中，推理层降级。")
                isReady = false
                return false
            }

            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(modelAssetPath)
                .build()

            val options = AudioClassifier.AudioClassifierOptions.builder()
                .setBaseOptions(baseOptions)
                .setMaxResults(maxResults)
                .setScoreThreshold(scoreThreshold)
                .build()

            classifier = AudioClassifier.createFromOptions(context, options)
            isReady = true
            Log.d(TAG, "AudioClassifier 初始化成功 — model=$modelAssetPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "AudioClassifier 初始化失败", e)
            isReady = false
            false
        }
    }

    /**
     * 对一段 16 kHz Float32 单声道音频执行分类推理。
     *
     * @param audioSamples 16kHz 单声道 Float32 数组
     */
    fun classify(audioSamples: FloatArray) {
        if (!isReady || classifier == null) return

        try {
            val audioData = AudioData.create(
                AudioData.AudioDataFormat.builder()
                    .setNumOfChannels(1)
                    .setSampleRate(AudioResampler.TARGET_SAMPLE_RATE.toFloat())
                    .build(),
                audioSamples.size
            )
            audioData.load(audioSamples)

            val result: AudioClassifierResult = classifier!!.classify(audioData)

            val items = mutableListOf<ClassificationItem>()
            for (classification in result.classificationResults()) {
                for (classificationEntry in classification.classifications()) {
                    for (category in classificationEntry.categories()) {
                        items.add(ClassificationItem(category.categoryName(), category.score()))
                    }
                }
            }

            if (items.isNotEmpty()) {
                Log.d(TAG, "分类结果: ${items.joinToString { "${it.label}(${String.format("%.2f", it.score)})" }}")
                listener?.onResult(items)
            }
        } catch (e: Exception) {
            Log.e(TAG, "分类推理异常", e)
        }
    }

    /** 释放资源 */
    fun close() {
        try {
            classifier?.close()
        } catch (_: Exception) {
        }
        classifier = null
        isReady = false
        Log.d(TAG, "AudioClassifier 已释放")
    }

    companion object {
        private const val TAG = "MediaPipeAudioInference"
        private const val DEFAULT_MODEL = "yamnet.tflite"
    }
}
