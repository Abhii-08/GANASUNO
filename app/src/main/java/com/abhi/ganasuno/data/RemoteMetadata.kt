package com.abhi.ganasuno.data

import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import android.os.SystemClock

/** Explicit, read-only provider calls. No credentials or persistent tracking. */
object RemoteMetadata {
    private val client=YtApi.http.newBuilder().followRedirects(false).followSslRedirects(false).build()
    private fun get(url:String,max:Long,agent:String="GANASUNO/14.0"):String=client.newCall(Request.Builder().url(url).header("User-Agent",agent).build()).execute().use{r->check(r.isSuccessful){"Provider HTTP ${r.code}"};val source=(r.body?:error("Empty response")).source();require(!source.request(max+1)){"Response exceeds size limit"};source.readUtf8()}
    @Volatile private var tree:List<String>?=null
    @Synchronized fun profiles(query:String):List<Batch14.Profile>{val paths=tree?:run{val root=JSONObject(get("https://api.github.com/repos/jaakkopasanen/AutoEq/git/trees/master?recursive=1",24L*1024*1024));val rows=root.getJSONArray("tree");(0 until rows.length()).mapNotNull{i->rows.optJSONObject(i)?.optString("path")?.takeIf{Batch14.profilePath(it)!=null}}.also{require(it.isNotEmpty()){ "No GraphicEQ entries returned" };tree=it}};return Batch14.findProfiles(paths,query)}
    fun graphic(profile:Batch14.Profile):String{require(Batch14.profilePath(profile.path)!=null);val url="https://raw.githubusercontent.com/jaakkopasanen/AutoEq/master".toHttpUrl().newBuilder();profile.path.split('/').forEach{url.addPathSegment(it)};return get(url.build().toString(),65536)}
    data class Recording(val title:String,val artist:String,val year:Int,val id:String)
    private var lastRequest=0L
    @Synchronized fun years(song:Song):List<Recording>{require(song.title.isNotBlank()&&song.artist.isNotBlank());val delay=1100-(SystemClock.elapsedRealtime()-lastRequest);if(delay>0)Thread.sleep(delay);lastRequest=SystemClock.elapsedRealtime();val url="https://musicbrainz.org/ws/2/recording".toHttpUrl().newBuilder().addQueryParameter("query","recording:${Batch14.lucene(song.title)} AND artist:${Batch14.lucene(song.artist)}").addQueryParameter("fmt","json").addQueryParameter("limit","15").build();val a=JSONObject(get(url.toString(),512*1024L,"GANASUNO/14.0 (Android music library; MusicBrainz metadata lookup)")).optJSONArray("recordings")?:return emptyList();return (0 until a.length()).mapNotNull{i->val o=a.optJSONObject(i)?:return@mapNotNull null;val y=Batch13.year(o.optString("first-release-date"))?:return@mapNotNull null;val title=o.optString("title");val credits=o.optJSONArray("artist-credit");val artist=(0 until (credits?.length()?:0)).mapNotNull{j->credits?.optJSONObject(j)?.optString("name")}.joinToString(", ");if(!Batch14.matchTitle(song.title,title))null else Recording(title,artist,y,o.optString("id"))}.distinctBy{Triple(it.title,it.artist,it.year)}.sortedBy{it.year}}
}
