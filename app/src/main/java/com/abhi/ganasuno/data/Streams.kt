package com.abhi.ganasuno.data

import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import java.util.concurrent.ConcurrentHashMap

class Resolved(
    val url: String,
    val live: Boolean,
)

class Sub(
    val url: String,
    val lang: String,
)

class VideoSources(
    val videoUrl: String?,
    val audioUrl: String?,
    val hlsUrl: String?,
    val subtitles: List<Sub>,
)

/** Turns a YouTube video id into playable URLs (the extractor handles signatures / throttling). */
object Streams {
    private const val TTL_MS = 15 * 60 * 1000L
    private val cache = ConcurrentHashMap<String, Pair<Resolved, Long>>()
    private val xtags = Regex("[?&]xtags=([^&]+)")

    // H.264 (avc1) mp4 video-only itags: 144p..1080p (+60fps variants). They play on every phone.
    private val avcVideoItags = setOf(160, 133, 134, 135, 136, 137, 298, 299)

    private fun info(videoId: String): StreamInfo =
        StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$videoId")

    fun invalidate(videoId: String) { cache.keys.filter { it.startsWith("$videoId|") }.forEach { cache.remove(it) } }

    /** Audio (or the HLS playlist for a live stream), chosen by the quality + language settings. */
    fun resolve(videoId: String): Resolved {
        val quality = Prefs.audioQuality
        val lang = Prefs.audioLang
        val key = "$videoId|$quality|$lang"
        cache[key]?.let { (res, time) ->
            if (System.currentTimeMillis() - time < TTL_MS && !PlaybackRecovery.expired(res.url, System.currentTimeMillis())) return res
        }

        val info = info(videoId)
        val audio = info.audioStreams
        val hls = info.hlsUrl.orEmpty()

        val best = pickAudio(audio.orEmpty(), quality, lang)
        // Use only URLs returned by the provider/extractor. A progressive combined stream
        // can still supply audio when the provider offers no separate audio stream.
        val muxed = info.videoStreams.orEmpty()
            .filter { PlaybackRecovery.validMediaUrl(it.content) && !PlaybackRecovery.expired(it.content, System.currentTimeMillis()) }
            .minByOrNull { heightOf(it).takeIf { height -> height > 0 } ?: Int.MAX_VALUE }
        val resolved = when {
            best != null -> Resolved(best.content, false)
            PlaybackRecovery.validMediaUrl(hls) && !PlaybackRecovery.expired(hls, System.currentTimeMillis()) -> Resolved(hls, true)
            muxed != null -> Resolved(muxed.content, false)
            else -> throw IllegalStateException("No playable media stream was returned")
        }
        cache[key] = resolved to System.currentTimeMillis()
        return resolved
    }

    /** Provider-reported fields only; do not expose signed stream URLs. */
    fun describe(videoId:String):String {
        val info=info(videoId);val best=pickAudio(info.audioStreams.orEmpty(),Prefs.audioQuality,Prefs.audioLang)
        fun optional(name:String):String?=runCatching{info.javaClass.getMethod(name).invoke(info)?.toString()}.getOrNull()?.takeIf{it.isNotBlank()&&it!="-1"&&it!="null"}
        val format=Batch15.streamFormat(best?.format?.suffix,best?.itagItem?.id)
        return buildString{appendLine("Codec estimate: ${format.first}");appendLine("MIME estimate: ${format.second}");appendLine("Selected audio: ${best?.format?.suffix?:"Unknown / live"}");appendLine("Average bitrate: ${best?.averageBitrate?.takeIf{it>0}?.let{"$it kbps"}?:"Unknown"}");appendLine("itag: ${best?.itagItem?.id?:"Unknown"}");appendLine("Views: ${Batch22.count(optional("getViewCount"))}");appendLine("Likes: ${Batch22.count(optional("getLikeCount"))}");appendLine("Dislikes: ${Batch22.count(optional("getDislikeCount"))}");appendLine("Published: ${optional("getTextualUploadDate")?:"Unavailable"}");appendLine("Provider data may be incomplete.")}
    }

