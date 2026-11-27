package com.abhi.ganasuno.player

/** Pure policy: the service owns IO and player mutations on their respective threads. */
object EndlessQueuePolicy {
    const val LOW_WATER = 5
    const val BATCH_SIZE = 15
    const val KEEP_HISTORY = 50
    const val RECENT_LIMIT = 150
    const val RETRY_MS = 15_000L
    fun shouldRefill(enabled:Boolean, active:Boolean, manual:Boolean, repeatOff:Boolean, live:Boolean, ahead:Int):Boolean =
        enabled && !live && (active || manual) && (repeatOff || manual) && (manual || ahead <= LOW_WATER)
    fun <T> fresh(preferred:List<T>, fallback:List<T>, excluded:Set<String>, id:(T)->String):List<T> =
        (preferred+fallback).distinctBy(id).filter{ id(it) !in excluded }.take(BATCH_SIZE)
    fun trimPrefix(current:Int, shuffle:Boolean):Int = if(shuffle) 0 else (current-KEEP_HISTORY).coerceAtLeast(0)
}
