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
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.util.concurrent.Executors

/** Opt-in RSS/Atom blog updates; first successful fetch sets baseline, no automatic browser launch. */
object BlogWatch {
    private const val JOB=8726
    private const val CHANNEL="developer_blog"
    fun prefs(c:Context)=c.getSharedPreferences("blog_watch_v26",Context.MODE_PRIVATE)
    fun configure(c:Context,raw:String?){val p=prefs(c);val url=raw?.let{Batch26.https(it)?:error("Use an HTTPS feed URL")};if(p.getString("url",null)!=url)p.edit().clear().apply();if(url!=null)p.edit().putString("url",url).apply();schedule(c,true)}
    fun schedule(c:Context,replace:Boolean=false){val s=c.getSystemService(JobScheduler::class.java);if(replace)s.cancel(JOB);if(Batch26.https(prefs(c).getString("url","").orEmpty())==null){s.cancel(JOB);return};if(s.getPendingJob(JOB)==null){check(s.schedule(JobInfo.Builder(JOB,ComponentName(c,BlogJob::class.java)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(86400000L).setPersisted(true).build())==JobScheduler.RESULT_SUCCESS){"Could not schedule blog updates"}}}
    fun fetch(raw:String):List<Batch26.Post>{val url=Batch26.https(raw)?:error("Invalid feed");val client=YtApi.http.newBuilder().followRedirects(false).followSslRedirects(false).build();return client.newCall(Request.Builder().url(url).build()).execute().use{r->check(r.isSuccessful){"Feed HTTP ${r.code}"};val source=(r.body?:error("Missing feed")).source();require(!source.request(1048577L)){"Feed too large"};parse(source.readUtf8())}}
    fun parse(raw:String):List<Batch26.Post>{require(raw.length<=1048576&&!raw.contains("<!DOCTYPE",true)&&!raw.contains("<!ENTITY",true));val p=XmlPullParserFactory.newInstance().apply{isNamespaceAware=true}.newPullParser();p.setInput(raw.reader());var depth=-1;var id="";var title="";var link="";val out=mutableListOf<Batch26.Post>();var e=p.eventType;while(e!=XmlPullParser.END_DOCUMENT&&out.size<100){if(e==XmlPullParser.START_TAG){if(depth<0&&p.name in setOf("item","entry")){depth=p.depth;id="";title="";link=""}else if(depth>=0&&p.depth==depth+1){when(p.name){"id","guid"->id=p.nextText();"title"->title=p.nextText();"link"->{val href=p.getAttributeValue(null,"href");val rel=p.getAttributeValue(null,"rel");if(href==null)link=p.nextText()else if(rel==null||rel=="alternate")link=href}}}}else if(e==XmlPullParser.END_TAG&&p.depth==depth&&p.name in setOf("item","entry")){Batch26.post(id,title,link)?.let(out::add);depth=-1};e=p.next()};return out.distinctBy{it.id}}
    @Synchronized fun deliver(c:Context,url:String,rows:List<Batch26.Post>){val p=prefs(c);if(p.getString("url",null)!=url||rows.isEmpty())return;val old=p.getString("seen","").orEmpty().split(',').filter{it.length==64};val fresh=Batch26.newPosts(rows,old.toSet());val manager=c.getSystemService(NotificationManager::class.java);manager.createNotificationChannel(NotificationChannel(CHANNEL,"Developer blog",NotificationManager.IMPORTANCE_DEFAULT));if(fresh.isNotEmpty()){if(!manager.areNotificationsEnabled()||manager.getNotificationChannel(CHANNEL)?.importance==NotificationManager.IMPORTANCE_NONE)return;val post=fresh.first();val intent=PendingIntent.getActivity(c,8726,Intent(Intent.ACTION_VIEW,Uri.parse(post.link)),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);manager.notify(8726,Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(if(fresh.size==1)"New developer blog post" else "${fresh.size} new blog posts").setContentText(post.title).setStyle(Notification.BigTextStyle().bigText(fresh.take(5).joinToString("\n"){it.title})).setAutoCancel(true).setContentIntent(intent).build())};p.edit().putString("seen",Batch26.seen(rows,old).joinToString(",")).putLong("last",System.currentTimeMillis()).apply()}
}
class BlogJob:JobService(){private val worker=Executors.newSingleThreadExecutor();@Volatile private var running:JobParameters?=null
    override fun onStartJob(params:JobParameters):Boolean{val url=BlogWatch.prefs(this).getString("url",null)?:return false;running=params;worker.execute{val result=runCatching{BlogWatch.fetch(url)};Handler(mainLooper).post{if(running!==params)return@post;running=null;val ok=result.mapCatching{BlogWatch.deliver(this,url,it)}.isSuccess;jobFinished(params,!ok)}};return true}
    override fun onStopJob(params:JobParameters):Boolean{running=null;return true}
    override fun onDestroy(){running=null;worker.shutdownNow();super.onDestroy()}
}
