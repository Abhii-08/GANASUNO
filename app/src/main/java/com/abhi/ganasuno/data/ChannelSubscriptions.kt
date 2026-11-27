package com.abhi.ganasuno.data

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** Official YouTube Data API: explicit, foreground changes using a session-only OAuth access token.
 * No cookie import, refresh token, logging, persistent storage or redirects. */
object ChannelSubscriptions {
    private val client=YtApi.http.newBuilder().followRedirects(false).followSslRedirects(false).build()
    data class State(val subscriptionId:String?,val title:String)
    private fun request(token:String,method:String,params:Map<String,String>,json:JSONObject?=null):JSONObject {
        require(Batch22.token(token));val url="https://www.googleapis.com/youtube/v3/subscriptions".toHttpUrl().newBuilder();params.forEach{(k,v)->url.addQueryParameter(k,v)}
        val b=Request.Builder().url(url.build()).header("Authorization","Bearer $token").header("Accept","application/json")
        when(method){"GET"->b.get();"POST"->b.post(requireNotNull(json).toString().toRequestBody("application/json".toMediaType()));"DELETE"->b.delete();else->error("Invalid method")}
        return client.newCall(b.build()).execute().use{r->check(r.isSuccessful){"YouTube subscription request failed (HTTP ${r.code})"};if(r.code==204)return@use JSONObject();val source=(r.body?:error("Empty response")).source();require(!source.request(512*1024L+1)){"Response too large"};JSONObject(source.readUtf8())}
    }
    fun state(token:String,channel:String):State {
        require(Batch16.valid("channel",channel));val o=request(token,"GET",mapOf("part" to "snippet","mine" to "true","forChannelId" to channel,"maxResults" to "50"));val a=o.optJSONArray("items")?:error("Invalid subscription response")
        for(i in 0 until a.length()){val row=a.optJSONObject(i)?:continue;val s=row.optJSONObject("snippet")?:continue;if(s.optJSONObject("resourceId")?.optString("channelId")!=channel)continue;val id=row.optString("id");require(id.matches(Regex("[A-Za-z0-9_-]{1,200}")));return State(id,Batch16.clean(s.optString("title"),200))}
        return State(null,"")
    }
    fun set(token:String,channel:String,follow:Boolean):State {
        val previous=state(token,channel)
        if(follow&&previous.subscriptionId==null){val data=JSONObject().put("snippet",JSONObject().put("resourceId",JSONObject().put("kind","youtube#channel").put("channelId",channel)));request(token,"POST",mapOf("part" to "snippet"),data)}
        if(!follow&&previous.subscriptionId!=null)request(token,"DELETE",mapOf("id" to previous.subscriptionId))
        return state(token,channel).also{check((it.subscriptionId!=null)==follow){"Subscription change not confirmed; check again"}}
    }
}
