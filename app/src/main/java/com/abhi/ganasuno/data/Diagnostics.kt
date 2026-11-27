package com.abhi.ganasuno.data

/** In-memory error codes only; no titles, credentials or stream URLs. */
object Diagnostics {
    private val events=ArrayDeque<String>()
    @Synchronized fun event(code:String){if(events.size>=100)events.removeFirst();events.addLast("${java.time.LocalDateTime.now().withNano(0)}  $code")}
    @Synchronized fun read()=events.joinToString("\n")
    @Synchronized fun clear()=events.clear()
}
