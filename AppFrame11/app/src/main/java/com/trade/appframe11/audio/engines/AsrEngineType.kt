package com.trade.appframe11.audio.engines

/**
 * ASR 引擎类型枚举。
 */
enum class AsrEngineType(val displayName: String) {
    SHERPA_ONNX("Sherpa-ONNX"),
    VOSK("Vosk"),
}
