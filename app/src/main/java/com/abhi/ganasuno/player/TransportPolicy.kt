package com.abhi.ganasuno.player

/** UI transport decisions shared by both player surfaces. No network or queue mutation. */
internal object TransportPolicy {
    enum class Action { EMPTY, ERROR, PAUSE, RESTART, PREPARE, PLAY }
    data class Skip(val seek:Boolean,val prepare:Boolean)
    fun toggle(hasItem:Boolean,requested:Boolean,idle:Boolean,ended:Boolean,error:Boolean):Action = when {
        !hasItem -> Action.EMPTY
        error -> Action.ERROR
        ended -> Action.RESTART
        requested && !idle -> Action.PAUSE
        idle -> Action.PREPARE
        else -> Action.PLAY
    }
    fun skip(forward:Boolean,hasNext:Boolean,hasPrevious:Boolean,idle:Boolean,ended:Boolean,error:Boolean):Skip {
        val available=if(forward)hasNext else hasPrevious
        return Skip(available,available&&(idle||ended||error))
    }
}
