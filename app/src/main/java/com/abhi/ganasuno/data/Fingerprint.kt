package com.abhi.ganasuno.data

import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/** Transparent local metrics; replay uses listening-time share of the top track, not guessed play counts. */
object Fingerprint {
    data class Scores(val consistency:Int,val discovery:Int?,val diversity:Int,val concentration:Int,val replay:Int)
    fun calculate(current:List<LibraryStore.Listen>,prior:List<LibraryStore.Listen>,days:Int):Scores? {
        val valid=current.filter{it.ms>0};if(valid.isEmpty())return null
        val total=valid.sumOf{it.ms}.toDouble();val tracks=valid.groupBy{it.song.id};val artists=valid.groupBy{it.song.artist.trim().lowercase().ifBlank{"unknown"}}
        val known=prior.map{it.song.id}.toSet();val dates=valid.map{Instant.ofEpochMilli(it.at).atZone(ZoneId.systemDefault()).toLocalDate()}.distinct().size
        fun percent(v:Double)=(v*100).roundToInt().coerceIn(0,100)
        return Scores(percent(dates.toDouble()/days.coerceAtLeast(dates)),if(prior.isEmpty())null else percent(tracks.keys.count{it !in known}.toDouble()/tracks.size),percent(artists.size.toDouble()/tracks.size),percent(artists.values.maxOf{g->g.sumOf{it.ms}}/total),percent(tracks.values.maxOf{g->g.sumOf{it.ms}}/total))
    }
    fun describe(s:Scores)=buildString{appendLine("Consistency: ${s.consistency}% of days active");appendLine("Discovery: ${s.discovery?.let{"$it% new vs comparison period"}?:"Not enough prior history"}");appendLine("Diversity: ${s.diversity}% artist-to-track ratio");appendLine("Concentration: ${s.concentration}% of time on top artist");appendLine("Replay focus: ${s.replay}% of time on top track")}
}
