package com.abhi.ganasuno.data

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import java.util.concurrent.Executors

/** Opt-in periodic metadata checks; never downloads APKs or starts an activity in background. */
object UpdateScheduler {
    private const val JOB=8722
    fun prefs(c:Context)=c.getSharedPreferences("updates",Context.MODE_PRIVATE)
    fun schedule(c:Context,replace:Boolean=false):Boolean {val p=prefs(c);val scheduler=c.getSystemService(JobScheduler::class.java);if(!p.getBoolean("auto",false)||V8Core.httpsUrl(p.getString("url","").orEmpty())==null){scheduler.cancel(JOB);return true};if(!replace&&scheduler.getPendingJob(JOB)!=null)return true;if(replace)scheduler.cancel(JOB);return scheduler.schedule(JobInfo.Builder(JOB,ComponentName(c,UpdateJob::class.java)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(86400000L).setPersisted(true).build())==JobScheduler.RESULT_SUCCESS}
    fun reset(c:Context){prefs(c).edit().remove("last").remove("pending_code").remove("pending_name").remove("pending_url").remove("pending_manifest").remove("pending_channel").apply();schedule(c,true)}
    fun pending(c:Context):ReleaseCheck.Release?{val p=prefs(c);if(p.getString("pending_manifest",null)!=p.getString("url",null)||p.getInt("pending_channel",-1)!=p.getInt("channel",0))return null;val code=p.getInt("pending_code",0);val url=V8Core.httpsUrl(p.getString("pending_url","").orEmpty())?:return null;if(code<=0)return null;return ReleaseCheck.Release(code,p.getString("pending_name","").orEmpty(),url)}
    fun acknowledge(c:Context){prefs(c).edit().remove("pending_code").remove("pending_name").remove("pending_url").remove("pending_manifest").remove("pending_channel").apply()}
}
class UpdateJob:JobService(){
    private val worker=Executors.newSingleThreadExecutor()
    @Volatile private var running:JobParameters?=null
    override fun onStartJob(params:JobParameters):Boolean{val p=UpdateScheduler.prefs(this);val url=p.getString("url","").orEmpty();val channel=p.getInt("channel",0);if(!p.getBoolean("auto",false)||!Batch16.due(p.getLong("last",0),System.currentTimeMillis())||V8Core.httpsUrl(url)==null)return false;running=params;worker.execute{val result=runCatching{ReleaseCheck.check(url,channel)};Handler(mainLooper).post{if(running!==params)return@post;running=null;if(p.getBoolean("auto",false)&&p.getString("url","")==url&&p.getInt("channel",0)==channel){result.onSuccess{r->p.edit().putLong("last",System.currentTimeMillis()).putInt("pending_code",r.versionCode).putString("pending_name",r.versionName).putString("pending_url",r.url).putString("pending_manifest",url).putInt("pending_channel",channel).apply()}};jobFinished(params,result.isFailure)}};return true}
    override fun onStopJob(params:JobParameters):Boolean{running=null;return true}
    override fun onDestroy(){running=null;worker.shutdownNow();super.onDestroy()}
}
