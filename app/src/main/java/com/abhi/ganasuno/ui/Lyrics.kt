package com.abhi.ganasuno.ui

import com.abhi.ganasuno.data.Song
import com.abhi.ganasuno.data.YtApi
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONArray

object Lyrics {
    fun parse(raw:String) = LrcParser.parse(raw)
    val providers=listOf("LRCLIB (synced or plain)","Lyrics.ovh (plain)","Local saved lyrics")
    fun fetch(s:Song,provider:Int=0):String {
        if(provider==2)return com.abhi.ganasuno.data.LibraryStore.lyric(s.id).ifBlank{error("No local lyrics saved")}
        if(provider==1){
            val url="https://api.lyrics.ovh/v1/".toHttpUrl().newBuilder().addPathSegment(s.artist).addPathSegment(s.title).build()
            return YtApi.http.newCall(Request.Builder().url(url).build()).execute().use{r->check(r.isSuccessful){"Lyrics service HTTP ${r.code}"};org.json.JSONObject(r.body?.string().orEmpty()).optString("lyrics").trim().take(100000).ifBlank{error("No lyrics returned")}}
        }
        val url="https://lrclib.net/api/search".toHttpUrl().newBuilder().addQueryParameter("track_name",s.title).addQueryParameter("artist_name",s.artist).build()
        YtApi.http.newCall(Request.Builder().url(url).header("User-Agent","GANASUNO/11.0 Android music player").build()).execute().use { r ->
            check(r.isSuccessful){"Lyrics service HTTP ${r.code}"}
            val a=JSONArray(r.body?.string().orEmpty())
            val matches=(0 until a.length()).map{a.getJSONObject(it)}.filter{it.optString("trackName").equals(s.title,true)&&it.optString("artistName").equals(s.artist,true)}
            val best=matches.firstOrNull{!it.isNull("syncedLyrics")}?:matches.firstOrNull()?:error("No exact lyrics match. Add your own LRC instead.")
            return if(!best.isNull("syncedLyrics"))best.getString("syncedLyrics") else if(!best.isNull("plainLyrics"))best.getString("plainLyrics") else error("No lyrics available")
        }
    }
}
