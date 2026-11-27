package com.abhi.ganasuno.data

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/** Public community estimates and segment data; no credentials or redirects. */
object CommunityVideo {
    private val client=YtApi.http.newBuilder().followRedirects(false).followSslRedirects(false).build()
    private fun get(request:Request,empty404:Boolean=false):String?=client.newCall(request).execute().use{r->
        if(empty404&&r.code==404)return@use null
        check(r.isSuccessful){"Community service HTTP ${r.code}"}
        val source=(r.body?:error("Empty response")).source()
        require(!source.request(1024*1024L+1)){"Community response too large"}
        source.readUtf8()
    }
    data class Votes(val likes:Long?,val dislikes:Long?,val views:Long?)
    fun votes(id:String):Votes {
        require(Batch25.videoId(id))
        val url="https://returnyoutubedislikeapi.com/votes".toHttpUrl().newBuilder().addQueryParameter("videoId",id).build()
        val o=JSONObject(get(Request.Builder().url(url).build())?:error("No data"))
        require(o.optString("id")==id&&!o.optBoolean("deleted")){"No matching video data"}
        return Votes(Batch25.count(o.optString("likes")),Batch25.count(o.optString("dislikes")),Batch25.count(o.optString("viewCount")))
    }
    fun segments(id:String,mask:Int):List<Batch25.Segment> {
        require(Batch25.videoId(id));val categories=Batch25.selected(mask);if(categories.isEmpty())return emptyList()
        val url="https://sponsor.ajay.app/api/skipSegments".toHttpUrl().newBuilder().addQueryParameter("videoID",id)
            .addQueryParameter("categories",JSONArray(categories).toString()).addQueryParameter("actionTypes",JSONArray(listOf("skip","poi")).toString()).build()
        val raw=get(Request.Builder().url(url).build(),true)?:return emptyList()
        val a=JSONArray(raw);return (0 until minOf(a.length(),500)).mapNotNull{i->
            val o=a.optJSONObject(i)?:return@mapNotNull null;val s=o.optJSONArray("segment")?:return@mapNotNull null
            val category=o.optString("category");if(category !in categories)return@mapNotNull null
            Batch25.segment(s.optDouble(0),s.optDouble(1),category,o.optString("actionType"))
        }.distinct().sortedBy{it.start}
    }
}
