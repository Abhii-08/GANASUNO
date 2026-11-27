package com.abhi.ganasuno.data

import com.abhi.ganasuno.BuildConfig
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.Executors
import java.security.MessageDigest

/** Opt-in, Full edition, process-memory credentials only. Never sends the audio or a stream URL. */
object LastFmScrobble {
    private data class Session(val apiKey:String,val secret:String,val sessionKey:String)
    @Volatile private var session:Session?=null
    @Volatile private var status="Not connected"
    private val worker=Executors.newSingleThreadExecutor()
    private val client=YtApi.http.newBuilder().followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build()
    fun available()=BuildConfig.FLAVOR=="full"
    fun active()=session!=null
    fun status()=status
    fun configure(key:String,secret:String,sessionKey:String){require(available());require(listOf(key,secret,sessionKey).all{it.matches(Regex("[A-Za-z0-9]{16,128}"))}){"Enter valid API credentials and an authorized session key"};session=Session(key,secret,sessionKey);status="Enabled for this app process; credentials not stored"}
    fun disconnect(){session=null;status="Disconnected"}
    fun submit(song:Song,duration:Long,played:Long,started:Long):Boolean{
        val auth=session?:return false
        if(song.live||song.title.isBlank()||song.artist.isBlank()||!Batch28.scrobbleEligible(duration,played)||started<=0)return false
        worker.execute {
            if(session!==auth)return@execute
            val result=runCatching{
                val fields=sortedMapOf("method" to "track.scrobble","api_key" to auth.apiKey,"sk" to auth.sessionKey,"track" to song.title.take(300),"artist" to song.artist.take(200),"timestamp" to started.toString(),"duration" to (duration/1000).toString())
                if(song.album.isNotBlank())fields["album"]=song.album.take(200)
                val sig=Batch28.lastFmSignature(fields,auth.secret)
                val body=FormBody.Builder();fields.forEach{(k,v)->body.add(k,v)};body.add("api_sig",sig).add("format","json")
                client.newCall(Request.Builder().url("https://ws.audioscrobbler.com/2.0/").post(body.build()).build()).execute().use{r->check(r.isSuccessful);val source=(r.body?:error("Empty response")).source();require(!source.request(262145L));val json=JSONObject(source.readUtf8());check(!json.has("error"));val attr=json.optJSONObject("scrobbles")?.optJSONObject("@attr")?:error("Unconfirmed response");check(attr.optInt("accepted",0)==1)}
            }
            if(session===auth)status=if(result.isSuccess)"Last track accepted by Last.fm" else "Last submission not confirmed; no automatic retry"
        }
        return true
    }
}
