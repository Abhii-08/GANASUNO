package com.abhi.ganasuno.data

import java.time.Instant
import java.time.temporal.ChronoUnit

/** Bounded public catalogue requests. No account tokens or playback URLs. */
object Batch16 {
    private val video=Regex("[A-Za-z0-9_-]{11}")
    private val channel=Regex("UC[A-Za-z0-9_-]{22}")
    private val playlist=Regex("[A-Za-z0-9_-]{10,100}")
    fun valid(kind:String,id:String)=when(kind){"video"->video.matches(id);"channel"->channel.matches(id);"playlist"->playlist.matches(id);else->false}
    fun pageSize(remaining:Int)=remaining.coerceIn(1,50)
    fun homeLimit(value:Int)=value.coerceIn(10,100)
    fun since(now:Instant,days:Long)=now.minus(days.coerceIn(1,90),ChronoUnit.DAYS).toString()
    fun fresh(saved:Long,now:Long)=saved>0&&now>=saved&&now-saved<7L*86400000
    fun due(last:Long,now:Long)=last<=0||now<last||now-last>=86400000L
    fun keyLooksValid(raw:String)=raw.matches(Regex("[A-Za-z0-9_-]{20,120}"))
    fun publicUrl(kind:String,id:String):String {require(valid(kind,id));return when(kind){"video"->"https://www.youtube.com/watch?v=$id";"channel"->"https://www.youtube.com/channel/$id";else->"https://www.youtube.com/playlist?list=$id"}}
    fun stats(label:String,value:String?):String="$label: ${value?.takeIf{it.matches(Regex("[0-9]{1,30}"))}?:"Unavailable"}"
    fun clean(raw:String,max:Int=300)=raw.filterNot{it.isISOControl()&&it!='\n'}.trim().take(max)
    /** Deduplicate results, stop on repeated pagination token, and cap upstream pages. */
    fun <T,K> collect(limit:Int,identity:(T)->K,load:(Int,String)->Pair<List<T>,String>):List<T>{require(limit in 1..200);val out=linkedMapOf<K,T>();var token="";val seen=mutableSetOf<String>();repeat(5){val (rows,next)=load(pageSize(limit-out.size),token);rows.forEach{out.putIfAbsent(identity(it),it)};if(out.size>=limit||next.isBlank()||!seen.add(next))return out.values.take(limit);token=next};return out.values.take(limit)}
    fun alternate(originalId:String,title:String,candidates:List<Pair<String,String>>):List<String> = candidates.filter{(id,t)->valid("video",id)&&id!=originalId&&Batch17.isVersion(title,t)}.map{it.first}.distinct()
}
