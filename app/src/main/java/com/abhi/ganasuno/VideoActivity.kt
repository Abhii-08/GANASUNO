package com.abhi.ganasuno

import android.net.Uri
import android.app.PictureInPictureParams
import android.util.Rational
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import com.abhi.ganasuno.data.Prefs
import com.abhi.ganasuno.data.Streams
import com.abhi.ganasuno.data.VideoSources
import com.abhi.ganasuno.data.YtApi
import com.abhi.ganasuno.databinding.ActivityVideoBinding
import java.util.concurrent.Executors

/**
 * Video player: up to 1080p (video-only + audio merged), with subtitles (CC button) and live streams.
 * Quality limit comes from Settings -> Video quality.
 */
@OptIn(UnstableApi::class)
class VideoActivity : AppCompatActivity() {
    private lateinit var b: ActivityVideoBinding
    private var player: ExoPlayer? = null
    private val io = Executors.newSingleThreadExecutor()
    private val ui = Handler(Looper.getMainLooper())
    private val communitySkipper=com.abhi.ganasuno.player.CommunitySkipper({player},{intent.getStringExtra(EXTRA_ID)})

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(b.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, b.root).let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val id = intent.getStringExtra(EXTRA_ID)
        if (id.isNullOrEmpty()) {
            finish()
            return
        }
        b.videoTitle.text = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        load(id)
    }

    private fun load(id: String) {
        b.loading.visibility = View.VISIBLE
        io.execute {
            val result = runCatching { Streams.videoSources(id) }
            ui.post {
                if (isDestroyed) return@post
                b.loading.visibility = View.GONE
                result
                    .onSuccess { start(it) }
                    .onFailure {
                        com.abhi.ganasuno.data.Diagnostics.event("Video stream lookup failed")
                        Toast.makeText(this, getString(R.string.video_failed, "The provider did not supply a playable stream."), Toast.LENGTH_LONG).show()
                        finish()
                    }
            }
        }
    }

    private fun start(src: VideoSources) {
        val http =
            DefaultHttpDataSource
                .Factory()
                .setUserAgent(YtApi.WEB_UA)
                .setAllowCrossProtocolRedirects(true)

        val hls = src.hlsUrl
        val source: MediaSource =
            if (hls != null) {
                HlsMediaSource.Factory(http).createMediaSource(MediaItem.fromUri(hls))
            } else {
                val progressive = ProgressiveMediaSource.Factory(http)
                val parts = ArrayList<MediaSource>()
                parts.add(progressive.createMediaSource(MediaItem.fromUri(src.videoUrl.orEmpty())))
                src.audioUrl?.let { parts.add(progressive.createMediaSource(MediaItem.fromUri(it))) }
                src.subtitles.forEach { sub ->
                    val config =
                        MediaItem.SubtitleConfiguration
                            .Builder(Uri.parse(sub.url))
                            .setMimeType(MimeTypes.TEXT_VTT)
                            .setLanguage(sub.lang)
                            .build()
                    parts.add(SingleSampleMediaSource.Factory(http).createMediaSource(config, C.TIME_UNSET))
                }
                if (parts.size == 1) parts[0] else MergingMediaSource(*parts.toTypedArray())
            }

        val p = ExoPlayer.Builder(this).build()
        p.trackSelectionParameters =
            p.trackSelectionParameters
                .buildUpon()
                .setPreferredTextLanguage(if (Prefs.audioLang == "hi") "hi" else "en")
                .setSelectUndeterminedTextLanguage(true)
                .build()
        p.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val failure = com.abhi.ganasuno.player.PlaybackFailures.inspect("video", error)
                com.abhi.ganasuno.data.Diagnostics.event("Video playback: ${failure.summary().replace('\n', ' ')}")
                android.util.Log.w("GANASUNO-Playback", failure.summary().replace('\n', ' '))
                if (!isFinishing && !isDestroyed) Toast.makeText(this@VideoActivity, failure.text, Toast.LENGTH_LONG).show()
            }
        })
        p.setMediaSource(source)
        p.prepare()
        p.playWhenReady = true
        b.player.player = p
        player = p
        communitySkipper.start()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (player?.isPlaying == true && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            runCatching { enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16,9)).build()) }
        }
    }

    override fun onStop() {
        if (!isInPictureInPictureMode) player?.pause()
        super.onStop()
    }

    override fun onDestroy() {
        communitySkipper.close()
        b.player.player = null
        player?.release()
        player = null
        io.shutdown()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
    }
}
