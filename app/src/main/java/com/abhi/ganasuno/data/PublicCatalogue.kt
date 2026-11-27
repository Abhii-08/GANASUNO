package com.abhi.ganasuno.data

import android.content.Context
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONArray
import java.time.Instant

/** Read-only official YouTube Data API metadata. Key is supplied for this session only.
 * No API response is used to resolve a stream. Videos open on the official service. */
object PublicCatalogue {
    data class Entry(val kind:String,val id:String,val title:String,val channel:String,val channelId:String,val date:String,val description:String)
    data class Page(val entries:List<Entry>,val next:String)
    private val client=YtApi.http.newBuilder().followRedirects(false).followSslRedirects(false).build()
    private fun request(key:String,path:String,params:Map<String,String>):JSONObject {
        require(Batch16.keyLooksValid(key)){"Enter a valid Data API key"}
        require(path in setOf("search","videos","channels","playlists","playlistItems","commentThreads","comments"))
        val u="https://www.googleapis.com/youtube/v3/".toHttpUrl().newBuilder().addPathSegment(path)
        params.forEach{(k,v)->u.addQueryParameter(k,v)}
        // Keep the key out of query strings and user-visible errors.
        val request=Request.Builder().url(u.build()).header("X-Goog-Api-Key",key).build()
        return client.newCall(request).execute().use{r->check(r.isSuccessful){"Catalogue HTTP ${r.code}; check API access and quota"};val source=(r.body?:error("Empty response")).source();require(!source.request(2*1024*1024L+1)){"Catalogue response too large"};JSONObject(source.readUtf8())}
    }
    private fun entry(o:JSONObject,kindHint:String):Entry? {
        val s=o.optJSONObject("snippet")?:return null
        val searchId=o.optJSONObject("id")
        val kind=if(searchId!=null)searchId.optString("kind").substringAfterLast('#') else kindHint
        val id=if(searchId!=null)searchId.optString(kind+"Id") else if(kindHint=="playlistItem")s.optJSONObject("resourceId")?.optString("videoId").orEmpty() else o.optString("id")
        val actual=if(kindHint=="playlistItem")"video" else kind
        if(!Batch16.valid(actual,id))return null
        val title=Batch16.clean(s.optString("title"));if(title.isBlank()||title in setOf("Private video","Deleted video"))return null
        return Entry(actual,id,title,Batch16.clean(s.optString("videoOwnerChannelTitle",s.optString("channelTitle"))),s.optString("videoOwnerChannelId",s.optString("channelId")),s.optString("publishedAt"),Batch16.clean(s.optString("description"),3000))
    }
    private fun page(o:JSONObject,kind:String):Page {val a=o.optJSONArray("items")?:JSONArray();return Page((0 until a.length()).mapNotNull{i->a.optJSONObject(i)?.let{entry(it,kind)}}.distinctBy{it.kind to it.id},o.optString("nextPageToken"))}
    private fun collect(limit:Int,load:(Int,String)->Page):List<Entry> = Batch16.collect(limit,{e:Entry->e.kind to e.id}){size,token->val p=load(size,token);p.entries to p.next}
    fun search(key:String,q:String,kind:String,region:String,limit:Int,latest:Boolean=false,channel:String=""):List<Entry>{require(kind in setOf("video","channel","playlist"));require(q.isNotBlank()||Batch16.valid("channel",channel));return collect(Batch16.homeLimit(limit)){size,token->val p=mutableMapOf("part" to "snippet","type" to kind,"maxResults" to size.toString(),"regionCode" to region,"q" to q.take(160));if(token.isNotBlank())p["pageToken"]=token;if(channel.isNotBlank()){require(Batch16.valid("channel",channel));p["channelId"]=channel};if(kind=="video")p["videoCategoryId"]="10";if(latest){require(kind=="video");p["order"]="date";p["publishedAfter"]=Batch16.since(Instant.now(),30)};page(request(key,"search",p),kind)}}
    fun popular(key:String,region:String,limit:Int)=collect(Batch16.homeLimit(limit)){size,token->val p=mutableMapOf("part" to "snippet","chart" to "mostPopular","regionCode" to region,"videoCategoryId" to "10","maxResults" to size.toString());if(token.isNotBlank())p["pageToken"]=token;page(request(key,"videos",p),"video")}
    fun boundedHome(key:String,region:String,limit:Int):List<Entry> = Batch20.boundedPages(Batch16.homeLimit(limit),{e:Entry->e.id}){size,token->
        val p=mutableMapOf("part" to "snippet","chart" to "mostPopular","regionCode" to region,"videoCategoryId" to "10","maxResults" to size.toString());if(token.isNotBlank())p["pageToken"]=token
        val response=page(request(key,"videos",p),"video");response.entries to response.next
    }
    fun playlist(key:String,id:String,limit:Int):List<Entry>{require(Batch16.valid("playlist",id));return collect(limit.coerceIn(1,200)){size,token->val p=mutableMapOf("part" to "snippet","playlistId" to id,"maxResults" to size.toString());if(token.isNotBlank())p["pageToken"]=token;page(request(key,"playlistItems",p),"playlistItem")}}
    fun curator(key:String,channel:String,limit:Int):List<Entry>{require(Batch16.valid("channel",channel));return Batch20.boundedPages(Batch16.homeLimit(limit),{e:Entry->e.id}){size,token->val p=mutableMapOf("part" to "snippet","channelId" to channel,"maxResults" to size.toString());if(token.isNotBlank())p["pageToken"]=token;val r=page(request(key,"playlists",p),"playlist");r.entries to r.next}}
    fun details(key:String,kind:String,id:String):String {require(Batch16.valid(kind,id));val endpoint=when(kind){"video"->"videos";"channel"->"channels";else->"playlists"};val o=request(key,endpoint,mapOf("part" to ("snippet,contentDetails"+if(kind!="playlist")",statistics" else ""),"id" to id)).optJSONArray("items")?.optJSONObject(0)?:error("Public item unavailable");val e=entry(o,kind)?:error("Missing metadata");val stats=o.optJSONObject("statistics");val content=o.optJSONObject("contentDetails");return buildString{appendLine(e.title);appendLine("Channel: ${e.channel.ifBlank{"Unavailable"}}");appendLine("Published: ${e.date.ifBlank{"Unavailable"}}");if(kind=="video"){appendLine(Batch16.stats("Views",stats?.optString("viewCount")));appendLine(Batch16.stats("Likes",stats?.optString("likeCount")));appendLine("Dislikes: not public via this API");appendLine("Duration: ${content?.optString("duration").orEmpty().ifBlank{"Unavailable"}}")};if(kind=="channel"){appendLine(Batch16.stats("Subscribers",stats?.optString("subscriberCount")?.takeUnless{stats?.optBoolean("hiddenSubscriberCount")==true}));appendLine(Batch16.stats("Videos",stats?.optString("videoCount")))};if(kind=="playlist")appendLine(Batch16.stats("Items",content?.optString("itemCount")));appendLine();append(e.description)}}
    data class Comment(val id:String,val author:String,val text:String,val date:String,val likes:Long?,val replies:Int)
    data class CommentPage(val rows:List<Comment>,val next:String)
    private fun comment(o:JSONObject,replies:Int=0):Comment? {
        val id=o.optString("id");if(id.isBlank()||id.length>200)return null
        val s=o.optJSONObject("snippet")?:return null
        return Comment(id,Batch25.safeText(s.optString("authorDisplayName"),150),Batch25.safeText(s.optString("textDisplay"),4000),s.optString("publishedAt"),Batch25.count(s.optString("likeCount")),replies.coerceAtLeast(0))
    }
    fun comments(key:String,id:String,next:String="",parent:String=""):CommentPage {
        require(Batch25.videoId(id));require(parent.length<=200&&next.length<=1000)
        val args=mutableMapOf("part" to "snippet","textFormat" to "plainText","maxResults" to "30")
        if(next.isNotBlank())args["pageToken"]=next
        if(parent.isBlank()){args["videoId"]=id;args["order"]="relevance"}else args["parentId"]=parent
        val o=request(key,if(parent.isBlank())"commentThreads" else "comments",args);val a=o.optJSONArray("items")?:JSONArray()
        val rows=(0 until a.length()).mapNotNull{i->val item=a.optJSONObject(i)?:return@mapNotNull null;if(parent.isNotBlank())comment(item)else{val sn=item.optJSONObject("snippet")?:return@mapNotNull null;sn.optJSONObject("topLevelComment")?.let{comment(it,sn.optInt("totalReplyCount"))}}}
        return CommentPage(rows,o.optString("nextPageToken"))
    }
    private fun cache(c:Context)=c.getSharedPreferences("public_catalogue_snapshots_v16",Context.MODE_PRIVATE)
    fun save(c:Context,id:String,rows:List<Entry>){require(Batch16.valid("playlist",id));purge(c);val p=cache(c);val sorted=p.all.keys.sortedBy{raw->runCatching{JSONObject(p.getString(raw,"").orEmpty()).optLong("at")}.getOrDefault(0)};val edit=p.edit();sorted.filter{it!=id}.dropLast(19).forEach{edit.remove(it)};val a=JSONArray();rows.take(200).forEach{e->if(e.kind=="video"&&Batch16.valid(e.kind,e.id))a.put(JSONObject().put("id",e.id).put("title",e.title).put("channel",e.channel).put("channelId",e.channelId).put("date",e.date))};edit.putString(id,JSONObject().put("at",System.currentTimeMillis()).put("rows",a).toString()).apply()}
    fun cached(c:Context,id:String):List<Entry>? {require(Batch16.valid("playlist",id));val p=cache(c);val o=runCatching{JSONObject(p.getString(id,"").orEmpty())}.getOrNull()?:return null;if(!Batch16.fresh(o.optLong("at"),System.currentTimeMillis())){p.edit().remove(id).apply();return null};val a=o.optJSONArray("rows")?:return null;return (0 until a.length()).mapNotNull{i->val e=a.optJSONObject(i)?:return@mapNotNull null;val v=e.optString("id");if(!Batch16.valid("video",v))null else Entry("video",v,e.optString("title"),e.optString("channel"),e.optString("channelId"),e.optString("date"),"")}}
    fun purge(c:Context){val p=cache(c);val now=System.currentTimeMillis();val edit=p.edit();p.all.keys.forEach{id->val at=runCatching{JSONObject(p.getString(id,"").orEmpty()).optLong("at")}.getOrDefault(0);if(!Batch16.valid("playlist",id)||!Batch16.fresh(at,now))edit.remove(id)};edit.apply()}
    fun clear(c:Context){cache(c).edit().clear().apply()}
}
