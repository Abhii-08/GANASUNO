package com.abhi.ganasuno.data

/** Small deterministic helpers for account import and sampled chart aggregation. */
object Batch27 {
    fun comment(raw:String):String?=raw.trim().takeIf{it.length in 1..10000&&it.none{c->c.isISOControl()&&c!='\n'&&c!='\t'}}
    fun commentId(raw:String)=raw.matches(Regex("[A-Za-z0-9_.-]{1,200}"))
    fun merge(local:List<Song>,remote:List<Song>):List<Song> = (local+remote.filter{it.id.matches(Regex("[A-Za-z0-9_-]{11}"))&&it.title.isNotBlank()&&it.localUri.isBlank()}).distinctBy{it.id}
    data class Rank(val id:String,val countries:Int,val score:Int)
    /** Equal weight per successful regional list; this is not global consumption data. */
    fun ranks(lists:List<List<String>>):List<Rank>{
        val counts=mutableMapOf<String,Int>();val scores=mutableMapOf<String,Int>()
        lists.forEach{rows->rows.distinct().take(20).forEachIndexed{i,id->if(id.matches(Regex("[A-Za-z0-9_-]{11}"))){counts[id]=(counts[id]?:0)+1;scores[id]=(scores[id]?:0)+20-i}}}
        return counts.map{(id,n)->Rank(id,n,scores.getValue(id))}.sortedWith(compareByDescending<Rank>{it.countries}.thenByDescending{it.score}.thenBy{it.id})
    }
}