    /** Video (up to the chosen height) + audio + subtitles for the video player. */
    fun videoSources(videoId: String): VideoSources {
        val info = info(videoId)
        val hls = info.hlsUrl
        if (info.audioStreams.isNullOrEmpty() && !hls.isNullOrEmpty()) {
            return VideoSources(null, null, hls, emptyList())
        }

        val limit = Prefs.videoHeight
        val subs =
            info.subtitles
                .orEmpty()
                .filter { it.format?.suffix == "vtt" && !it.content.isNullOrEmpty() }
                .map { Sub(it.content, it.languageTag ?: "") }

        val videoOnly =
            info.videoOnlyStreams
                .orEmpty()
                .filter { avcVideoItags.contains(it.itagItem?.id ?: -1) && heightOf(it) in 1..limit }
                .maxByOrNull { heightOf(it) }

        if (videoOnly != null) {
            val audio = pickAudio(info.audioStreams.orEmpty(), Prefs.Q_NORMAL, Prefs.audioLang)
            if (audio != null) return VideoSources(videoOnly.content, audio.content, null, subs)
        }

        // fallback: a muxed stream (video + audio together, usually 360p or 720p)
        val muxed =
            info.videoStreams
                .orEmpty()
                .filter { heightOf(it) in 1..limit }
                .maxByOrNull { heightOf(it) }
                ?: info.videoStreams.orEmpty().firstOrNull()
                ?: throw IllegalStateException("No video stream")
        return VideoSources(muxed.content, null, null, subs)
    }

    private fun heightOf(stream: VideoStream): Int =
        Regex("(\\d+)p").find(stream.resolution ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0

    /**
     * A video can carry several audio tracks (dubs) under the same itag. The URL names its track in
     * `xtags` (e.g. acont=original:lang=en-US). Rank 0 = preferred language, 1 = original, 2 = rest.
     */
    private fun trackRank(
        url: String,
        preferred: String,
    ): Int {
        val tags =
            xtags
                .find(url)
                ?.groupValues
                ?.get(1)
                ?.replace("%3D", "=", ignoreCase = true)
                ?.replace("%3A", ":", ignoreCase = true)
                ?.split(':')
                ?.associate { it.substringBefore('=') to it.substringAfter('=', "") }
                .orEmpty()
        val acont = tags["acont"]
        val lang = tags["lang"]?.substringBefore('-')
        return when {
            preferred.isNotBlank() && acont != "descriptive" &&
                lang.equals(preferred.substringBefore('-'), ignoreCase = true) -> 0
            acont == null || acont == "original" -> 1
            else -> 2
        }
    }

    private fun pickAudio(
        audio: List<AudioStream>,
        quality: Int,
        lang: String,
    ): AudioStream? {
        val usable = audio.filter { PlaybackRecovery.validMediaUrl(it.content) && !PlaybackRecovery.expired(it.content, System.currentTimeMillis()) }
        if (usable.isEmpty()) return null

        val bestRank = usable.minOf { trackRank(it.content, lang) }
        val pool = usable.filter { trackRank(it.content, lang) == bestRank }

        return when (quality) {
            Prefs.Q_LOW ->
                pool.filter { it.averageBitrate > 0 }.minByOrNull { it.averageBitrate } ?: pool.first()
            Prefs.Q_320 ->
                V8Core.preferredBitrate(pool.map { it.averageBitrate })?.let { rate -> pool.firstOrNull { it.averageBitrate == rate } } ?: pool.first()
            Prefs.Q_HIGH ->
                pool.maxByOrNull { it.averageBitrate } ?: pool.first()
            else ->
                // itag 140 = AAC 128k (plays everywhere), 251 = Opus
                pool.firstOrNull { it.itagItem?.id == 140 }
                    ?: pool.firstOrNull { it.itagItem?.id == 251 }
                    ?: pool.maxByOrNull { it.averageBitrate }
                    ?: pool.first()
        }
    }
}
