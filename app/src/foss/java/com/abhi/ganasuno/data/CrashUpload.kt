package com.abhi.ganasuno.data
object CrashUpload {
    const val available=false
    fun send(dsn:String,report:String):String=error("Crash upload is unavailable in this edition")
}
