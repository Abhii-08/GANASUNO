package com.abhi.ganasuno.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import org.json.JSONObject
import java.time.LocalDate
import java.util.concurrent.Executors

/** A confirmed MusicBrainz artist identity rather than a guessed name match. */
object ArtistReleaseWatch {
    private const val JOB=8830
    private const val CHANNEL="artist_releases_v28"
    private fun prefs(c:Context)=c.getSharedPreferences("artist_releases_v28",Context.MODE_PRIVATE)
    fun contact(c:Context)=prefs(c).getString("contact","").orEmpty()
    fun contact(c:Context,raw:String){val value=Batch21.contact(raw)?:error("Enter a valid maintainer email");prefs(c).edit().putString("contact",value).apply();schedule(c)}
    private fun root(c:Context)=runCatching{JSONObject(prefs(c).getString("artists","{}").orEmpty())}.getOrDefault(JSONObject())
    fun watched(c:Context):Map<String,String>{val o=root(c);return o.keys().asSequence().associateWith{o.optString(it)}}
    @Synchronized fun watch(c:Context,id:String,name:String){require(Batch21.validId(id)&&name.isNotBlank()&&name.length<=300);val o=root(c);require(o.has(id)||o.length()<20){"Maximum 20 artists"};o.put(id,name);prefs(c).edit().putString("artists",o.toString()).apply();LibraryStore.setArtistFollowing(name.take(200),true);schedule(c)}
    @Synchronized fun unwatch(c:Context,id:String){val o=root(c);o.remove(id);prefs(c).edit().putString("artists",o.toString()).remove("seen:$id").apply();schedule(c)}
    fun schedule(c:Context){val s=c.getSystemService(JobScheduler::class.java);if(contact(c).isBlank()||watched(c).isEmpty()){s.cancel(JOB);return};if(s.getPendingJob(JOB)==null)check(s.schedule(JobInfo.Builder(JOB,ComponentName(c,ArtistReleaseJob::class.java)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(86400000L).setPersisted(true).build())==JobScheduler.RESULT_SUCCESS)}
    fun fetch(c:Context,id:String):List<Batch28.Release>{val today=LocalDate.now();return MusicCatalogue.recentArtist(contact(c),id,50).filter{Batch21.isRecent(it.date,today)}.map{Batch28.Release(it.id,it.title,it.date)}}
    @Synchronized fun deliver(c:Context,id:String,rows:List<Batch28.Release>):Int {
        val name=watched(c)[id]?:return 0;if(LibraryStore.followed().none{it.equals(name.take(200),true)})return 0
        val p=prefs(c);val old=p.getStringSet("seen:$id",null);val fresh=Batch28.unseen(rows,old.orEmpty(),old!=null,LocalDate.now());val nm=c.getSystemService(NotificationManager::class.java)
        if(fresh.isNotEmpty()){
            nm.createNotificationChannel(NotificationChannel(CHANNEL,"Followed artist releases",NotificationManager.IMPORTANCE_DEFAULT))
            if(!nm.areNotificationsEnabled()||nm.getNotificationChannel(CHANNEL)?.importance==NotificationManager.IMPORTANCE_NONE)return 0
            val item=fresh.first();val pending=PendingIntent.getActivity(c,id.hashCode(),Intent(Intent.ACTION_VIEW,Uri.parse(Batch21.url("release-group",item.id))),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            nm.notify(id.hashCode(),Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("New release · $name").setContentText(item.title).setStyle(Notification.BigTextStyle().bigText(fresh.joinToString("\n"){it.title})).setAutoCancel(true).setContentIntent(pending).build())
        }
        p.edit().putStringSet("seen:$id",(rows.map{it.id}+old.orEmpty()).distinct().take(500).toSet()).apply();return fresh.size
    }
    fun run(c:Context,active:()->Boolean):Int{var failures=0;for((id,name) in watched(c)){if(!active())break;if(LibraryStore.followed().none{it.equals(name.take(200),true)})continue;runCatching{val rows=fetch(c,id);if(active())deliver(c,id,rows)}.onFailure{failures++}};return failures}
}
class ArtistReleaseJob:JobService(){private val worker=Executors.newSingleThreadExecutor();@Volatile private var running:JobParameters?=null
    override fun onStartJob(params:JobParameters):Boolean{if(ArtistReleaseWatch.contact(this).isBlank())return false;running=params;worker.execute{val failed=runCatching{ArtistReleaseWatch.run(this){running===params}}.getOrDefault(1);Handler(mainLooper).post{if(running===params){running=null;jobFinished(params,failed>0)}}};return true}
    override fun onStopJob(params:JobParameters):Boolean{running=null;return true}
    override fun onDestroy(){running=null;worker.shutdownNow();super.onDestroy()}
}
