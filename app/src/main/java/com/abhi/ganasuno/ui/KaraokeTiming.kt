package com.abhi.ganasuno.ui

import java.text.BreakIterator
import java.util.Locale

/** Progressive karaoke timing. Explicit terminal stamps take priority over next-line boundaries. */
object KaraokeTiming {
    data class Segment(val start:Int,val end:Int,val startMs:Long,val endMs:Long,val clusters:List<Int>)
    data class Line(val text:String,val segments:List<Segment>)
    data class Fill(val start:Int,val end:Int,val fraction:Float)
    private val terminal = Regex("<(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?>\\s*$")
    private fun endStamp(raw:String):Long? = terminal.find(raw)?.let { m ->
        val seconds=m.groupValues[2].toLong()
        if(seconds>=60)null else m.groupValues[1].toLong()*60000+seconds*1000+m.groupValues[3].padEnd(3,'0').toLong()
    }
    fun prepare(raw:String,nextLineMs:Long?):Line {
        val words=WordLrc.parse(raw)
        if(words.isEmpty())return Line(WordLrc.plain(raw),emptyList())
        val text=StringBuilder()
        val segments=words.mapIndexed { i,w ->
            val start=text.length;text.append(w.text)
            val end=words.getOrNull(i+1)?.time ?: endStamp(raw)?.takeIf{it>=w.time} ?: nextLineMs?.takeIf{it>=w.time} ?: w.time
            val boundaries=mutableListOf<Int>()
            val iterator=BreakIterator.getCharacterInstance(Locale.ROOT).apply{setText(w.text)}
            var boundary=iterator.first();while(boundary!=BreakIterator.DONE){boundaries.add(start+boundary);boundary=iterator.next()}
            Segment(start,text.length,w.time,end.coerceAtLeast(w.time),boundaries)
        }
        return Line(text.toString(),segments)
    }
    fun progress(segment:Segment,position:Long):Float = when {
        position<segment.startMs->0f
        segment.endMs<=segment.startMs || position>=segment.endMs->1f
        else->(position-segment.startMs).toFloat()/(segment.endMs-segment.startMs)
    }
    fun fills(line:Line,position:Long):List<Fill> {
        val result=mutableListOf<Fill>()
        for(s in line.segments){
            val p=progress(s,position)
            if(p>=1f){result.add(Fill(s.start,s.end,1f));continue}
            if(p<=0f)continue
            val clusters=s.clusters
            val amount=p*(clusters.size-1)
            val completed=amount.toInt().coerceIn(0,clusters.size-1)
            if(completed>0)result.add(Fill(s.start,clusters[completed],1f))
            if(completed<clusters.lastIndex)result.add(Fill(clusters[completed],clusters[completed+1],amount-completed))
        }
        return result
    }
}
