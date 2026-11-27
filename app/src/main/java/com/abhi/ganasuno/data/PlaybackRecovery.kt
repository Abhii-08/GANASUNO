package com.abhi.ganasuno.data

import java.net.URI

/** Provider-independent playback validation and recovery policy. */
object PlaybackRecovery {
    fun validMediaUrl(raw: String?): Boolean = runCatching {
        val u = URI(raw.orEmpty())
        u.scheme.equals("https", true) && !u.host.isNullOrBlank() && u.rawUserInfo == null
    }.getOrDefault(false)

    fun expired(raw: String, nowMs: Long): Boolean = runCatching {
        val seconds = URI(raw).rawQuery.orEmpty().split('&').firstOrNull { it.startsWith("expire=") }?.substringAfter('=')?.toLongOrNull()
        seconds != null && seconds <= nowMs / 1000 + 30
    }.getOrDefault(false)

    fun transient(code: Int, http: Int?, restricted: Boolean): Boolean {
        if (restricted) return false
        if (http != null) return http == 408 || http in 500..599
        return code in setOf(2001, 2002)
    }

    fun description(code: Int, http: Int?, lookup: Boolean, restricted: Boolean): String = when {
        restricted || http == 401 || http == 403 -> "The music provider denied access to this stream."
        http == 429 -> "The music provider is rate-limiting requests. Please try again later."
        http == 404 || http == 410 -> "The media is no longer available at this address."
        http != null -> "The media server returned HTTP $http."
        code == 2001 -> "Unable to connect to the media server. Check your internet connection."
        code == 2002 -> "The media request timed out. Please try again."
        lookup -> "The music provider did not supply a playable stream."
        code == 2003 || code in 3001..3004 -> "The media format is invalid or unsupported."
        code in 4001..4005 -> "The device could not decode this media format."
        code in 5001..5003 -> "The device could not initialize audio output."
        code == 2005 || code == 2006 -> "The audio file is missing or cannot be accessed."
        else -> "Unable to play this track."
    }
}

class StreamLookupFailure(cause: Throwable) : java.io.IOException("Stream lookup failed", cause)
