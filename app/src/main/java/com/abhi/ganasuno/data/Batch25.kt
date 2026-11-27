package com.abhi.ganasuno.data

/** Pure validation and segment selection; independent of Android and network. */
object Batch25 {
    val cacheChoices = listOf(16,32,64,128,256,512)
    fun cacheMiB(value:Int) = value.takeIf{it in cacheChoices} ?: 64
    fun cacheBytes(value:Int) = cacheMiB(value)*1048576L
    val categories = listOf("sponsor","selfpromo","interaction","intro","outro","preview","music_offtopic","filler","poi_highlight")
    val labels = listOf("Sponsor","Self promotion","Interaction reminder","Intro","Outro","Preview / recap","Non-music section","Filler","Highlight (manual seek only)")
    const val ALL = 511
    fun selected(mask:Int)=categories.filterIndexed{i,_->mask and (1 shl i)!=0}
    fun videoId(id:String)=id.matches(Regex("[A-Za-z0-9_-]{11}"))
    data class Segment(val start:Long,val end:Long,val category:String,val action:String)
    fun segment(start:Double,end:Double,category:String,action:String):Segment? {
        if(category !in categories||!start.isFinite()||!end.isFinite()||start<0||end<start||end>86400)return null
        if(action=="skip"&&(category=="poi_highlight"||end-start<0.05))return null
        if(action=="poi"&&category!="poi_highlight")return null
        if(action !in setOf("skip","poi"))return null
        return Segment((start*1000).toLong(),(end*1000).toLong(),category,action)
    }
    fun target(position:Long,duration:Long,segments:List<Segment>,mask:Int):Long? {
        if(position<0||duration<=0||position>=duration)return null
        val allowed=selected(mask).toSet()
        val valid=segments.filter{it.action=="skip"&&it.category in allowed&&it.start>=0&&it.end>it.start&&it.end<=duration}
        var target=position
        // Merge overlapping adjacent ranges without seeking backwards.
        repeat(valid.size){val next=valid.filter{target>=it.start&&target<it.end}.maxOfOrNull{it.end}?:return target.takeIf{it>position};target=next}
        return target.takeIf{it>position}
    }
    fun count(raw:String?):Long?=raw?.toLongOrNull()?.takeIf{it>=0}
    fun safeText(raw:String,max:Int)=raw.filterNot{it.isISOControl()&&it!='\n'}.trim().take(max)
    fun rankLabels(titles:List<String>)=titles.mapIndexed{i,t->"${i+1}. $t"}
}
