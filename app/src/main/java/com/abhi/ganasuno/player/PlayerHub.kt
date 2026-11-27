@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.abhi.ganasuno.player

import androidx.media3.exoplayer.ExoPlayer
import com.abhi.ganasuno.data.Prefs

/** Holds the service's player so settings (skip silence, normalize) and the sleep timer can reach it. */
object PlayerHub {
    var player: ExoPlayer? = null
    var extendQueueForNext: (((Boolean) -> Unit) -> Unit)? = null
    var roomActive=false
    var cancelTransition:(()->Unit)?=null
    var transitionActive:(()->Boolean)?=null


    fun applyAll() {
        val p = player ?: return
        p.skipSilenceEnabled = Prefs.skipSilence
        applyNormalize()
        ReverbControl.apply()
    }

    /** PCM loudness balancing is handled by EffectsProcessor. */
    fun baseVolume():Float = 1f

    fun applyNormalize() {

        val p = player ?: return
        EffectsSettings.refresh()
        if(transitionActive?.invoke()!=true)p.volume = baseVolume()
        // Adaptive normalization runs on PCM in EffectsProcessor, not a fixed hardware boost.

    }

    fun release() {
        ReverbControl.release()
        player = null
    }
}
