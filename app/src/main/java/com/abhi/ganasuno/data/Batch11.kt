package com.abhi.ganasuno.data

import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.random.Random

/** Local recommendations, intentionally independent of remote personalized accounts. */
object Batch11 {
    fun key(s:String)=s.trim().lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+")," ").trim()
    fun quick(history:List<LibraryStore.Listen>, favourites:List<Song>, limit:Int=12):List<Song> {
        val top=history.filter{it.ms>0}.groupBy{it.song.id}.values.sortedByDescending{g->g.sumOf{it.ms}}.map{it.last().song}
        return (top+favourites).distinctBy{it.id}.take(limit.coerceIn(0,50))
    }
    fun mix(history:List<LibraryStore.Listen>, favourites:List<Song>, candidates:List<Song>, limit:Int=40):List<Song> {
        val weights=history.filter{it.ms>0}.groupBy{key(it.song.artist)}.mapValues{it.value.sumOf{r->r.ms}}
        val favouritesArtists=favourites.map{key(it.artist)}.filter{it.isNotBlank()}.toSet()
        val recent=history.takeLast(40).map{it.song.id}.toSet()
        val pool=candidates.distinctBy{it.id}.sortedWith(compareByDescending<Song>{(weights[key(it.artist)]?:0L).coerceAtMost(3600000L)+(if(key(it.artist) in favouritesArtists)600000L else 0L)-(if(it.id in recent)180000L else 0L)}.thenBy{it.title})
        // Round-robin artist groups avoids a single artist monopolizing the mix.
        val groups=pool.groupBy{key(it.artist)}.values.map{it.toMutableList()};val out=mutableListOf<Song>()
        while(out.size<limit.coerceIn(0,100)&&groups.any{it.isNotEmpty()}) for(g in groups) if(g.isNotEmpty()&&out.size<limit.coerceIn(0,100))out.add(g.removeAt(0))
        return out
    }
    data class Discovery(val title:String,val subtitle:String,val tracks:List<Song>)
    fun daily(history:List<LibraryStore.Listen>, fav:List<Song>, pool:List<Song>, date:LocalDate):List<Discovery> {
        val known=history.map{it.song.id}.toSet();val seed=date.toEpochDay();val fresh=pool.distinctBy{it.id}.filterNot{it.id in known}.shuffled(Random(seed))
        val picks=quick(history,fav,30).shuffled(Random(seed+1)).take(12)
        val mix=mix(history,fav,pool.shuffled(Random(seed+2)),30).shuffled(Random(seed+3)).take(12)
        return listOf(Discovery("Discover something different","Unplayed tracks in your local collection",fresh.take(12)),Discovery("Rediscover your favourites","Songs you have enjoyed",picks),Discovery("Daily mix","Based on your listening and collection",mix)).filter{it.tracks.isNotEmpty()}
    }
    data class Suggestion(val title:String,val artist:String)
    fun suggestions(text:String):List<Suggestion> = text.lineSequence().mapNotNull{line->
        val parts=line.trim().removePrefix("- ").split('\t',limit=2)
        if(parts.size!=2)null else {val t=parts[0].trim();val a=parts[1].trim();if(t.length in 1..180&&a.length in 1..180&&!t.contains("://")&&!a.contains("://"))Suggestion(t,a) else null}
    }.distinctBy{key(it.title) to key(it.artist)}.take(15).toList()
    fun matching(s:Suggestion, tracks:List<Song>):Song?=tracks.firstOrNull{key(it.title)==key(s.title)&&key(it.artist)==key(s.artist)}
    data class Recap(val minutes:Long,val tracks:Int,val artists:Int,val days:Int,val topSongs:List<Pair<String,Long>>,val topArtists:List<Pair<String,Long>>,val months:List<Pair<Int,Long>>)
    fun recap(history:List<LibraryStore.Listen>,year:Int,zone:ZoneId):Recap? {
        val h=history.filter{it.ms>0&&Instant.ofEpochMilli(it.at).atZone(zone).year==year};if(h.isEmpty())return null
        fun rank(group:(LibraryStore.Listen)->String)=h.groupBy(group).map{it.key to it.value.sumOf{r->r.ms}}.sortedByDescending{it.second}.take(5)
        return Recap(h.sumOf{it.ms}/60000,h.map{it.song.id}.distinct().size,h.map{key(it.song.artist)}.filter{it.isNotBlank()}.distinct().size,h.map{Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate()}.distinct().size,rank{it.song.title+" — "+it.song.artist},rank{it.song.artist.ifBlank{"Unknown"}},(1..12).map{month->month to h.filter{Instant.ofEpochMilli(it.at).atZone(zone).monthValue==month}.sumOf{it.ms}})
    }
}
