package com.abhi.ganasuno.data

import java.net.URI
import java.util.Locale
import kotlin.math.abs

/** Pure helpers shared by v22 catalogue, download, audio and room upgrades. */
object Batch22 {
    fun httpsFile(raw:String):String?=runCatching { val u=URI(raw.trim());require(u.scheme=="https"&&!u.host.isNullOrBlank()&&u.rawUserInfo==null&&u.rawFragment==null&&u.port in listOf(-1,443));u.toASCIIString() }.getOrNull()
    fun token(raw:String)=raw.length in 20..4096 && raw.all{it.code in 33..126} && !raw.contains(':')
    fun channel(raw:String):String? { val v=raw.trim();if(Batch16.valid("channel",v))return v;return runCatching{val u=URI(v);require(u.scheme=="https"&&u.host in setOf("www.youtube.com","youtube.com")&&u.rawUserInfo==null);u.path.trimEnd('/').takeIf{it.startsWith("/channel/")}?.removePrefix("/channel/")?.takeIf{Batch16.valid("channel",it)}}.getOrNull() }
    fun duration(raw:String):Long? { val parts=raw.trim().split(':');if(parts.size !in 2..3||parts.any{!it.matches(Regex("[0-9]{1,4}"))})return null;val n=parts.map{it.toLong()};if(n.drop(1).any{it>=60})return null;return n.fold(0L){a,b->a*60+b}.takeIf{it in 1..86400}?.times(1000) }
    fun norm(s:String)=java.text.Normalizer.normalize(s,java.text.Normalizer.Form.NFKC).lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{M}\\p{N}]+")," ").trim()
    fun versionScore(seed:Song,other:Song):Int? { if(seed.id==other.id||other.live||!Batch17.isVersion(seed.title,other.title))return null;val a=norm(seed.artist);val b=norm(other.artist);if(a.isBlank()||b.isBlank()||a!=b)return null;val x=duration(seed.duration);val y=duration(other.duration);return 100+(if(x!=null&&y!=null&&abs(x-y)<=15000)20 else 0) }
    fun versions(seed:Song,rows:List<Song>)=rows.distinctBy{it.id}.mapNotNull{r->versionScore(seed,r)?.let{it to r}}.sortedByDescending{it.first}.map{it.second}.take(30)
    fun nextOffset(offset:Int,requested:Int,total:Int):Int? {require(offset>=0&&requested in 1..50);val next=offset.toLong()+requested;return next.takeIf{it<total&&it<=10000}?.toInt()}
    fun checkedSize(actual:Long,expected:Long,limit:Long)=actual in 1..limit&&(expected<0||actual==expected)
    fun seek(current:Float,delta:Float):Float = if(current.isFinite()&&delta.isFinite())(current+delta).coerceIn(0f,1f)else 0f
    // Small drift is corrected by temporary bounded speed adjustment; larger drift seeks instead.
    data class Sync(val speed:Float,val seek:Boolean)
    fun sync(current:Long,target:Long,host:Double,playing:Boolean,buffering:Boolean):Sync { val base=host.takeIf{Batch18.validSpeed(it)}?:1.0;val delta=target.coerceIn(0L,86400000L)-current.coerceIn(0L,86400000L);if(buffering)return Sync(base.toFloat(),false);if(!playing)return Sync(base.toFloat(),abs(delta)>250);if(abs(delta)>1500)return Sync(base.toFloat(),true);val corrected=if(abs(delta)<150)base else (base+delta/20000.0).coerceIn((base-.04).coerceAtLeast(.5),(base+.04).coerceAtMost(2.0));return Sync(corrected.toFloat(),false) }
    fun count(raw:String?):String=raw?.takeIf{it.matches(Regex("[0-9]{1,30}"))}?:"Unavailable"
    fun bpmAlternatives(bpm:Int?):List<Int> = if(bpm==null)emptyList()else listOf(bpm,(bpm/2.0).toInt(),bpm*2).filter{it in 20..300}.distinct()
}
