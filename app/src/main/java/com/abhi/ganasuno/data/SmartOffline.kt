package com.abhi.ganasuno.data

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

/** Only explicitly registered direct-file URLs; never uses stream extraction for downloads. */
object SmartOffline {
    private const val DAILY=8828
    private const val SOON=8829
    private fun prefs(c:Context)=c.getSharedPreferences("smart_offline_v28",Context.MODE_PRIVATE)
    fun target(c:Context)=prefs(c).getInt("quality",192)
    fun setTarget(c:Context,kbps:Int){require(kbps in 32..1411);prefs(c).edit().putInt("quality",kbps).apply()}
    fun auto(c:Context)=prefs(c).getBoolean("auto",false)
    fun smart(c:Context)=prefs(c).getBoolean("smart",false)
    fun configure(c:Context,auto:Boolean,smart:Boolean){prefs(c).edit().putBoolean("auto",auto).putBoolean("smart",smart).apply();schedule(c);if(auto)kick(c)}
    private fun root(c:Context)=runCatching{JSONObject(prefs(c).getString("sources","{}").orEmpty())}.getOrDefault(JSONObject())
    @Synchronized fun register(c:Context,id:String,variants:List<Batch28.Variant>){require(id.length in 1..2048);val verified=Batch28.parse(variants.joinToString("\n"){"${it.kbps} ${it.url}"});val o=root(c);require(o.has(id)||o.length()<200){"Maximum 200 registered sources"};o.put(id,JSONArray().apply{verified.forEach{put(JSONObject().put("kbps",it.kbps).put("url",it.url))}});check(prefs(c).edit().putString("sources",o.toString()).commit());if(auto(c))kick(c)}
    @Synchronized fun forget(c:Context,id:String){val o=root(c);o.remove(id);prefs(c).edit().putString("sources",o.toString()).remove("saved:$id").apply()}
    fun variants(c:Context,id:String):List<Batch28.Variant>{val a=root(c).optJSONArray(id)?:return emptyList();return (0 until a.length()).mapNotNull{n->val o=a.optJSONObject(n)?:return@mapNotNull null;val url=Batch28.direct(o.optString("url"))?:return@mapNotNull null;val kbps=o.optInt("kbps");if(kbps !in 32..1411)null else Batch28.Variant(kbps,url)}}
    fun registered(c:Context)=root(c).keys().asSequence().toSet()
    private fun saved(c:Context,id:String):Boolean {val localId=prefs(c).getString("saved:$id",null)?:return false;return LibraryStore.list("local").any{it.id==localId&&OfflineFiles.owned(c,it)&&runCatching{c.contentResolver.openFileDescriptor(android.net.Uri.parse(it.localUri),"r")?.use{fd->fd.statSize>0}?:false}.getOrDefault(false)}}
    private val saveLock=Any()
    fun save(c:Context,id:String,token:OfflineFiles.Cancellation=OfflineFiles.Cancellation()):Song = synchronized(saveLock) {
        token.check()
        if(saved(c,id)){val localId=prefs(c).getString("saved:$id",null);return@synchronized LibraryStore.list("local").first{it.id==localId}}
        val choice=Batch28.select(variants(c,id),target(c))?:error("No authorized source registered")
        OfflineFiles.fetch(c,choice.url,token).also{prefs(c).edit().putString("saved:$id",it.id).commit()}
    }
    private fun manager(c:Context)=c.getSystemService(JobScheduler::class.java)
    fun schedule(c:Context){val s=manager(c);if(!auto(c)&&!smart(c)){s.cancel(DAILY);s.cancel(SOON);return};if(s.getPendingJob(DAILY)==null)check(s.schedule(JobInfo.Builder(DAILY,ComponentName(c,SmartOfflineJob::class.java)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED).setPeriodic(86400000L).setPersisted(true).build())==JobScheduler.RESULT_SUCCESS)}
    fun kick(c:Context){if(!auto(c))return;val s=manager(c);if(s.getPendingJob(SOON)==null)s.schedule(JobInfo.Builder(SOON,ComponentName(c,SmartOfflineSoonJob::class.java)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED).setMinimumLatency(1000L).build())}
    fun wifi(c:Context):Boolean{val m=c.getSystemService(ConnectivityManager::class.java);val caps=m.getNetworkCapabilities(m.activeNetwork)?:return false;return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)&&caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)}
    fun status(c:Context)=prefs(c).getString("last", "No automatic run yet").orEmpty()
    fun refresh(c:Context,active:()->Boolean, favoritesOnly:Boolean,newToken:()->OfflineFiles.Cancellation):Boolean{
        if(!wifi(c))return true
        val registered=registered(c);val done=registered.filter{saved(c,it)}.toSet()
        val rows=Batch28.candidates(LibraryStore.list("favorites").map{it.id},LibraryStore.mostPlayed().map{it.first.id},registered,done,auto(c),smart(c)&&!favoritesOnly,5)
        var success=0;var failures=0
        for(id in rows){if(!active()||!wifi(c)||(!auto(c)&&!smart(c)))break; if(OfflineFiles.size(c)>400L*1024*1024)break;runCatching{save(c,id,newToken())}.onSuccess{success++}.onFailure{failures++}}
        prefs(c).edit().putString("last","${java.util.Date()} · saved $success · failed $failures · no automatic deletion").apply();return failures==0
    }
    fun isSoon(params:JobParameters)=params.jobId==SOON
}
open class SmartOfflineJob:JobService(){
    private val worker=Executors.newSingleThreadExecutor();@Volatile private var running:JobParameters?=null;@Volatile private var token:OfflineFiles.Cancellation?=null
    override fun onStartJob(params:JobParameters):Boolean{if(!SmartOffline.auto(this)&&!SmartOffline.smart(this))return false;running=params;worker.execute{val ok=runCatching{SmartOffline.refresh(this,{running===params},SmartOffline.isSoon(params)){OfflineFiles.Cancellation().also{token=it;if(running!==params)it.cancel()}}}.getOrDefault(false);Handler(mainLooper).post{if(running===params){running=null;jobFinished(params,!ok)}}};return true}
    override fun onStopJob(params:JobParameters):Boolean{running=null;token?.cancel();return true}
    override fun onDestroy(){running=null;token?.cancel();worker.shutdownNow();super.onDestroy()}
}

class SmartOfflineSoonJob:SmartOfflineJob()
