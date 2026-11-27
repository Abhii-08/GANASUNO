package com.abhi.ganasuno.data

import java.time.LocalDate
import java.util.Locale

/** Pure helpers for catalogue identity, bounded requests and release date filtering. */
object Batch21 {
    val kinds=setOf("artist","recording","release-group","release")
    fun validId(id:String)=id.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
    fun url(kind:String,id:String):String {require(kind in kinds&&validId(id));return "https://musicbrainz.org/$kind/$id"}
    fun text(s:String,limit:Int=300)=s.filterNot{it.isISOControl()}.trim().take(limit)
    fun quoted(s:String):String {val clean=text(s,160);require(clean.isNotBlank());return "\""+clean.replace("\\","\\\\").replace("\"","\\\"")+"\""}
    fun search(kind:String,q:String):String {require(kind in kinds);val field=when(kind){"release-group"->"releasegroup";else->kind};return "$field:${quoted(q)}"}
    fun recent(q:String,today:LocalDate,days:Int=30):String = (if(q.isBlank()) "" else "artist:${quoted(q)} AND ")+"(primarytype:album OR primarytype:single OR primarytype:ep) AND firstreleasedate:[${today.minusDays(days.coerceIn(1,90).toLong())} TO $today]"
    fun isRecent(raw:String,today:LocalDate,days:Int=30):Boolean=runCatching{val d=LocalDate.parse(raw);!d.isAfter(today)&&!d.isBefore(today.minusDays(days.coerceIn(1,90).toLong()))}.getOrDefault(false)
    fun contact(raw:String):String?=raw.trim().takeIf{it.length in 5..160&&it.matches(Regex("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}"))}
    fun limit(v:Int)=v.coerceIn(1,50)
    fun animationContainer(bytes:ByteArray):Boolean {if(bytes.size<12)return false;val header=bytes.copyOfRange(0,6).toString(Charsets.US_ASCII);return header in setOf("GIF87a","GIF89a") || (bytes.copyOfRange(0,4).toString(Charsets.US_ASCII)=="RIFF"&&bytes.copyOfRange(8,12).toString(Charsets.US_ASCII)=="WEBP")}
    fun fraction(x:Float,width:Int):Float?=if(width>0&&x.isFinite())(x/width).coerceIn(0f,1f)else null
    fun dragged(start:Float,end:Float,slop:Int)=start.isFinite()&&end.isFinite()&&kotlin.math.abs(end-start)>slop.coerceAtLeast(0)
    fun artistTypes(raw:String)=raw.lowercase(Locale.ROOT) in setOf("album","single","ep")
    fun <T> distinctValid(rows:List<T>,id:(T)->String,limit:Int=50)=rows.filter{validId(id(it))}.distinctBy{id(it).lowercase(Locale.ROOT)}.take(limit(limit))
}
