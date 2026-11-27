package com.abhi.ganasuno.data

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.*

/** Local, transparent analytics and safe user-imported profile helpers. */
object Batch13 {
    fun year(raw:String?):Int? = raw?.trim()?.let { Regex("^((?:19|20)\\d{2}|2100)(?:$|[-/ .].*)").matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }
    fun decades(samples:List<LibraryStore.Listen>,years:Map<String,Int>):Map<Int?,Long> = samples.filter{it.ms>0}.groupBy { years[it.song.id]?.takeIf{y->y in 1900..2100}?.let{y->y/10*10} }.mapValues{(_,v)->v.sumOf{it.ms}}
    fun key(s:String)=s.trim().lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+")," ").trim()
    fun profiles(names:List<String>,query:String):List<String> { val words=key(query).split(' ').filter{it.isNotBlank()};return names.filter{n->words.all{key(n).contains(it)}}.sortedWith(compareBy<String>{if(key(it).startsWith(key(query)))0 else 1}.thenBy{key(it)}) }
    fun profileName(s:String)=s.trim().takeIf{it.length in 1..100&&it.none{c->c.isISOControl()}}
    data class Metrics(val consistency:Int,val discovery:Int?,val diversity:Int,val concentration:Int,val replay:Int)
    fun metrics(current:List<LibraryStore.Listen>,prior:List<LibraryStore.Listen>,days:Int,zone:ZoneId=ZoneId.systemDefault()):Metrics? {
        val h=current.filter{it.ms>0};if(h.isEmpty())return null
        val total=h.sumOf{it.ms}.toDouble();val tracks=h.groupBy{it.song.id};val artists=h.groupBy{key(it.song.artist).ifBlank{"unknown"}}
        fun percent(d:Double)=(d*100).roundToInt().coerceIn(0,100)
        val active=h.map{Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate()}.distinct().size
        val priorIds=prior.filter{it.ms>0}.map{it.song.id}.toSet()
        // Effective artist count relative to track count. A dominant artist lowers diversity.
        val entropy=-artists.values.sumOf{g->val p=g.sumOf{it.ms}/total;p*ln(p)}
        val diversity=if(tracks.size==1)0 else percent((exp(entropy)-1)/(tracks.size-1))
        return Metrics(percent(active.toDouble()/days.coerceAtLeast(active)),if(priorIds.isEmpty())null else percent(tracks.keys.count{it !in priorIds}.toDouble()/tracks.size),diversity,percent(artists.values.maxOf{g->g.sumOf{it.ms}}/total),percent(tracks.values.maxOf{g->g.sumOf{it.ms}}/total))
    }
    fun archetype(m:Metrics,tracks:Int,artists:Int):String=when {
        m.concentration>=60 && artists<=3 -> "Devotee"
        m.replay>=45 || tracks<=5 -> "Deep Diver"
        artists>=15 && m.diversity>=55 && m.concentration<=25 -> "Omnivore"
        (m.discovery?:0)>=60 && tracks>=10 -> "Explorer"
        else -> "Regular"
    }
    fun fraction(position:Long,duration:Long):Float=if(duration<=0)0f else (position.coerceIn(0,duration).toDouble()/duration).toFloat()
}
