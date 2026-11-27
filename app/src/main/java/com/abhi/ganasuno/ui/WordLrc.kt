package com.abhi.ganasuno.ui

/** Enhanced LRC uses <mm:ss.xxx> timestamps before words. */
object WordLrc {
    data class Word(val time:Long,val text:String)
    private val stamp=Regex("<(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?>")
    private val lineStamp=Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?\\]")
    private val metadata=Regex("^\\[(ar|al|ti|au|by|re|ve|length|offset):[^\\]\\r\\n]*\\]$",RegexOption.IGNORE_CASE)
    private fun time(m:MatchResult):Long? { val seconds=m.groupValues[2].toLong();if(seconds>=60)return null;return m.groupValues[1].toLong()*60000+seconds*1000+m.groupValues[3].padEnd(3,'0').toLong() }
    fun parse(line:String):List<Word> {
        val matches=stamp.findAll(line).toList();val result=mutableListOf<Word>()
        for((i,m) in matches.withIndex()) {val t=time(m)?:return emptyList();val text=line.substring(m.range.last+1,matches.getOrNull(i+1)?.range?.first?:line.length);if(text.isNotBlank())result.add(Word(t,text))}
        return result.takeIf{it.zipWithNext().all{(a,b)->a.time<=b.time}}?:emptyList()
    }
    fun plain(line:String)=stamp.replace(line,"")
    fun error(raw:String):String? {
        if(raw.isBlank()||raw.length>100000)return "Enter 1–100,000 characters."
        var content=false;var timed=false;var untimed=false
        for((i,original) in raw.lineSequence().withIndex()){
            val line=original.trim();if(line.isEmpty())continue
            if(metadata.matches(line))continue
            val marks=lineStamp.findAll(line).toList();val words=stamp.findAll(line).toList()
            val stripped=lineStamp.replace(line,"");val plain=stamp.replace(stripped,"")
            if(Regex("[\\[<]\\d+:").containsMatchIn(plain)||marks.any{time(it)==null}||words.any{time(it)==null})return "Line ${i+1}: invalid timestamp."
            if(marks.isNotEmpty()){
                var end=0;for(m in marks){if(m.range.first!=end)return "Line ${i+1}: line timestamps must appear at the start.";end=m.range.last+1}
                timed=true
                if(words.isNotEmpty()){
                    if(marks.size!=1||!stripped.trimStart().startsWith("<")||parse(stripped).isEmpty()||words.zipWithNext().any{(a,b)->time(a)!!>time(b)!!}||time(words.first())!!<time(marks.first())!!)return "Line ${i+1}: word timings must follow the line timestamp in order."
                    if(words.zipWithNext().any{(a,b)->line.substring(a.range.last+1,b.range.first).isBlank()})return "Line ${i+1}: empty word segment."
                }
            }else{if(words.isNotEmpty())return "Line ${i+1}: word timing requires a line timestamp.";untimed=true}
            if(plain.isNotBlank())content=true
        }
        if(timed&&untimed)return "Use either plain lyrics or timestamp every lyric line."
        return if(content)null else "No lyric text found."
    }
    fun valid(raw:String)=error(raw)==null
}
