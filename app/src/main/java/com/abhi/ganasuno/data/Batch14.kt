package com.abhi.ganasuno.data

import kotlin.math.*

object Batch14 {
    data class Profile(val path:String) { val name get()=path.substringAfterLast('/').removeSuffix(" GraphicEQ.txt"); val source get()=path.split('/').getOrElse(1){"Unknown"} }
    fun profilePath(path:String):Profile?=path.takeIf{it.startsWith("results/")&&it.endsWith(" GraphicEQ.txt")&&it.length<=600&&it.split('/').none{p->p.isBlank()||p=="."||p==".."}}?.let{Profile(it)}
    fun findProfiles(paths:List<String>,query:String):List<Profile>{val words=Batch13.key(query).split(' ').filter{it.isNotBlank()};return paths.mapNotNull(::profilePath).distinctBy{it.path}.filter{p->words.all{Batch13.key(p.name).contains(it)}}.sortedBy{it.name.lowercase()}.take(60)}
    fun lucene(s:String)="\""+s.take(160).replace("\\","\\\\").replace("\"","\\\"")+"\""
    fun matchTitle(a:String,b:String):Boolean {val x=Batch13.key(a);val y=Batch13.key(b);return x.isNotEmpty()&&y.isNotEmpty()&&(x==y||y.startsWith("$x ")||x.startsWith("$y "))}
    fun alternateVersions(song:Song, candidates:List<Song>):List<Song> = candidates.filter{it.id!=song.id&&!it.live&&Batch17.isVersion(song.title,it.title)}.distinctBy{it.id}.sortedByDescending{Batch13.key(it.artist)==Batch13.key(song.artist)}.take(40)
    fun mediaBudget(limit:Int)=limit.coerceIn(10,100)*64L*1024L
    fun backupKeep(keep:Int)=keep.coerceIn(1,20)
}

/** Adaptive RMS gain with bounded boost, slow attack/release and peak safety. Not LUFS/EBU R128. */
class RmsNormalizer(private val rate:Int,private val channels:Int){
    init{require(rate in 8000..192000&&channels in 1..2)}
    private var energy=0.0;private var gain=1.0
    private val smoothing=1-exp(-1.0/(rate*.5));private val attack=1-exp(-1.0/(rate*.08));private val release=1-exp(-1.0/(rate*1.5))
    fun process(samples:ShortArray){var i=0;while(i+channels<=samples.size){var power=0.0;var peak=0.0;for(j in 0 until channels){val x=samples[i+j]/32768.0;power+=x*x;peak=max(peak,abs(x))};energy+=(power/channels-energy)*smoothing;val rms=sqrt(max(energy,0.0));val target=if(rms<.003)1.0 else (.125/rms).coerceIn(.25,2.0);gain+=(target-gain)*if(target<gain)attack else release;val safe=min(gain,.98/max(peak,.0001));for(j in 0 until channels)samples[i+j]=(samples[i+j]*safe).roundToInt().coerceIn(-32768,32767).toShort();i+=channels}}
}
