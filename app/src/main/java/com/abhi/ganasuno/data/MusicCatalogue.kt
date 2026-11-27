package com.abhi.ganasuno.data

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONArray
import java.time.LocalDate
import android.os.SystemClock

/** Read-only metadata. No guessed playable IDs, no credentials and no audio download. */
object MusicCatalogue {
    data class Item(val kind:String,val id:String,val title:String,val artist:String="",val date:String="",val type:String="",val note:String="")
    data class Track(val number:String,val title:String,val artist:String,val durationMs:Long)
    data class Release(val title:String,val date:String,val country:String,val tracks:List<Track>)
    private val client=YtApi.http.newBuilder().followRedirects(false).followSslRedirects(false).build()
    private var last=0L
    @Synchronized private fun request(contact:String,kind:String,id:String?=null,params:Map<String,String> = emptyMap()):JSONObject {
        require(Batch21.contact(contact)!=null&&kind in Batch21.kinds);require(id==null||Batch21.validId(id))
        val url="https://musicbrainz.org/ws/2/".toHttpUrl().newBuilder().addPathSegment(kind)
        if(id!=null)url.addPathSegment(id)
        url.addQueryParameter("fmt","json");params.forEach{(k,v)->url.addQueryParameter(k,v)}
        val wait=1100L-(SystemClock.elapsedRealtime()-last);if(wait>0)Thread.sleep(wait)
        last=SystemClock.elapsedRealtime()
        val req=Request.Builder().url(url.build()).header("User-Agent","GANASUNO/22.0 ($contact)").header("Accept","application/json").build()
        return client.newCall(req).execute().use{r->check(r.isSuccessful){"MusicBrainz HTTP ${r.code}"};val s=(r.body?:error("Empty metadata response")).source();require(!s.request(2*1024*1024L+1)){"Metadata response exceeds limit"};JSONObject(s.readUtf8())}
    }
    private fun credit(o:JSONObject):String {val a=o.optJSONArray("artist-credit")?:return "";return (0 until minOf(a.length(),30)).mapNotNull{i->a.optJSONObject(i)?.let{it.optString("name",it.optJSONObject("artist")?.optString("name").orEmpty())+it.optString("joinphrase")}}.joinToString("").let{Batch21.text(it)}}
    private fun item(kind:String,o:JSONObject):Item?{val id=o.optString("id");val title=Batch21.text(o.optString(if(kind=="artist")"name" else "title"));if(!Batch21.validId(id)||title.isBlank())return null;return Item(kind,id,title,credit(o),Batch21.text(o.optString("first-release-date",o.optString("date")),10),Batch21.text(o.optString("primary-type",o.optString("type")),80),Batch21.text(o.optString("disambiguation")))}
    private fun rows(root:JSONObject,kind:String,limit:Int):List<Item>{val a=root.optJSONArray(when(kind){"release-group"->"release-groups";else->kind+"s"})?:JSONArray();return Batch21.distinctValid((0 until minOf(a.length(),100)).mapNotNull{i->a.optJSONObject(i)?.let{item(kind,it)}},{it.id},limit)}
    fun search(contact:String,kind:String,q:String,limit:Int)=rows(request(contact,kind,params=mapOf("query" to Batch21.search(kind,q),"limit" to Batch21.limit(limit).toString())),kind,limit)
    fun alternatives(contact:String,title:String,artist:String,limit:Int):List<Item>{val q="recording:${Batch21.quoted(title)} AND artist:${Batch21.quoted(artist)}";return rows(request(contact,"recording",params=mapOf("query" to q,"limit" to Batch21.limit(limit).toString())),"recording",limit)}
    fun recent(contact:String,q:String,limit:Int):List<Item>{val today=LocalDate.now();return rows(request(contact,"release-group",params=mapOf("query" to Batch21.recent(q,today),"limit" to Batch21.limit(limit).toString())),"release-group",limit).filter{Batch21.artistTypes(it.type)&&Batch21.isRecent(it.date,today)}.sortedByDescending{it.date}}
    fun recentArtist(contact:String,id:String,limit:Int):List<Item>{require(Batch21.validId(id));val today=LocalDate.now();val query="arid:$id AND "+Batch21.recent("",today);return rows(request(contact,"release-group",params=mapOf("query" to query,"limit" to Batch21.limit(limit).toString())),"release-group",limit).filter{Batch21.artistTypes(it.type)&&Batch21.isRecent(it.date,today)}.sortedByDescending{it.date}}
    fun browse(contact:String,kind:String,filter:String,id:String,limit:Int):List<Item>{require(Batch21.validId(id));require((kind to filter) in setOf("recording" to "artist","release-group" to "artist","release" to "release-group","release" to "track_artist"));return rows(request(contact,kind,params=mapOf(filter to id,"limit" to Batch21.limit(limit).toString(),"inc" to "artist-credits")),kind,limit)}
    data class BrowsePage(val items:List<Item>,val next:Int?)
    fun browsePage(contact:String,kind:String,filter:String,id:String,offset:Int,limit:Int):BrowsePage {
        require(Batch21.validId(id)&&offset in 0..10000);require((kind to filter) in setOf("recording" to "artist","release-group" to "artist","release" to "release-group","release" to "track_artist"));val size=Batch21.limit(limit);val o=request(contact,kind,params=mapOf(filter to id,"limit" to size.toString(),"offset" to offset.toString(),"inc" to "artist-credits"));val total=o.optInt("$kind-count",o.optInt("count",0));return BrowsePage(rows(o,kind,size),Batch22.nextOffset(offset,size,total))
    }
    fun artistInfo(contact:String,id:String):String{val o=request(contact,"artist",id,mapOf("inc" to "genres"));val item=item("artist",o)?:error("Missing artist");return buildString{appendLine(item.title);appendLine(item.type);appendLine(item.note);appendLine("Country: ${o.optString("country").ifBlank{"Unknown"}}");appendLine("Area: ${o.optJSONObject("area")?.optString("name").orEmpty()}");val years=o.optJSONObject("life-span");appendLine("Active from: ${years?.optString("begin").orEmpty()}");val genres=o.optJSONArray("genres")?:JSONArray();appendLine("Genres: "+(0 until minOf(genres.length(),15)).mapNotNull{genres.optJSONObject(it)?.optString("name")}.joinToString(", "))}}
    fun release(contact:String,id:String):Release {val o=request(contact,"release",id,mapOf("inc" to "recordings+artist-credits"));val item=item("release",o)?:error("Missing release");val media=o.optJSONArray("media")?:JSONArray();val tracks=mutableListOf<Track>();for(i in 0 until minOf(media.length(),30)){val m=media.optJSONObject(i)?:continue;val a=m.optJSONArray("tracks")?:continue;for(j in 0 until minOf(a.length(),500-tracks.size)){val t=a.optJSONObject(j)?:continue;val title=Batch21.text(t.optString("title",t.optJSONObject("recording")?.optString("title").orEmpty()));if(title.isBlank())continue;tracks.add(Track("${i+1}.${Batch21.text(t.optString("number",(j+1).toString()),20)}",title,credit(t).ifBlank{item.artist},t.optLong("length",0).coerceIn(0,86400000)))}};return Release(item.title,item.date,Batch21.text(o.optString("country"),20),tracks)}
}
