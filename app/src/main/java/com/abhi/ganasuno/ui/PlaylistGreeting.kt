package com.abhi.ganasuno.ui

/** Pure logic shared by the animation and JVM tests. */
object PlaylistGreeting {
    const val SOURCE_KEY = "ganasuno.playback.source.v37"
    const val FRONT_MS = 6000L
    const val BACK_MS = 3000L
    const val FLIP_MS = 750L
    fun source(value: String?) = value?.trim()?.takeIf { it.isNotEmpty() } ?: "Now Playing"
    fun greeting(hour: Int): String = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }
    private val messages = listOf("Enjoy Your Music", "Music Is Life", "Stay Inspired", "Feel the Music", "Find Your Rhythm", "Keep Vibing", "Take a Musical Break", "Relax and Unwind", "Let the Music Play", "Your Daily Dose of Music", "Good Vibes Only", "Lost in the Melody", "Music for Your Mood", "Make Today Beautiful", "A Little Music, A Better Day")
    fun reverse(hour: Int, cycle: Int): String = if (cycle % 2 == 0) greeting(hour) else messages[(cycle / 2).mod(messages.size)]
    // The card reaches edge-on, swaps faces, then returns to a readable orientation.
    fun angle(fraction: Float): Float = if (fraction < .5f) fraction * 180f else (fraction - 1f) * 180f
}
