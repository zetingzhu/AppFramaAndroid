package com.trade.appframe11

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.ui.PlayerView
import com.trade.appframe11.audio.AudioAiPipelineManager
import com.trade.appframe11.audio.SubtitleOverlayView
import com.trade.appframe11.audio.engines.AsrEngineType

@UnstableApi
class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private var pipelineManager: AudioAiPipelineManager? = null

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ---------- 初始化 AI 管线 ----------
        val subtitleOverlay = findViewById<SubtitleOverlayView>(R.id.subtitle_overlay)
        val engineStatus = findViewById<TextView>(R.id.engine_status)

        pipelineManager = AudioAiPipelineManager(this).also { pm ->
            pm.onEngineChanged = { type, success ->
                engineStatus.text = if (success) "●" else "✗"
                engineStatus.setTextColor(
                    if (success) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
                )
            }
            pm.start(subtitleOverlay)
        }

        // ---------- 引擎选择 Spinner ----------
        setupEngineSpinner()

        // ---------- 初始化 ExoPlayer ----------
        val teeProcessor = pipelineManager!!.getTeeAudioProcessor()

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: android.content.Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): androidx.media3.exoplayer.audio.AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(teeProcessor))
                    .build()
            }
        }

        player = ExoPlayer.Builder(this, renderersFactory)
            .build()
            .also { exo ->
                findViewById<PlayerView>(R.id.player_view).player = exo

                val mediaItem = MediaItem.fromUri(LIVE_STREAM_URL)
                exo.setMediaItem(mediaItem)

                exo.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "播放错误: ${error.message}", error)
                    }
                })

                exo.prepare()
                exo.playWhenReady = true
            }
    }

    private fun setupEngineSpinner() {
        val spinner = findViewById<Spinner>(R.id.engine_spinner)
        val engines = AsrEngineType.entries.toTypedArray()
        val names = engines.map { it.displayName }

        // 使用自定义布局
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val selected = engines[pos]
                if (selected != pipelineManager?.currentEngineType) {
                    pipelineManager?.switchEngine(selected)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    @OptIn(UnstableApi::class)
    override fun onStart() {
        super.onStart()
        player?.playWhenReady = true
        val subtitleOverlay = findViewById<SubtitleOverlayView>(R.id.subtitle_overlay)
        pipelineManager?.resume(subtitleOverlay)
    }

    @OptIn(UnstableApi::class)
    override fun onStop() {
        super.onStop()
        player?.playWhenReady = false
        pipelineManager?.pause()
    }

    @OptIn(UnstableApi::class)
    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
        pipelineManager?.stop()
        pipelineManager = null
    }

    companion object {
        private const val TAG = "MainActivity"

        /** 直播流/视频地址 */
//      const val LIVE_STREAM_URL = "rtmp://live-play.xtsdtredy.com/live/user_live?txSecret=3777e7a63ffcd91ab99afae43afd9d73&txTime=69bbbd86"
        const val LIVE_STREAM_URL =
            "https://oss.xtrendspeed.com/video/classRoom/how_use_credit-en.mp4"
    }
}