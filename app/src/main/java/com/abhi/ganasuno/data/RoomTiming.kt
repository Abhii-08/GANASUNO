package com.abhi.ganasuno.data
object RoomTiming {
    const val MAX_RTT=4000L
    fun estimate(samples:List<Long>):Long=samples.takeLast(8).filter{it in 0..MAX_RTT}.minOrNull()?.div(2)?:0L
    fun publicationAge(capture:Long,dispatch:Long,samples:List<Long>):Long=((dispatch-capture).coerceIn(0,2000)+estimate(samples)).coerceAtMost(2000)
    fun arrivalAge(rtt:Long,dispatchDelay:Long,samples:List<Long>):Long=(estimate(samples+listOf(rtt))+dispatchDelay.coerceIn(0,2000)).coerceAtMost(2000)
}
