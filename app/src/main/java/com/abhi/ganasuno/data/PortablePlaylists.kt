package com.abhi.ganasuno.data

/** Portable import for user-converted playlists containing YouTube video IDs or URLs. */
object PortablePlaylists {
    private val id = Regex("[A-Za-z0-9_-]{11}")
    fun parse(raw: String): List<String> = raw.lineSequence().flatMap { line ->
        val found = Regex("https?://(?:www\\.|music\\.)?(?:youtube\\.com/watch\\?[^\\s,;]*?v=|youtu\\.be/)([A-Za-z0-9_-]{11})(?![A-Za-z0-9_-])").findAll(line).map { it.groupValues[1] }
        val bare = line.trim().takeIf { id.matches(it) }
        if (bare != null) sequenceOf(bare) else found
    }.distinct().take(500).toList()
    fun export(ids: List<String>): String = ids.filter { id.matches(it) }.distinct().joinToString("\n") { "https://music.youtube.com/watch?v=$it" }
}
