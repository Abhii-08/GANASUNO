package com.abhi.ganasuno.data

import java.io.InputStream
import java.security.MessageDigest
import kotlin.math.*

/** Pure helpers for the offline audit, saved curator list and tempo candidates. */
object Batch23 {
    const val MAX_AUDIO = 100L * 1024 * 1024
    data class Digest(val size:Long,val sha256:String)
    fun digest(input:InputStream,limit:Long=MAX_AUDIO):Digest {
        require(limit>0); val md=MessageDigest.getInstance("SHA-256"); val buf=ByteArray(65536); var total=0L
        while(true){check(!Thread.currentThread().isInterrupted);val n=input.read(buf);if(n<0)break;if(n==0)continue;total+=n;require(total<=limit){"Audio exceeds size limit"};md.update(buf,0,n)}
        require(total>0){"Empty audio"};return Digest(total,md.digest().joinToString(""){"%02x".format(it.toInt() and 255)})
    }
    fun audit(actual:Digest,expected:Digest?):String=when{expected==null->"No saved checksum";actual==expected->"Verified";else->"Changed or damaged"}
    fun addChannel(old:List<String>,raw:String):List<String>{val id=Batch22.channel(raw)?:error("Invalid channel ID");return (listOf(id)+old.filter{Batch16.valid("channel",it)}).distinct().take(30)}
    fun artistChannel(raw:String):String?=Batch22.channel(raw)
    data class Tempo(val bpm:Int,val correlation:Double)
    /** Normalized onset autocorrelation over 45–300 BPM; alternatives, not ground truth. */
    fun tempos(envelope:List<Double>,hz:Double):List<Tempo>{
        if(envelope.size<250||!hz.isFinite()||hz<=0||envelope.any{!it.isFinite()})return emptyList()
        val onset=DoubleArray(envelope.size-1){max(0.0,envelope[it+1]-envelope[it])};val mean=onset.average();if(onset.sumOf{(it-mean).pow(2)}<.0001)return emptyList()
        val minLag=(60*hz/300).roundToInt().coerceAtLeast(1);val maxLag=(60*hz/45).roundToInt().coerceAtMost(onset.size/3);if(maxLag<minLag)return emptyList()
        val scores=(minLag..maxLag).map{lag->var dot=0.0;var a=0.0;var b=0.0;for(i in lag until onset.size){val x=onset[i]-mean;val y=onset[i-lag]-mean;dot+=x*y;a+=x*x;b+=y*y};Tempo((60*hz/lag).roundToInt(),dot/sqrt(max(a*b,1e-12)))}
        return scores.filterIndexed{i,t->t.bpm in 45..300&&t.correlation>=.2&&(i==0||t.correlation>=scores[i-1].correlation)&&(i==scores.lastIndex||t.correlation>=scores[i+1].correlation)}.sortedByDescending{it.correlation}.distinctBy{it.bpm}.take(5)
    }
}
