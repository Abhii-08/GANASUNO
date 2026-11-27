package com.abhi.ganasuno.data

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Explicitly shared, expiring snapshots, not a cloud-account or local audio upload. */
object PlaylistLinks {
    private val client=YtApi.http.newBuilder().followRedirects(false).followSslRedirects(false).callTimeout(15,TimeUnit.SECONDS).build()
    data class Shared(val link:String,val code:String,val revoke:String)
    data class Snapshot(val name:String,val ids:List<String>)
    private fun url(raw:String):HttpUrl {val u=raw.trim().toHttpUrlOrNull()?:error("Invalid URL");require(u.isHttps&&u.username.isEmpty()&&u.password.isEmpty()&&u.query==null&&u.fragment==null){"Trusted HTTPS URL required"};return u}
    private fun base(raw:String)=url(raw).also{require(it.encodedPath=="/"){"Enter server origin only, without a path"}}
    private fun request(r:Request):JSONObject=client.newCall(r).execute().use{response->check(response.isSuccessful){"Playlist server HTTP ${response.code}"};val source=(response.body?:error("Empty response")).source();require(!source.request(65537)){"Response too large"};JSONObject(source.readUtf8())}
    private fun post(server:HttpUrl,action:String,body:JSONObject):JSONObject=request(Request.Builder().url(server.newBuilder().addPathSegments("v1/playlists/$action").build()).post(body.toString().toRequestBody("application/json".toMediaType())).build())
    fun create(server:String,name:String,ids:List<String>):Shared{val origin=base(server);val title=Batch15.cleanTitle(name);require(title.isNotBlank());val r=post(origin,"create",JSONObject().put("name",title).put("tracks",JSONArray(Batch15.shareIds(ids))));val code=r.getString("code");val token=r.getString("revoke");require(Batch15.validCode(code)&&token.matches(Regex("[A-Za-z0-9_-]{43}")));return Shared(origin.newBuilder().addPathSegments("p/$code").build().toString(),code,token)}
    fun read(raw:String):Snapshot{val link=url(raw);val segments=link.pathSegments;require(segments.size==2&&segments[0]=="p"&&Batch15.validCode(segments[1])){"Invalid GANASUNO playlist link"};val o=request(Request.Builder().url(link.newBuilder().addPathSegment("json").build()).build());val a=o.getJSONArray("tracks");require(a.length() in 1..100);val title=Batch15.cleanTitle(o.getString("name"));require(title.isNotBlank());return Snapshot(title,Batch15.shareIds((0 until a.length()).map{a.getString(it)}))}
    fun revoke(raw:String,token:String){val link=url(raw);val segments=link.pathSegments;require(segments.size==2&&segments[0]=="p"&&Batch15.validCode(segments[1])&&token.matches(Regex("[A-Za-z0-9_-]{43}")));val origin=link.newBuilder().encodedPath("/").build();check(post(origin,"delete",JSONObject().put("code",segments[1]).put("revoke",token)).optBoolean("ok"))}
}
