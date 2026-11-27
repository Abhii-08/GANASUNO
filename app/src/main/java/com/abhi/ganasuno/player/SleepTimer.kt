package com.abhi.ganasuno.player

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.Player

/** Sleep timer: after N minutes the volume fades out over ~8 s and playback pauses. Or stop after this song. */
object SleepTimer {
    private var fadeMs = 8000L
    private const val STEP_MS = 400L

    private val handler = Handler(Looper.getMainLooper())
    private var fadeLevel = 1f

    var endsAt: Long = 0L
        private set
    var untilSongEnd: Boolean = false
        private set

    val isActive: Boolean get() = endsAt != 0L || untilSongEnd

    private val fadeStep =
        object : Runnable {
            override fun run() {
                val p = PlayerHub.player
                if (p == null) {
                    cancel()
                    return
                }
                fadeLevel -= STEP_MS.toFloat() / fadeMs
                if (fadeLevel <= 0f) {
                    p.pause()
                    cancel()
                    return
                }
                p.volume = fadeLevel * PlayerHub.baseVolume()
                handler.postDelayed(this, STEP_MS)
            }
        }

    private val songEndListener =
        object : Player.Listener {
            override fun onPlayWhenReadyChanged(
                playWhenReady: Boolean,
                reason: Int,
            ) {
                if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) cancel()
            }
        }

    fun startMinutes(minutes: Int) {
        PlayerHub.cancelTransition?.invoke()
        ManualFade.cancel()
        cancel()
        fadeMs = com.abhi.ganasuno.data.LibraryStore.option("sleep_fade",8000).coerceIn(2000,15000).toLong()
        val total = minutes * 60_000L
        endsAt = SystemClock.elapsedRealtime() + total
        handler.postDelayed(fadeStep, (total - fadeMs).coerceAtLeast(0L))
    }

    fun startEndOfSong() {
        PlayerHub.cancelTransition?.invoke()
        ManualFade.cancel()
        cancel()
        val p = PlayerHub.player ?: return
        untilSongEnd = true
        p.pauseAtEndOfMediaItems = true
        p.addListener(songEndListener)
    }

    fun cancel() {
        handler.removeCallbacks(fadeStep)
        fadeLevel = 1f
        PlayerHub.player?.let {
            it.volume = PlayerHub.baseVolume()
            it.pauseAtEndOfMediaItems = false
            it.removeListener(songEndListener)
        }
        endsAt = 0L
        untilSongEnd = false
    }

    fun remainingMs(): Long =
        if (endsAt == 0L) 0L else (endsAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
}
