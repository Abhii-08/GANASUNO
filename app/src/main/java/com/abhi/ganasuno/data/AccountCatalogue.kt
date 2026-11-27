package com.abhi.ganasuno.data

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray

/** Official Google endpoints only. Short-lived token is memory-only; mutations never auto-retry. */
object AccountCatalogue {
    private val client=YtApi.http.newBuilder().followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build()
    private fun request(token:String,path:String,p:Map<String,String>,post:JSONObject?=null,emptyPost:Boolean=false):JSONObject {
        require(Batch22.token(token));require(path in setOf("playlists","playlistItems","channels","videos","videos/rate","videos/getRating","comments","commentThreads"))
        val u=("https://www.googleapis.com/youtube/v3/"+path).toHttpUrl().newBuilder();p.forEach{(k,v)->u.addQueryParameter(k,v)}
        val b=Request.Builder().url(u.build()).header("Authorization","Bearer $token").header("Accept","application/json")
        if(post!=null||emptyPost)b.post((post?.toString()?:"").toRequestBody("application/json".toMediaType()))
        return client.newCall(b.build()).execute().use{r->check(r.isSuccessful){"Google API HTTP ${r.code}; check token, permission and quota"};if(r.code==204)return@use JSONObject();val source=(r.body?:error("Missing response")).source();require(!source.request(2097153L)){"Response too large"};JSONObject(source.readUtf8())}
    }
    private fun rows(o:JSONObject):List<JSONObject>{val a=o.optJSONArray("items")?:JSONArray();return (0 until a.length()).mapNotNull{a.optJSONObject(it)}}
    fun create(token:String,name:String):String {
        require(name.isNotBlank()&&name.length<=100)
        val body=JSONObject().put("snippet",JSONObject().put("title",name)).put("status",JSONObject().put("privacyStatus","private"))
        return request(token,"playlists",mapOf("part" to "snippet,status"),body).getString("id").also{require(Batch16.valid("playlist",it))}
    }
    private fun verifyOwner(token:String,id:String){require(Batch16.valid("playlist",id));val owners=rows(request(token,"channels",mapOf("part" to "id","mine" to "true","maxResults" to "50"))).map{it.optString("id")}.toSet();val owner=rows(request(token,"playlists",mapOf("part" to "snippet","id" to id))).firstOrNull{it.optString("id")==id}?.optJSONObject("snippet")?.optString("channelId");require(owner!=null&&owner in owners){"Playlist does not belong to the authorized channel"}}
    private fun existing(token:String,id:String):Set<String>{val result=mutableSetOf<String>();var cursor="";val seen=mutableSetOf<String>();repeat(20){val p=mutableMapOf("part" to "contentDetails","playlistId" to id,"maxResults" to "50");if(cursor.isNotEmpty())p["pageToken"]=cursor;val o=request(token,"playlistItems",p);rows(o).forEach{it.optJSONObject("contentDetails")?.optString("videoId")?.let(result::add)};cursor=o.optString("nextPageToken");if(cursor.isBlank())return result;require(seen.add(cursor)){"Repeated playlist page"}};error("Remote playlist exceeds 1000 entries; no changes made")}
    data class SyncResult(val added:Int,val already:Int,val pending:Int,val error:String?)
    fun append(token:String,id:String,ids:List<String>):SyncResult {
        require(ids.size in 1..100&&ids.all{Batch16.valid("video",it)});verifyOwner(token,id);val unique=ids.distinct();val pending=Batch26.missing(unique,existing(token,id));var done=0
        for(video in pending){val payload=JSONObject().put("snippet",JSONObject().put("playlistId",id).put("resourceId",JSONObject().put("kind","youtube#video").put("videoId",video)));try{request(token,"playlistItems",mapOf("part" to "snippet"),payload);done++}catch(e:Exception){return SyncResult(done,unique.size-pending.size,pending.size-done,"Stopped after a failed/uncertain write. Recheck remote playlist before retrying.")}}
        return SyncResult(done,unique.size-pending.size,0,null)
    }
    fun rating(token:String,id:String):String{require(Batch16.valid("video",id));val r=rows(request(token,"videos/getRating",mapOf("id" to id))).firstOrNull{it.optString("videoId")==id}?.optString("rating");return r?.takeIf(Batch26::rating)?:error("Rating unavailable")}
    fun rate(token:String,id:String,value:String):String{require(Batch16.valid("video",id)&&Batch26.rating(value));request(token,"videos/rate",mapOf("id" to id,"rating" to value),emptyPost=true);return rating(token,id).also{check(it==value){"Rating not confirmed"}}}
    fun liked(token:String):List<Song>{return Batch16.collect(100,{s:Song->s.id}){size,cursor->val p=mutableMapOf("part" to "snippet","myRating" to "like","maxResults" to size.toString());if(cursor.isNotBlank())p["pageToken"]=cursor;val o=request(token,"videos",p);rows(o).mapNotNull{r->val id=r.optString("id");val s=r.optJSONObject("snippet");if(!Batch16.valid("video",id)||s==null)null else Song(id,Batch16.clean(s.optString("title"),300),Batch16.clean(s.optString("channelTitle"),200),"","https://i.ytimg.com/vi/$id/hqdefault.jpg")}.filter{it.title.isNotBlank()} to o.optString("nextPageToken")}}
    fun postComment(token:String,video:String,text:String,parent:String=""):String {
        require(Batch16.valid("video",video));val body=Batch27.comment(text)?:error("Invalid comment text")
        val result=if(parent.isBlank()){
            val snippet=JSONObject().put("videoId",video).put("topLevelComment",JSONObject().put("snippet",JSONObject().put("textOriginal",body)))
            request(token,"commentThreads",mapOf("part" to "snippet"),JSONObject().put("snippet",snippet))
        }else{
            require(Batch27.commentId(parent))
            val source=rows(request(token,"comments",mapOf("part" to "snippet","id" to parent))).firstOrNull{it.optString("id")==parent}?.optJSONObject("snippet")?:error("Parent comment unavailable")
            require(source.optString("videoId")==video&&source.optString("parentId").isBlank()){"Parent must be a top-level comment on this video"}
            request(token,"comments",mapOf("part" to "snippet"),JSONObject().put("snippet",JSONObject().put("parentId",parent).put("textOriginal",body)))
        }
        return result.getString("id").also{require(Batch27.commentId(it))}
    }
    fun pull(token:String,id:String):List<Song>{
        verifyOwner(token,id);val out=linkedMapOf<String,Song>();var cursor="";val seen=mutableSetOf<String>()
        repeat(20){
            val p=mutableMapOf("part" to "snippet","playlistId" to id,"maxResults" to "50");if(cursor.isNotBlank())p["pageToken"]=cursor
            val o=request(token,"playlistItems",p)
            rows(o).forEach{r->val sn=r.optJSONObject("snippet")?:return@forEach;val v=sn.optJSONObject("resourceId")?.optString("videoId").orEmpty();val title=Batch16.clean(sn.optString("title"));if(Batch16.valid("video",v)&&title.isNotBlank()&&title !in setOf("Private video","Deleted video")){out.putIfAbsent(v,Song(v,title,Batch16.clean(sn.optString("videoOwnerChannelTitle",sn.optString("channelTitle")),200),"","https://i.ytimg.com/vi/$v/hqdefault.jpg"))}}
            cursor=o.optString("nextPageToken");if(cursor.isBlank())return out.values.toList();require(seen.add(cursor)){"Repeated playlist page"}
        };error("Remote playlist exceeds 1000 entries; no local changes made")
    }
}
