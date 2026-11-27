package com.abhi.ganasuno.player

import kotlin.math.pow
import kotlin.math.roundToInt

/** Streaming feedback-comb / all-pass reverb for decoded mono or stereo PCM16. */
class SoftwareReverb(private val rate: Int, private val channels: Int, preset: Int) {
    data class Profile(val decay: Double, val scale: Double, val damping: Float, val diffusion: Float)
    companion object {
        fun profile(preset: Int): Profile = when (preset) {
            1 -> Profile(.45, .65, .50f, .48f)
            2 -> Profile(.75, .85, .45f, .55f)
            3 -> Profile(1.15, 1.0, .42f, .60f)
            4 -> Profile(1.8, 1.15, .36f, .65f)
            5 -> Profile(3.1, 1.35, .33f, .68f)
            6 -> Profile(2.2, .95, .22f, .72f)
            7 -> Profile(6.0, 1.65, .30f, .70f)
            else -> error("Select reverb preset 1–7")
        }
    }
    init { require(rate in 8000..192000 && channels in 1..2) }
    private val p = profile(preset)
    private fun frames(ms: Double) = (rate * ms / 1000).roundToInt().coerceAtLeast(1)
    private class Comb(size: Int, private val feedback: Float, private val damping: Float) {
        private val data = FloatArray(size)
        private var index = 0
        private var low = 0f
        fun tick(input: Float): Float {
            val delayed = data[index]
            low = delayed * (1f - damping) + low * damping
            data[index] = input + low * feedback
            if (++index == data.size) index = 0
            return delayed
        }
    }
    private class AllPass(size: Int, private val gain: Float) {
        private val data = FloatArray(size)
        private var index = 0
        fun tick(input: Float): Float {
            val delayed = data[index]
            val output = delayed - gain * input
            data[index] = input + gain * output
            if (++index == data.size) index = 0
            return output
        }
    }
    private val combs = Array(channels) { ch ->
        doubleArrayOf(29.7, 37.1, 41.1, 43.7, 53.3, 61.7).map { ms ->
            val n = frames(ms * p.scale + ch * 1.3)
            val feedback = 10.0.pow(-3.0 * n / rate / p.decay).toFloat().coerceAtMost(.98f)
            Comb(n, feedback, p.damping)
        }
    }
    private val diffusers = Array(channels) { ch ->
        doubleArrayOf(5.0, 7.7, 11.3).map { AllPass(frames(it * p.scale + ch * .4), p.diffusion) }
    }
    fun process(samples: ShortArray, mix: Int) {
        require(samples.size % channels == 0)
        val wet = mix.coerceIn(0, 100) / 100f
        for (frame in samples.indices step channels) {
            for (ch in 0 until channels) {
                val i = frame + ch
                val dry = samples[i] / 32768f
                var tail = 0f
                for (comb in combs[ch]) tail += comb.tick(dry * .22f)
                tail /= combs[ch].size
                for (ap in diffusers[ch]) tail = ap.tick(tail)
                if (wet > 0f) samples[i] = ((dry * (1f - wet * .5f) + tail * wet) * 32768f).roundToInt().coerceIn(-32768, 32767).toShort()
            }
        }
    }
}
