package com.abhi.ganasuno.data

import java.time.Instant
import java.time.ZoneId

/** Calculations over the actual local playback samples, not play-count estimates. */
object Insights {
    fun report(h: List<LibraryStore.Listen>, previous: List<LibraryStore.Listen>, days: Int, albumTags:Map<String,String> = emptyMap()): String {
        if (h.isEmpty()) return "No listening data in this period."
        val zone = ZoneId.systemDefault()
        val dated = h.groupBy { Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() }
        val hourly = h.groupBy { Instant.ofEpochMilli(it.at).atZone(zone).hour }.mapValues { it.value.sumOf { r -> r.ms } }
        val total = h.sumOf { it.ms }
        val groups = h.groupBy { it.song.id }.values.sortedByDescending { it.sumOf { r -> r.ms } }
        val artist = h.groupBy { it.song.artist.ifBlank { "Unknown" } }.entries.sortedByDescending { it.value.sumOf { r -> r.ms } }
        return buildString {
            appendLine("Listening time: ${total / 60000} min · ${groups.size} tracks")
            appendLine("Active days: ${dated.size} · Daily average: ${total / 60000 / (if (days > 0) days else dated.size.coerceAtLeast(1))} min")
            appendLine("Busiest day: ${dated.maxByOrNull { it.value.sumOf { r -> r.ms } }?.key}")
            appendLine("Busiest hour: %02d:00".format(hourly.maxByOrNull { it.value }?.key ?: 0))
            if (days > 0) appendLine("Previous period: ${previous.sumOf { it.ms } / 60000} min · Difference: ${(total - previous.sumOf { it.ms }) / 60000} min")
            appendLine("\nListening clock (minutes)")
            for (hour in 0..23) { val ms = hourly[hour] ?: 0L; if (ms > 0) appendLine("%02d:00  %d min".format(hour, ms / 60000)) }
            appendLine("\nTop songs")
            groups.take(10).forEach { appendLine("${it.first().song.title} · ${it.sumOf { r -> r.ms } / 60000} min") }
            appendLine("\nTop artists")
            artist.take(10).forEach { appendLine("${it.key} · ${it.value.sumOf { r -> r.ms } / 60000} min") }
            appendLine("\nTop albums (known metadata)")
            val albumGroups=BatchCore.albums(h.map{BatchCore.AlbumSample(albumTags[it.song.id]?:it.song.album,it.song.artist,it.ms)})
            if(albumGroups.isEmpty())appendLine("No album metadata yet. Set an album name from a song menu.")
            albumGroups.take(10).forEach{appendLine("${it.first} · ${it.second/60000} min")}
            val unknown=h.filter{(albumTags[it.song.id]?:it.song.album).isBlank()}.sumOf{it.ms};if(unknown>0)appendLine("Unknown album · ${unknown/60000} min")
            val span=if(days>0)days else (java.time.temporal.ChronoUnit.DAYS.between(dated.keys.minOrNull()!!,dated.keys.maxOrNull()!!)+1).toInt().coerceAtLeast(1)
            Batch13.metrics(h,previous,span,zone)?.let{m->
                appendLine("Listener type (GANASUNO local heuristic): ${Batch13.archetype(m,groups.size,artist.size)}")
                appendLine("\nListening fingerprint · local metrics")
                appendLine("Consistency: ${m.consistency}% active days")
                appendLine("Discovery: ${m.discovery?.let{"$it% new versus comparison period"}?:"No prior history"}")
                appendLine("Diversity: ${m.diversity}% effective artist diversity")
                appendLine("Concentration: ${m.concentration}% top artist time share")
                appendLine("Replay: ${m.replay}% top track time share")
            }
            appendLine("\nArtist diversity: ${artist.size} · Top track share: ${groups.first().sumOf { it.ms } * 100 / total.coerceAtLeast(1)}%")
        }
    }
}
