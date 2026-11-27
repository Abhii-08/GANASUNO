package com.abhi.ganasuno.data

import java.net.URI
import java.time.LocalDate
import kotlin.math.abs

/** Provider-independent, deterministic policy for opt-in offline sources and release notices. */
object Batch28 {
    data class Variant(val kbps:Int,val url:String)
    fun direct(raw:String):String?=runCatching {
        val u=URI(raw.trim());val host=u.host?.lowercase()?:error("Missing host")
        require(u.scheme.equals("https",true)&&u.rawUserInfo==null&&u.rawFragment==null&&u.port in listOf(-1,443))
        require(listOf("youtube.com","youtu.be","googlevideo.com","spotify.com","ytimg.com").none{host==it||host.endsWith(".$it")})
        require(!host.endsWith(".local")&&host.contains('.')&&host.any{it.isLetter()}&&host!="localhost")
        require(u.toASCIIString().length<=2048);u.toASCIIString()
    }.getOrNull()
    fun parse(raw:String):List<Variant> {
        require(raw.length<=15000)
        val rows=raw.lineSequence().filter{it.isNotBlank()}.map {line->
            val parts=line.trim().split(Regex("\\s+"),limit=2);require(parts.size==2){"Use bitrate followed by HTTPS URL"}
            val n=parts[0].toIntOrNull()?:error("Invalid bitrate");require(n in 32..1411)
            Variant(n,direct(parts[1])?:error("Invalid or restricted audio URL"))
        }.toList();require(rows.size in 1..6&&rows.distinctBy{it.kbps}.size==rows.size);return rows.sortedBy{it.kbps}
    }
    fun select(rows:List<Variant>,target:Int):Variant?=rows.minWithOrNull(compareBy<Variant>{abs(it.kbps-target.coerceIn(32,1411))}.thenBy{it.kbps})
    fun candidates(favorites:List<String>,played:List<String>,registered:Set<String>,saved:Set<String>,auto:Boolean,smart:Boolean,limit:Int):List<String>{
        val ordered=(if(auto)favorites else emptyList())+(if(smart)played.take(20) else emptyList())
        return ordered.distinct().filter{it in registered&&it !in saved}.take(limit.coerceIn(0,20))
    }
    data class Release(val id:String,val title:String,val date:String)
    fun unseen(rows:List<Release>,seen:Set<String>,initialized:Boolean,today:LocalDate):List<Release> = if(!initialized)emptyList() else rows.distinctBy{it.id}.filter{it.id !in seen&&Batch21.validId(it.id)&&Batch21.isRecent(it.date,today,30)}
    fun fingerprints(raw:String):Set<String> = raw.split(',', '\n',' ').map{it.replace(":", "").trim().lowercase()}.filter{it.matches(Regex("[0-9a-f]{64}"))}.toSet()
    fun signatureStatus(actual:Set<String>,expected:Set<String>):String=when{actual.isEmpty()->"Signing identity unavailable";expected.isEmpty()->"Unconfigured · no trusted reference";actual==expected->"Matches configured signing identity";else->"Signing identity mismatch"}
    fun scrobbleEligible(durationMs:Long,playedMs:Long)=durationMs>30000L&&durationMs<=86400000L&&playedMs>=minOf(durationMs/2,240000L)
    fun lastFmSignature(fields:Map<String,String>,secret:String):String {
        val material=fields.filterKeys{it !in setOf("format","callback","api_sig")}.toSortedMap().entries.joinToString(""){it.key+it.value}+secret
        return java.security.MessageDigest.getInstance("MD5").digest(material.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it.toInt() and 255)}
    }
}
