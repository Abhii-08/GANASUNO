package com.abhi.ganasuno.data

object Batch10 {
    const val PLAY_THRESHOLD_MS = 5000L
    fun fade(start:Float,end:Float,elapsed:Long,duration:Long):Float = start+(end-start)*(elapsed.toDouble()/duration.coerceAtLeast(1)).coerceIn(0.0,1.0).toFloat()
    fun crop(width:Int,height:Int,anchorX:Float,anchorY:Float):IntArray {
        require(width>0&&height>0);val side=minOf(width,height)
        return intArrayOf(((width-side)*anchorX.coerceIn(0f,1f)).toInt(),((height-side)*anchorY.coerceIn(0f,1f)).toInt(),side)
    }
    fun ranked(counts:Map<Song,Long>):List<Pair<Song,Long>> = counts.entries.sortedWith(compareByDescending<Map.Entry<Song,Long>>{it.value}.thenBy{it.key.title.lowercase()}.thenBy{it.key.id}).map{it.key to it.value}
    fun explicitBadge(icon:String)=icon.equals("MUSIC_EXPLICIT_BADGE",true)||icon.equals("EXPLICIT",true)
}
