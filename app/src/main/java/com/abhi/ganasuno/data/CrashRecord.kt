package com.abhi.ganasuno.data

import android.content.Context
import java.io.File

/** Local crash summary only: no exception messages, URLs, song titles or keys. Never uploads. */
object CrashRecord {
    private fun file(c:Context)=File(c.filesDir,"last-crash.txt")
    fun install(c:Context){
        val prior=Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread,error ->
            runCatching{val tmp=File(c.filesDir,"last-crash.tmp");tmp.writeText(buildString{
                appendLine("Uncaught ${error.javaClass.name}")
                appendLine("Android API ${android.os.Build.VERSION.SDK_INT}")
                error.stackTrace.take(12).forEach{appendLine("${it.className}.${it.methodName}:${it.lineNumber}")}
            });java.nio.file.Files.move(tmp.toPath(),file(c).toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING)}
            if(prior!=null)prior.uncaughtException(thread,error)else android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
    fun pending(c:Context):String?=runCatching{file(c).takeIf{it.exists()}?.readText()?.take(4000)}.getOrNull()
    fun stamp(c:Context)=file(c).lastModified()
    fun clear(c:Context){file(c).delete();File(c.filesDir,"last-crash.tmp").delete()}
}
