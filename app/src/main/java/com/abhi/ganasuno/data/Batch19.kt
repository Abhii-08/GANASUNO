package com.abhi.ganasuno.data

import java.net.URI
import java.util.Locale

object Batch19 {
    data class Destination(val endpoint:String,val key:String,val host:String)
    fun destination(raw:String):Destination? = runCatching {
        val u=URI(raw.trim()); require(u.scheme=="https" && u.host!=null && u.rawQuery==null && u.rawFragment==null && u.port in listOf(-1,443))
        val key=u.rawUserInfo.orEmpty(); require(key.matches(Regex("[a-fA-F0-9]{32}")))
        val segments=u.rawPath.split('/').filter{it.isNotEmpty()}; require(segments.isNotEmpty() && segments.last().matches(Regex("[0-9]{1,20}")))
        require(segments.dropLast(1).all{it.matches(Regex("[A-Za-z0-9_-]{1,60}"))})
        val host=u.host.lowercase(Locale.ROOT); require(host.matches(Regex("[a-z0-9.-]+")))
        val path=segments.dropLast(1).joinToString("/").let{if(it.isBlank())"" else "/$it"}
        Destination("https://$host$path/api/${segments.last()}/envelope/",key,host)
    }.getOrNull()
    fun safeReport(raw:String):String = raw.lineSequence().take(20).filter{it.matches(Regex("(?:Uncaught [A-Za-z0-9_.$]+|Android API [0-9]{1,3}|[A-Za-z0-9_.$]+:[-0-9]{1,8})"))}.joinToString("\n").take(3000)
    fun bitrate(bytes:Long,durationMs:Long):Int? = if(bytes in 1..(100L*1024*1024)&&durationMs in 1..86400000L) (bytes*8/durationMs).toInt().takeIf{it in 1..100000} else null
    fun bpm(raw:String):Int?=raw.trim().toIntOrNull()?.takeIf{it in 20..300}
    fun key(raw:String):String? { val v=raw.trim().replace('♯','#').replace('♭','b');return v.takeIf{it.matches(Regex("[A-G](?:#|b)? (?:major|minor|dorian|mixolydian|phrygian|lydian|locrian|major pentatonic|minor pentatonic|blues)"))} }
    fun scale(key:String?):String?=key?.substringAfter(' ',"")?.takeIf{it.isNotBlank()}
}
