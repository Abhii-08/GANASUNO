package com.abhi.ganasuno.data

import kotlin.math.*

/** Streaming PCM summary: 120 waveform peaks, onset autocorrelation, pitch-class energy. */
class AudioAnalysis(private val rate:Int, val durationMs:Long){
    data class Result(val peaks:FloatArray,val bpm:Int?,val key:String?,val analyzedMs:Long,val tempoCandidates:List<Batch23.Tempo> = emptyList())
    init{require(rate in 8000..192000 && durationMs>0)}
    private val peaks=FloatArray(120);private var frames=0L;private val hop=(rate/50).coerceAtLeast(1)
    private var energy=0.0;private val envelope=ArrayList<Double>();private val stride=(rate/8000).coerceAtLeast(1);private val pitchRate=rate.toDouble()/stride
    private val pitch=DoubleArray(2048);private var used=0;private val chroma=DoubleArray(12);private var windows=0
    private val coeff=DoubleArray(48){2*cos(2*PI*440*2.0.pow((it+36-69)/12.0)/pitchRate)}
    private val window=DoubleArray(pitch.size){0.5-0.5*cos(2*PI*it/(pitch.size-1))}
    fun sample(raw:Double){val x=if(raw.isFinite())raw.coerceIn(-1.0,1.0)else 0.0;val idx=(frames*120*1000/(rate.toLong()*durationMs)).coerceIn(0,119).toInt();peaks[idx]=max(peaks[idx],abs(x).toFloat());if(frames<rate*90L){energy+=x*x;if((frames+1)%hop==0L){envelope.add(sqrt(energy/hop));energy=0.0};if(frames%stride==0L){pitch[used]=x*window[used];used++;if(used==pitch.size){accumulate();used=0}}};frames++}
    private fun accumulate(){if(pitch.sumOf{it*it}<0.01)return;for(i in coeff.indices){var a=0.0;var b=0.0;for(v in pitch){val next=v+coeff[i]*a-b;b=a;a=next};chroma[(i+36)%12]+=sqrt(max(0.0,a*a+b*b-coeff[i]*a*b))};windows++}
    fun result():Result {val bpm=tempo();val max=peaks.maxOrNull()?:0f;return Result(if(max>0f)FloatArray(peaks.size){peaks[it]/max}else peaks.copyOf(),bpm,key(),frames*1000/rate,Batch23.tempos(envelope,rate.toDouble()/hop))}
    private fun tempo():Int? {if(envelope.size<250)return null;val onset=DoubleArray(envelope.size-1){max(0.0,envelope[it+1]-envelope[it])};val mean=onset.average();val variance=onset.sumOf{(it-mean)*(it-mean)};if(variance<0.0001)return null;var best=0.0;var lag=0;for(l in 15..50){var dot=0.0;var a=0.0;var b=0.0;for(i in l until onset.size){val x=onset[i]-mean;val y=onset[i-l]-mean;dot+=x*y;a+=x*x;b+=y*y};val score=dot/sqrt(max(a*b,1e-12));if(score>best+0.015){best=score;lag=l}};return if(best>=0.2&&lag>0)(60.0*rate/hop/lag).roundToInt()else null}
    private fun key():String? {if(windows<8||chroma.sum()<1e-6)return null;val major=doubleArrayOf(6.35,2.23,3.48,2.33,4.38,4.09,2.52,5.19,2.39,3.66,2.29,2.88);val minor=doubleArrayOf(6.33,2.68,3.52,5.38,2.60,3.53,2.54,4.75,3.98,2.69,3.34,3.17);val mean=chroma.average();val norm=sqrt(chroma.sumOf{(it-mean).pow(2)});if(norm<1e-6)return null;val scores=(0..23).map{i->val p=if(i<12)major else minor;val m=p.average();val n=sqrt(p.sumOf{(it-m).pow(2)});i to (p.indices.sumOf{j->(p[j]-m)*(chroma[(j+i%12)%12]-mean)}/(norm*n))}.sortedByDescending{it.second};if(scores[0].second<0.45||scores[0].second-scores[1].second<0.025)return null;val i=scores.first().first;return listOf("C","C♯","D","D♯","E","F","F♯","G","G♯","A","A♯","B")[i%12]+if(i<12)" major" else " minor"}
}
