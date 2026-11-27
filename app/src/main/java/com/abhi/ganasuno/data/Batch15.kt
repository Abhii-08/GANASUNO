package com.abhi.ganasuno.data

/** Offline grouping and validation, shared by catalogue screens and playlist transport. */
object Batch15 {
    private val video=Regex("[A-Za-z0-9_-]{11}")
    private val shareCode=Regex("[A-Za-z0-9_-]{32}")
    fun publicIds(songs:List<Song>)=songs.filter{it.localUri.isBlank()&&video.matches(it.id)&&!it.live}.map{it.id}.distinct().take(100)
    fun validCode(code:String)=shareCode.matches(code)
    fun cleanTitle(raw:String)=raw.trim().take(100).filterNot{it.isISOControl()}.trim()
    data class Group(val title:String,val artist:String,val tracks:List<Song>)
    fun artists(songs:List<Song>):List<Group> = songs.distinctBy{it.id}.filter{it.artist.isNotBlank()}.groupBy{Batch13.key(it.artist)}.values.map{Group(it.first().artist,"",it)}.sortedBy{Batch13.key(it.title)}
    fun albums(songs:List<Song>,tags:Map<String,String> = emptyMap()):List<Group> = songs.distinctBy{it.id}.map{it.copy(album=tags[it.id]?:it.album)}.filter{it.album.isNotBlank()}.groupBy{Batch13.key(it.album) to Batch13.key(it.artist)}.values.map{Group(it.first().album,it.first().artist,it)}.sortedWith(compareBy<Group>{Batch13.key(it.title)}.thenBy{Batch13.key(it.artist)})
    fun matching(groups:List<Group>,q:String):List<Group>{val terms=Batch13.key(q).split(' ').filter{it.isNotBlank()};return groups.filter{g->terms.all{term->Batch13.key(g.title+" "+g.artist).split(' ').any{word->word.startsWith(term)}}}}
    fun shareIds(raw:List<String>):List<String>{require(raw.size in 1..100&&raw.all{video.matches(it)});return raw.distinct()}
    fun recap(samples:List<LibraryStore.Listen>,start:Long,end:Long,zone:java.time.ZoneId=java.time.ZoneId.systemDefault(),albumTags:Map<String,String> = emptyMap()):String {
        require(start<end);val rows=samples.filter{it.at>=start&&it.at<end&&it.ms>0};if(rows.isEmpty())return ""
        fun top(items:Map<String,List<LibraryStore.Listen>>)=items.entries.sortedByDescending{it.value.sumOf{r->r.ms}}.take(12).joinToString("\n"){(name,values)->"${name.take(120)}: ${values.sumOf{it.ms}/60000} min"}
        fun month(at:Long)=java.time.YearMonth.from(java.time.Instant.ofEpochMilli(at).atZone(zone))
        val first=month(start);val last=month(end-1);val months=rows.groupBy{month(it.at)};val total=rows.sumOf{it.ms}
        val dates=rows.map{java.time.Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate()}.distinct().size
        val days=java.time.temporal.ChronoUnit.DAYS.between(java.time.Instant.ofEpochMilli(start).atZone(zone).toLocalDate(),java.time.Instant.ofEpochMilli(end).atZone(zone).toLocalDate()).toInt().coerceAtLeast(1)
        val metrics=Batch13.metrics(rows,emptyList(),days,zone)
        return buildString{appendLine("Recorded listening only: ${total/60000} minutes, $dates active days, ${rows.map{it.song.id}.distinct().size} tracks.");appendLine("Months (zero means no recorded data, not proof of no listening):");var m=first;while(m<=last&&m<first.plusMonths(13)){appendLine("$m: ${(months[m]?.sumOf{it.ms}?:0)/60000} min");m=m.plusMonths(1)};appendLine("Top artists:\n"+top(rows.groupBy{it.song.artist.ifBlank{"Unknown"}}));appendLine("Top tracks:\n"+top(rows.groupBy{it.song.title+" — "+it.song.artist}));appendLine("Known albums:\n"+top(rows.filter{(albumTags[it.song.id]?:it.song.album).isNotBlank()}.groupBy{(albumTags[it.song.id]?:it.song.album)+" — "+it.song.artist}));metrics?.let{appendLine("Consistency ${it.consistency}%, diversity ${it.diversity}%, concentration ${it.concentration}%, replay ${it.replay}% (local heuristics).")}}
    }
    fun streamFormat(suffix:String?,itag:Int?):Pair<String,String> = when {
        itag in setOf(249,250,251) -> "Opus" to "audio/webm"
        itag in setOf(139,140,141) -> "AAC" to "audio/mp4"
        suffix.equals("m4a",true) -> "Unknown (MP4 audio)" to "audio/mp4"
        suffix.equals("webm",true) -> "Unknown (WebM audio)" to "audio/webm"
        else -> "Unknown" to "Unknown"
    }
}
