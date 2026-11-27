package com.abhi.ganasuno.data

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Read-only Last.fm artist.getSimilar. Artist and user-supplied API key only. */
object SimilarArtists {
    private val http=YtApi.http.newBuilder().followRedirects(false).followSslRedirects(false).callTimeout(15,TimeUnit.SECONDS).build()
    fun fetch(artist:String,key:String):List<Batch20.SimilarArtist> {
        require(artist.isNotBlank()&&artist.length<=200&&key.matches(Regex("[a-fA-F0-9]{32}")))
        val url="https://ws.audioscrobbler.com/2.0/".toHttpUrl().newBuilder().addQueryParameter("method","artist.getsimilar").addQueryParameter("artist",artist).addQueryParameter("api_key",key).addQueryParameter("limit","30").addQueryParameter("autocorrect","1").addQueryParameter("format","json").build()
        return http.newCall(Request.Builder().url(url).build()).execute().use{r->
            check(r.isSuccessful){"Last.fm unavailable"};val source=(r.body?:error("Empty result")).source();require(!source.request(512*1024L+1));val o=JSONObject(source.readUtf8());check(!o.has("error")){"Last.fm rejected the request"}
            val similar=o.optJSONObject("similarartists")?:return@use emptyList();val array=similar.optJSONArray("artist")?:return@use emptyList();val rows=(0 until array.length()).mapNotNull{i->val v=array.optJSONObject(i)?:return@mapNotNull null;val score=v.optString("match").toDoubleOrNull()?:return@mapNotNull null;Batch20.SimilarArtist(v.optString("name").trim(),score)}
            Batch20.similarArtists(similar.optString("artist",artist),rows)
        }
    }
}
