package com.trade.appframe11

import android.os.Bundle
import android.util.Log
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
import androidx.media3.exoplayer.audio.AudioProcessor
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.ui.PlayerView
import com.trade.appframe11.audio.AudioAiPipelineManager
import com.trade.appframe11.audio.SubtitleOverlayView

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
        pipelineManager = AudioAiPipelineManager(this).also { pm ->
            pm.start(subtitleOverlay)
        }

        // ---------- 初始化 ExoPlayer ----------
        val teeProcessor = pipelineManager!!.getTeeAudioProcessor()

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: android.content.Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf<AudioProcessor>(teeProcessor))
                    .build()
            }
        }

        player = ExoPlayer.Builder(this, renderersFactory)
            .build()
            .also { exo ->
                // 绑定 PlayerView
                findViewById<PlayerView>(R.id.player_view).player = exo

                // 设置 RTMP 直播流
                val mediaItem = MediaItem.fromUri(LIVE_STREAM_URL)
                exo.setMediaItem(mediaItem)

                // 错误监听
                exo.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "播放错误: ${error.message}", error)
                    }
                })

                exo.prepare()
                exo.playWhenReady = true
            }
    }

    @OptIn(UnstableApi::class)
    override fun onStart() {
        super.onStart()
        player?.playWhenReady = true
    }

    @OptIn(UnstableApi::class)
    override fun onStop() {
        super.onStop()
        player?.playWhenReady = false
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

        /** RTMP 直播流地址 */
        private const val LIVE_STREAM_URL =
            "rtmp://live-play.xtsdtredy.com/live/user_live?txSecret=3777e7a63ffcd91ab99afae43afd9d73&txTime=69bbbd86"
    }
}