package com.abhi.ganasuno.ui

object LrcParser {
    data class Line(val ms:Long,val text:String)
    private val stamp=Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?\\]")
    fun parse(raw:String):List<Line> = raw.lineSequence().flatMap { line ->
        val text=stamp.replace(line,"").trim()
        stamp.findAll(line).filter{it.groupValues[2].toInt()<60}.map { m -> Line(m.groupValues[1].toLong()*60000+m.groupValues[2].toLong()*1000+m.groupValues[3].padEnd(3,'0').take(3).toLong(),text) }
    }.sortedBy{it.ms}.toList()
}
