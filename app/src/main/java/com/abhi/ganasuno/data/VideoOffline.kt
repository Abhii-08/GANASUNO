package com.abhi.ganasuno.data

import android.content.Context
import android.media.MediaMetadataRetriever
import java.io.File
import okhttp3.Request

/** Authorized direct MP4 files only; no platform stream extraction or DRM removal. */
object VideoOffline {
    private const val LIMIT=200L*1024*1024
    data class Download(val file:File,val height:Int,val bytes:Long)
    fun fetch(c:Context,raw:String,token:OfflineFiles.Cancellation):Download {
        val url=Batch28.direct(raw)?:error("Authorized direct HTTPS MP4 required")
        val file=File.createTempFile("ganasuno-video-",".mp4",c.cacheDir)
        try {
            val client=YtApi.http.newBuilder().followRedirects(false).followSslRedirects(false).build()
            val call=client.newCall(Request.Builder().url(url).build());token.attach(call);token.check()
            val count=call.execute().use {response->
                check(response.isSuccessful){"HTTP ${response.code}"};val body=response.body?:error("No video body")
                require(body.contentType()?.toString()?.substringBefore(';')?.trim()?.lowercase()=="video/mp4"){"Direct MP4 video required"};require(body.contentLength()<=LIMIT){"Video exceeds 200 MB"}
                var bytes=0L;body.byteStream().use{input->file.outputStream().use{out->val buffer=ByteArray(65536);while(true){token.check();val n=input.read(buffer);if(n<0)break;bytes+=n;require(bytes<=LIMIT){"Video exceeds 200 MB"};out.write(buffer,0,n)}}};require(Batch22.checkedSize(bytes,body.contentLength(),LIMIT)){"Incomplete video"};bytes
            }
            token.check();val r=MediaMetadataRetriever();val height=try{r.setDataSource(file.path);require(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)=="yes"){"No video track"};r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()?.takeIf{it>0}?:error("Video dimensions unavailable")}finally{r.release()}
            token.commit{};return Download(file,height,count)
        }catch(e:Exception){file.delete();throw e}
    }
    fun purge(c:Context){val cutoff=System.currentTimeMillis()-86400000L;c.cacheDir.listFiles().orEmpty().filter{it.name.startsWith("ganasuno-video-")&&it.name.endsWith(".mp4")&&it.lastModified()<cutoff}.forEach{it.delete()}}
}
