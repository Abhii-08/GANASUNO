package com.abhi.ganasuno.data

import java.security.MessageDigest
import java.net.URI

object Batch20 {
    const val MAX_ART_BYTES = 8L * 1024 * 1024
    const val PRELOAD_BYTES = 512L * 1024
    fun digest(value:String):String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString(""){ "%02x".format(it.toInt() and 255) }
    fun lyricKey(id:String,lyrics:String):String? = if(id.matches(Regex("[A-Za-z0-9_-]{11}")) && lyrics.isNotBlank()) digest(id+"\n"+lyrics.replace("\r\n","\n").trim()) else null
    fun origin(raw:String):String? = runCatching { val u=URI(raw.trim());require(u.scheme=="https" && !u.host.isNullOrBlank() && u.userInfo==null && u.query==null && u.fragment==null && u.path in listOf("", "/") && u.port in listOf(-1,443)); "https://${u.host}" }.getOrNull()
    fun artworkName(id:String)=digest(id)+".gif"
    fun rateLabel(kbps:Int?):String = when { kbps==null || kbps<=0 -> "Bitrate unavailable"; kbps in 304..336 -> "Approximately 320 kbps ($kbps kbps reported)"; else -> "$kbps kbps reported; not approximately 320 kbps" }
    fun canPreload(enabled:Boolean,cache:Boolean,unmetered:Boolean,live:Boolean,https:Boolean)=enabled&&cache&&unmetered&&!live&&https
    /** Limit the sum of requested page sizes, even if rows are filtered or duplicated. */
    fun <T,K> boundedPages(limit:Int,identity:(T)->K,load:(Int,String)->Pair<List<T>,String>):List<T>{
        require(limit in 1..200);val out=linkedMapOf<K,T>();var remaining=limit;var token="";val seen=mutableSetOf<String>()
        while(remaining>0){val size=minOf(50,remaining);remaining-=size;val (rows,next)=load(size,token);rows.take(size).forEach{out.putIfAbsent(identity(it),it)};if(next.isBlank()||!seen.add(next))break;token=next}
        return out.values.take(limit)
    }
    fun nearestMarker(markers:List<Float>,point:Float,tolerance:Float):Int? {
        if(!point.isFinite()||!tolerance.isFinite()||tolerance<0)return null
        return markers.indices.filter{markers[it].isFinite()&&markers[it] in 0f..1f&&kotlin.math.abs(markers[it]-point)<=tolerance}.minByOrNull{kotlin.math.abs(markers[it]-point)}
    }
    data class SimilarArtist(val name:String,val score:Double)
    fun similarArtists(seed:String,rows:List<SimilarArtist>,limit:Int=30):List<SimilarArtist>{
        fun norm(s:String)=s.trim().lowercase(java.util.Locale.ROOT)
        return rows.filter{it.name.isNotBlank()&&it.name.length<=200&&norm(it.name)!=norm(seed)&&it.score.isFinite()&&it.score in 0.0..1.0}.sortedByDescending{it.score}.distinctBy{norm(it.name)}.take(limit.coerceIn(1,50))
    }
}
