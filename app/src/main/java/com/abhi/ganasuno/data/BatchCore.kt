package com.abhi.ganasuno.data

object BatchCore {
    fun swipe(dx:Float,dy:Float,threshold:Float):Int = when {
        dy>threshold && dy>kotlin.math.abs(dx) -> 3
        kotlin.math.abs(dx)>threshold && kotlin.math.abs(dx)>kotlin.math.abs(dy) -> if(dx>0)1 else 2
        else -> 0
    }
    val sortLabels=listOf("Title A–Z","Artist A–Z","Newest added first","Oldest added first","Recently played","Keep current / custom order","Reverse current order")
    fun order(items:List<Song>,mode:Int,added:Map<String,Long>,recent:Map<String,Long>):List<Song> = when(mode){
        0->items.sortedBy{it.title.lowercase(java.util.Locale.ROOT)}
        1->items.sortedBy{it.artist.lowercase(java.util.Locale.ROOT)}
        2->items.sortedByDescending{added[it.id]?:0L}
        3->items.sortedWith(compareBy<Song>{(added[it.id]?:0L)==0L}.thenBy{added[it.id]?:0L})
        4->items.sortedByDescending{recent[it.id]?:0L}
        6->items.reversed()
        else->items.toList()
    }
    fun color(raw:String):Int?=raw.trim().removePrefix("#").takeIf{it.matches(Regex("[0-9a-fA-F]{6}"))}?.toLong(16)?.let{(it or 0xff000000L).toInt()}
    fun headers(raw:String):Map<String,String>{
        require(raw.length<=8192){"Headers too long"};val out=linkedMapOf<String,String>();val blocked=setOf("host","content-length","content-type","transfer-encoding","connection","upgrade","proxy-authorization","proxy-connection","te","trailer","expect","accept-encoding")
        for(line in raw.lineSequence().filter{it.isNotBlank()}){
            val sep=line.indexOf(':');require(sep>0){"Use Name: value for each header"};val name=line.substring(0,sep).trim();val value=line.substring(sep+1).trim();val lower=name.lowercase(java.util.Locale.ROOT)
            require(name.matches(Regex("[A-Za-z0-9!#$%&'*+.^_`|~-]{1,80}"))&&lower !in blocked&&value.isNotEmpty()&&value.all{it.code in 32..126}&&out.keys.none{it.equals(name,true)}){"Invalid, duplicate or reserved header"};out[name]=value;require(out.size<=20){"Too many headers"}
        };return out
    }
    data class AlbumSample(val album:String,val artist:String,val ms:Long)
    fun albums(samples:List<AlbumSample>):List<Pair<String,Long>> = samples.filter{it.album.isNotBlank()&&it.ms>0}.groupBy{it.album.trim().lowercase(java.util.Locale.ROOT) to it.artist.trim().lowercase(java.util.Locale.ROOT)}.values.map{group->val first=group.first();(first.album.trim()+if(first.artist.isBlank())"" else " — ${first.artist.trim()}") to group.sumOf{it.ms}}.sortedByDescending{it.second}
}
