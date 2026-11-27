package com.abhi.ganasuno.data

import kotlin.math.abs
import kotlin.math.roundToLong

object Batch18 {
    private val publicId=Regex("[A-Za-z0-9_-]{11}")
    fun local(s:Song)=s.localUri.startsWith("content://") && s.id=="local:${s.localUri}"
    fun eligible(seed:Song, tracks:List<Song>, related:List<String>, excluded:Set<String>, allowed:(Song)->Boolean, limit:Int=10):List<Song> {
        val offline=local(seed)
        val pool=tracks.filter{!it.live && (if(offline)local(it) else it.localUri.isBlank()&&publicId.matches(it.id)) }
        return Batch17.radio(seed,pool,related,excluded,100).filter(allowed).take(limit.coerceIn(0,100))
    }
    fun markerFractions(positions:List<Long>,duration:Long):List<Float> = if(duration<=0) emptyList() else positions.filter{it in 0..duration}.distinct().sorted().map{it.toFloat()/duration}
    fun validSpeed(speed:Double)=speed.isFinite() && speed in 0.5..2.0
    fun projectedPosition(position:Long,playing:Boolean,speed:Double,roundTrip:Long,duration:Long?):Long {
        val max=duration?.takeIf{it>0}?.coerceAtMost(86400000L)?:86400000L
        val delta=if(playing&&validSpeed(speed)) (roundTrip.coerceIn(0L,2000L)*0.5*speed).roundToLong() else 0L
        return (position.coerceIn(0L,max)+delta).coerceAtMost(max)
    }
    fun shouldSeek(current:Long,target:Long,playing:Boolean,buffering:Boolean)=!buffering && abs(current.coerceIn(0L,86400000L)-target.coerceIn(0L,86400000L))>if(playing) 1500L else 250L
    fun albumKey(album:String,artist:String):String {val a=album.trim().lowercase(java.util.Locale.ROOT);val b=artist.trim().lowercase(java.util.Locale.ROOT);return "${a.length}:$a$b"}
}
