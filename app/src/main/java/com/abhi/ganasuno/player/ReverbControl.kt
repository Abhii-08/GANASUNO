package com.abhi.ganasuno.player

/** Software reverb settings are read by the PCM processor on its audio thread. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object ReverbControl {
    fun apply():Boolean {
        EffectsSettings.refreshReverb()
        return true
    }
    fun release() { /* The processor clears its buffers on flush/reset. */ }
}
