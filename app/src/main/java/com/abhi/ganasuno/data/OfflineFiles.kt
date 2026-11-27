package com.abhi.ganasuno.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream
import java.util.UUID
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Copies only files the user selected or direct HTTPS audio files they have permission to save. */
object OfflineFiles {
    class Cancellation {
        @Volatile private var cancelled=false
        private var committed=false
        private var call:okhttp3.Call?=null
        @Synchronized fun attach(value:okhttp3.Call){call=value;if(cancelled)value.cancel()}
        @Synchronized fun cancel():Boolean{if(committed)return false;cancelled=true;call?.cancel();return true}
        fun check(){if(cancelled)throw java.io.InterruptedIOException("Download cancelled")}
        @Synchronized fun commit(action:()->Unit){check();action();committed=true;call=null}
        fun isCancelled()=cancelled
    }
    private const val LIMIT = 100L * 1024 * 1024
    private fun dir(c: Context) = File(c.filesDir,"offline-media").apply { mkdirs() }
    fun estimatedBitrate(c:Context,s:Song):Int? = runCatching {
        if(!owned(c,s))return null
        val uri=Uri.parse(s.localUri);val name=uri.lastPathSegment.orEmpty();if(!name.matches(Regex("[a-f0-9-]{36}\\.audio")))return null
        val r=android.media.MediaMetadataRetriever();try{r.setDataSource(c,uri);r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.takeIf{it>0}?.let{return it/1000};val duration=r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?:return null;Batch19.bitrate(File(dir(c),name).length(),duration)}finally{r.release()}
    }.getOrNull()
    private fun receipts(c:Context)=c.getSharedPreferences("offline_integrity_v23",Context.MODE_PRIVATE)
    private fun ownedFile(c:Context,s:Song):File? {if(!owned(c,s))return null;val name=Uri.parse(s.localUri).lastPathSegment.orEmpty();return name.takeIf{it.matches(Regex("[a-f0-9-]{36}\\.audio"))}?.let{File(dir(c),it)}}
    fun audit(c:Context,s:Song):String {
        val file=ownedFile(c,s)?:return "External file · not app-owned"
        if(!file.isFile)return "Missing file"
        val actual=file.inputStream().use{Batch23.digest(it)}
        val saved=receipts(c).getString(file.name,null)?.split(':',limit=2)
        val legacy=saved?.takeIf{it.size==2}?.let{parts->parts[0].toLongOrNull()?.let{Batch23.Digest(it,parts[1])}}
        val portable=AudioReceipts.read(c.filesDir,file.name)
        val expected=legacy?:portable?.let{Batch23.Digest(it.size,it.sha256)}
        if(portable==null&&legacy!=null&&actual==legacy)AudioReceipts.write(c.filesDir,file.name,AudioReceipts.Receipt(legacy.size,legacy.sha256))
        return "${Batch23.audit(actual,expected)} · ${actual.size} bytes\nSHA-256: ${actual.sha256}"
    }
    fun size(c: Context) = dir(c).listFiles().orEmpty().sumOf { it.length() }
    fun owned(c: Context, s: Song) = Uri.parse(s.localUri).authority == "${c.packageName}.offline"
    private fun copy(c: Context, input: InputStream, title: String, artist: String, token:Cancellation=Cancellation(),expected:Long=-1,checksum:String?=null): Song {
        val file = File(dir(c), UUID.randomUUID().toString()+".audio")
        try {
            var total = 0L
            val digest=java.security.MessageDigest.getInstance("SHA-256")
            file.outputStream().use { out -> val buf = ByteArray(65536); while (true) { token.check();val n=input.read(buf); if(n<0)break; total+=n; require(total<=LIMIT){"File exceeds 100 MB"}; digest.update(buf,0,n);out.write(buf,0,n) } }
            require(Batch22.checkedSize(total,expected,LIMIT)){"Incomplete or empty audio file"}
            val actual=digest.digest().joinToString(""){"%02x".format(it.toInt() and 255)};require(checksum==null||actual.equals(checksum,true)){"SHA-256 mismatch"}
            val uri=FileProvider.getUriForFile(c,"${c.packageName}.offline",file)
            val r=android.media.MediaMetadataRetriever()
            var taggedYear:Int?=null
            val song=try { r.setDataSource(c,uri); require(r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)=="yes"){"No audio track"}; taggedYear=Batch13.year(r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_YEAR))?:Batch13.year(r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DATE)); val ms=r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?:0L; Song("local:$uri",r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)?:title,r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)?:artist,"%d:%02d".format(ms/60000,ms/1000%60),"",false,uri.toString(),r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty()) } finally { r.release() }
            token.commit{AudioReceipts.write(c.filesDir,file.name,AudioReceipts.Receipt(total,actual));receipts(c).edit().putString(file.name,"$total:$actual").apply();LibraryStore.addLocal(song);taggedYear?.let{LibraryStore.setReleaseYear(song.id,it)}}; return song
        } catch(e:Exception){file.delete();AudioReceipts.file(c.filesDir,file.name).delete();throw e}
    }
    fun copyLocal(c: Context,s:Song,token:Cancellation=Cancellation()):Song = c.contentResolver.openInputStream(Uri.parse(s.localUri))?.use { copy(c,it,s.title,s.artist,token) } ?: error("Cannot read file")
    fun fetch(c: Context, raw: String,token:Cancellation=Cancellation(),checksum:String?=null): Song {
        require(checksum==null||checksum.matches(Regex("[A-Fa-f0-9]{64}"))){"Expected SHA-256 must be 64 hexadecimal digits"}
        val url=raw.trim().toHttpUrlOrNull() ?: error("Invalid URL")
        require(url.isHttps && url.username.isEmpty() && url.password.isEmpty()) { "HTTPS direct audio URL required" }
        val host=url.host.lowercase(); require(listOf("youtube.com","youtu.be","googlevideo.com","spotify.com").none {host==it||host.endsWith(".$it")}) {"Use an authorized direct audio file, not a streaming-service link"}
        val client=YtApi.http.newBuilder().followRedirects(false).followSslRedirects(false).build()
        val call=client.newCall(Request.Builder().url(url).build());token.attach(call);token.check()
        call.execute().use { response ->
            require(response.isSuccessful){"HTTP ${response.code}; redirects are not supported"}; val body=response.body?:error("Empty response")
            require(body.contentType()?.type=="audio"){"URL must serve an audio file"}; require(body.contentLength()<=LIMIT){"File exceeds 100 MB"}
            return body.byteStream().use {copy(c,it,url.pathSegments.lastOrNull()?.take(100)?.ifBlank{"Downloaded audio"}?:"Downloaded audio","Offline audio",token,body.contentLength(),checksum)}
        }
    }
    fun remove(c:Context,s:Song) { if(owned(c,s)){ val name=Uri.parse(s.localUri).lastPathSegment.orEmpty(); if(name.matches(Regex("[a-f0-9-]{36}\\.audio"))) { File(dir(c),name).delete(); AudioReceipts.file(c.filesDir,name).delete(); receipts(c).edit().remove(name).apply() } };LibraryStore.removeLocal(s) }
}
