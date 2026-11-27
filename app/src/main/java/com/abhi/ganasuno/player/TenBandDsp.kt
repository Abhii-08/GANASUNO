package com.abhi.ganasuno.player

import kotlin.math.*

/** Independent ten-band peaking EQ. Each channel has its own filter history. */
class TenBandDsp(private val rate:Int, private val channels:Int, gains:List<Int>, preamp:Int=0) {
    companion object {
        val frequencies = listOf(31,62,125,250,500,1000,2000,4000,8000,16000)
        /** GraphicEQ point pairs, interpolated in log frequency. Not a parametric EQ conversion. */
        fun graphicProfile(raw:String):List<Int> {
            val text=raw.trim().removePrefix("GraphicEQ:").trim()
            val points=text.split(';').filter{it.isNotBlank()}.map { part ->
                val v=part.trim().split(Regex("\\s+")); require(v.size==2){"Expected frequency and gain pairs"}
                val f=v[0].toDouble(); val g=v[1].toDouble(); require(f.isFinite()&&f in 10.0..48000.0&&g.isFinite()&&g in -40.0..40.0); f to g
            }.sortedBy{it.first}
            require(points.size in 2..1000 && points.zipWithNext().all{it.first.first<it.second.first}){"Provide at least two distinct frequencies"}
            return frequencies.map{f ->
                val i=points.indexOfFirst{it.first>=f}; val g=when{ i==0->points.first().second;i<0->points.last().second;else->{val a=points[i-1];val b=points[i];a.second+(b.second-a.second)*(ln(f.toDouble()/a.first)/ln(b.first/a.first))} };g.roundToInt().coerceIn(-12,12)
            }
        }
    }
    private class Filter(f:Int, rate:Int, gain:Int){
        private val a=10.0.pow(gain/40.0);private val w=2*PI*f/rate;private val alpha=sin(w)/(2*1.4)
        private val norm=1+alpha/a;private val b0=(1+alpha*a)/norm;private val b1=-2*cos(w)/norm;private val b2=(1-alpha*a)/norm
        private val a1=b1;private val a2=(1-alpha/a)/norm;private var z1=0.0;private var z2=0.0
        fun tick(x:Double):Double {val y=b0*x+z1;z1=b1*x-a1*y+z2;z2=b2*x-a2*y;return y}
    }
    init {require(rate>0&&channels in 1..2&&gains.size==10)}
    private val filters=Array(channels){ frequencies.mapIndexedNotNull {i,f->val g=gains[i].coerceIn(-12,12);if(g==0||f>=rate*0.45)null else Filter(f,rate,g) } }
    private val scale=10.0.pow(preamp.coerceIn(-36,12)/20.0)
    fun process(samples:ShortArray){for(i in samples.indices){var v=samples[i]*scale;for(filter in filters[i%channels])v=filter.tick(v);samples[i]=v.roundToInt().coerceIn(-32768,32767).toShort()}}
}
