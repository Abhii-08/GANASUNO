package com.abhi.ganasuno.data

import java.net.URI
import java.util.Locale

/** Pure helpers shared by the v8 discovery and library tools. */
object V8Core {
    fun playlistId(raw: String): String? {
        val value = raw.trim()
        if (value.matches(Regex("[A-Za-z0-9_-]{10,100}"))) return value
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        if (uri.scheme != "https" || uri.host?.lowercase(Locale.ROOT) !in setOf("youtube.com", "www.youtube.com", "music.youtube.com") || uri.userInfo != null || uri.port != -1) return null
        return uri.rawQuery?.split('&')?.firstOrNull { it.startsWith("list=") }?.substringAfter('=')?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{10,100}")) }
    }
    fun httpsUrl(raw: String): String? {
        val value = raw.trim().takeIf { it.length <= 2000 } ?: return null
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        return value.takeIf { uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null }
    }
    fun related(artist: String, groups: List<List<String>>): List<String> {
        val key = artist.trim().lowercase(Locale.ROOT)
        if (key.isBlank()) return emptyList()
        val counts = linkedMapOf<String, Pair<String, Int>>()
        groups.forEach { group ->
            val cleaned = group.map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase(Locale.ROOT) }
            if (cleaned.any { it.lowercase(Locale.ROOT) == key }) cleaned.filterNot { it.lowercase(Locale.ROOT) == key }.forEach { name ->
                val k = name.lowercase(Locale.ROOT)
                counts[k] = (counts[k]?.first ?: name) to ((counts[k]?.second ?: 0) + 1)
            }
        }
        return counts.values.sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first.lowercase(Locale.ROOT) }).take(30).map { it.first }
    }
    fun preferredBitrate(values: List<Int>): Int? = values.filter { it > 0 }.minWithOrNull(compareBy<Int> { kotlin.math.abs(it.toLong() - 320L) }.thenByDescending { it })
}
