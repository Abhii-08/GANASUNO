package com.abhi.ganasuno.player

import kotlin.math.*
import java.util.Locale

/** Audio transition calculations independent of Android. */
object TransitionMath {
    fun durationMs(seconds:Int)=seconds.coerceIn(0,15)*1000L
    fun sameAlbum(a:String,b:String,artistA:String,artistB:String):Boolean {
        fun norm(s:String)=s.trim().lowercase(Locale.ROOT).replace(Regex("\\s+")," ")
        val x=norm(a);val y=norm(b)
        return x.isNotEmpty()&&x==y&&norm(artistA).isNotEmpty()&&norm(artistA)==norm(artistB)
    }
    fun gains(progress:Double):Pair<Float,Float>{val t=progress.coerceIn(0.0,1.0)*PI/2;return cos(t).toFloat() to sin(t).toFloat()}
    fun progress(remaining:Long,length:Long):Double=if(length<=0)0.0 else (1.0-remaining.toDouble()/length).coerceIn(0.0,1.0)
    fun canOverlap(seconds:Int,remaining:Long,live:Boolean,nextLive:Boolean,sameAlbum:Boolean,keep:Boolean):Boolean=seconds in 1..15&&remaining>0&&!live&&!nextLive&&!(sameAlbum&&keep)
    fun cutoff(progress:Double,incoming:Boolean):Double {val t=progress.coerceIn(0.0,1.0);return if(incoming) 1800.0*(25.0/1800.0).pow(t) else 20000.0*(300.0/20000.0).pow(t)}
}
/** Per-deck first-order low/high-pass filter with continuous sample state. */
class SweepFilter(private val rate:Int,private val channels:Int){
    private val low=DoubleArray(channels)
    fun process(samples:ShortArray,progress:Double,incoming:Boolean){
        require(rate>0&&channels>0&&samples.size%channels==0)
        val hz=TransitionMath.cutoff(progress,incoming).coerceIn(1.0,rate*0.45)
        val alpha=1-exp(-2*PI*hz/rate)
        for(i in samples.indices){val ch=i%channels;val x=samples[i].toDouble();low[ch]+=alpha*(x-low[ch]);val out=if(incoming)x-low[ch] else low[ch];samples[i]=out.roundToInt().coerceIn(-32768,32767).toShort()}
    }
}
class DeckSweep { @Volatile var progress:Double=-1.0; @Volatile var incoming:Boolean=false; fun clear(){progress=-1.0} }
