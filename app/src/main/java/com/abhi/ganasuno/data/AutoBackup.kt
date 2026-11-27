package com.abhi.ganasuno.data

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.app.job.JobService
import android.app.job.JobParameters
import android.content.ComponentName
import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.util.concurrent.Executors

/** Private snapshots, no account, no uploads. Android may defer periodic jobs. */
object AutoBackup {
    private fun directory(c: Context) = File(c.filesDir, "library-snapshots").apply { mkdirs() }
    fun files(c: Context) = directory(c).listFiles().orEmpty().filter { it.name.matches(Regex("[0-9]+\\.json")) }.sortedByDescending { it.name.substringBefore('.').toLong() }
    @Synchronized fun save(c: Context) {
        val target = AtomicFile(File(directory(c), "${System.currentTimeMillis()}.json"))
        val out = target.startWrite()
        try { out.write(LibraryStore.backup().toByteArray(Charsets.UTF_8)); target.finishWrite(out) }
        catch (e: Exception) { target.failWrite(out); throw e }
        files(c).drop(LibraryStore.option("backup_keep", 5).coerceIn(1,20)).forEach { it.delete() }
    }
    fun audioFiles(c:Context)=directory(c).listFiles().orEmpty().filter{it.name.matches(Regex("[0-9]+\\.zip"))}.sortedByDescending{it.name.substringBefore('.').toLong()}
    @Synchronized fun saveAudio(c:Context){val dir=directory(c);val target=File(dir,"${System.currentTimeMillis()}.zip");val tmp=File.createTempFile("snapshot-",".tmp",dir);try{require(dir.usableSpace>OfflineFiles.size(c)+64L*1024*1024){"Insufficient space for audio backup"};tmp.outputStream().use{MediaArchive.write(c,it,TransferSettings.export())};check(tmp.renameTo(target)){"Could not commit snapshot"};audioFiles(c).drop(LibraryStore.option("backup_keep",5).coerceIn(1,20)).forEach{it.delete()}}finally{tmp.delete()}}
    fun schedule(c: Context): Boolean {
        val scheduler = c.getSystemService(JobScheduler::class.java)
        scheduler.cancel(8721)
        val days = LibraryStore.option("backup_days")
        if (days <= 0) return true
        return scheduler.schedule(JobInfo.Builder(8721, ComponentName(c, BackupJob::class.java)).setPeriodic(days * 86400000L).setPersisted(true).build()) == JobScheduler.RESULT_SUCCESS
    }
}
class BackupJob : JobService() {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var running: JobParameters? = null
    override fun onStartJob(params: JobParameters): Boolean {
        running = params
        executor.execute {
            val failed = runCatching { AutoBackup.save(this);if(LibraryStore.option("backup_audio")==1)AutoBackup.saveAudio(this) }.isFailure
            android.os.Handler(mainLooper).post { if (running === params) { running = null; jobFinished(params, failed) } }
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean { running = null; return true }
    override fun onDestroy() { executor.shutdown(); super.onDestroy() }
}
