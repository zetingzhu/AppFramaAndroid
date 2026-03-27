package com.trade.appframe11.audio.engines

import android.content.Context

/**
 * 统一 ASR 引擎接口 —— 策略模式的核心抽象。
 *
 * 所有语音识别引擎（Sherpa-ONNX、Whisper、MediaPipe、Vosk）
 * 都实现此接口，使管线层可以透明切换引擎。
 */
interface AsrEngine {

    /** 引擎名称（用于日志和 UI 显示） */
    val name: String

    /** 引擎类型 */
    val type: AsrEngineType

    /** 是否为流式识别（边说边出字） */
    val isStreaming: Boolean

    /**
     * 初始化引擎。应在后台线程调用（可能耗时较长）。
     * @return true 表示初始化成功
     */
    fun initialize(context: Context): Boolean

    /**
     * 喂入一帧 16 kHz 单声道 Float32 音频数据。
     * 引擎内部决定何时触发识别回调。
     */
    fun feedAudio(samples: FloatArray)

    /**
     * 设置识别结果回调。
     * @param listener 识别出文本时调用，传入识别文字；null 取消回调
     */
    fun setOnTextListener(listener: ((String) -> Unit)?)

    /** 释放引擎资源。 */
    fun release()
}
