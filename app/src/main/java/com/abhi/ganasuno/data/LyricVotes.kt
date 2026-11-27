package com.abhi.ganasuno.data

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

object LyricVotes {
    private val http=YtApi.http.newBuilder().followRedirects(false).followSslRedirects(false).callTimeout(12,TimeUnit.SECONDS).build()
    data class Result(val up:Int,val down:Int,val mine:Int)
    @Synchronized private fun token(c:Context,origin:String):String {
        val p=c.getSharedPreferences("lyric_vote_tokens",Context.MODE_PRIVATE);val key=Batch20.digest(origin)
        return p.getString(key,null)?:ByteArray(32).also{SecureRandom().nextBytes(it)}.joinToString(""){"%02x".format(it.toInt() and 255)}.also{p.edit().putString(key,it).commit()}
    }
    fun request(c:Context,server:String,id:String,lyrics:String,vote:Int?):Result {
        val origin=Batch20.origin(server)?:error("Enter trusted HTTPS origin only")
        val key=Batch20.lyricKey(id,lyrics)?:error("Community votes require a public track ID and saved lyrics")
        require(vote==null || vote in -1..1)
        val body=JSONObject().put("lyric",key).put("voter",token(c,origin));if(vote!=null)body.put("value",vote)
        val r=Request.Builder().url("$origin/v1/lyrics/vote").post(body.toString().toRequestBody("application/json".toMediaType())).build()
        return http.newCall(r).execute().use{response->check(response.isSuccessful);val source=(response.body?:error("Empty response")).source();require(!source.request(8193));val o=JSONObject(source.readUtf8());Result(o.getInt("up"),o.getInt("down"),o.getInt("mine"))}
    }
}
