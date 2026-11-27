package com.abhi.ganasuno.ui

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import com.abhi.ganasuno.data.Batch20
import java.io.File
import okhttp3.Request

/** User-selected local animated GIF or WebP; no service or account required. */
object AnimatedCover {
    private fun directory(c:Context)=File(c.filesDir,"animated-covers").apply{mkdirs()}
    private fun file(c:Context,id:String)=File(directory(c),Batch20.artworkName(id))
    private fun remote(c:Context)=File(c.cacheDir,"animated-art-v22").apply{mkdirs()}
    private fun remoteFile(c:Context,id:String)=File(remote(c),Batch20.artworkName(id))
    private fun remoteEntries(c:Context)=remote(c).listFiles().orEmpty().filter{it.isFile&&it.name.matches(Regex("[a-f0-9]{64}\\.gif"))}
    @Synchronized fun remoteStats(c:Context):Pair<Int,Long>{val files=remoteEntries(c);return files.size to files.sumOf{it.length()}}
    @Synchronized fun clearRemote(c:Context)=remoteEntries(c).count{it.delete()}
    @Synchronized fun remove(c:Context,id:String){file(c,id).delete();remoteFile(c,id).delete()}
    fun fetch(c:Context,id:String,raw:String){
        if (Build.VERSION.SDK_INT < 28) error("Android 9+ required")
        val url=com.abhi.ganasuno.data.Batch22.httpsFile(raw)?:error("Valid HTTPS URL required")
        val tmp=File.createTempFile("art-",".tmp",remote(c));try{
            val http=com.abhi.ganasuno.data.YtApi.http.newBuilder().followRedirects(false).followSslRedirects(false).callTimeout(40,java.util.concurrent.TimeUnit.SECONDS).build()
            http.newCall(Request.Builder().url(url).build()).execute().use{r->check(r.isSuccessful){"Image request failed"};val b=r.body?:error("Empty image");require(b.contentType()?.let{it.type=="image"&&it.subtype in setOf("gif","webp")}==true){"GIF or WebP required"};require(b.contentLength()<=Batch20.MAX_ART_BYTES);var total=0L;b.byteStream().use{input->tmp.outputStream().use{out->val buffer=ByteArray(8192);while(true){val n=input.read(buffer);if(n<0)break;total+=n;require(total<=Batch20.MAX_ART_BYTES);out.write(buffer,0,n)}}};require(com.abhi.ganasuno.data.Batch22.checkedSize(total,b.contentLength(),Batch20.MAX_ART_BYTES))}
            val header=tmp.inputStream().use{ByteArray(12).also{bytes->java.io.DataInputStream(it).readFully(bytes)}};require(com.abhi.ganasuno.data.Batch21.animationContainer(header));require(decode(tmp) is AnimatedImageDrawable){"Image is not animated"}
            synchronized(this){val target=remoteFile(c,id);val files=remoteEntries(c).filter{it!=target}.sortedBy{it.lastModified()};var used=files.sumOf{it.length()};for(old in files){if(used+tmp.length()<=32L*1024*1024)break;val size=old.length();if(old.delete())used-=size};require(used+tmp.length()<=32L*1024*1024);java.nio.file.Files.move(tmp.toPath(),target.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);target.setLastModified(System.currentTimeMillis())}
        }finally{tmp.delete()}
    }
    @Synchronized fun storedBytes(c:Context):Long=directory(c).listFiles().orEmpty().filter{it.name.matches(Regex("[a-f0-9]{64}\\.gif"))&&it.isFile}.sumOf{it.length()}
    @Synchronized fun count(c:Context):Int=directory(c).listFiles().orEmpty().count{it.name.matches(Regex("[a-f0-9]{64}\\.gif"))&&it.isFile}
    @Synchronized fun clearImported(c:Context):Int=directory(c).listFiles().orEmpty().count{it.name.matches(Regex("[a-f0-9]{64}\\.gif"))&&it.isFile&&it.delete()}
    @Synchronized fun save(c:Context,id:String,uri:Uri){
        if (Build.VERSION.SDK_INT < 28) error("Animated image decoding requires Android 9 or newer")
        val dir=directory(c); val tmp=File.createTempFile("cover-",".tmp",dir)
        try {
            c.contentResolver.openInputStream(uri)?.use{input->tmp.outputStream().use{out->val buf=ByteArray(8192);var total=0L;while(true){val n=input.read(buf);if(n<0)break;total+=n;require(total<=Batch20.MAX_ART_BYTES){"Image exceeds 8 MB"};out.write(buf,0,n)}}}?:error("Cannot read image")
            val header=tmp.inputStream().use{ByteArray(12).also{b->java.io.DataInputStream(it).readFully(b)}}
            require(com.abhi.ganasuno.data.Batch21.animationContainer(header)){"Choose GIF or WebP"}
            val drawable=decode(tmp);require(drawable is AnimatedImageDrawable){"Image is not animated"}
            val used=dir.listFiles().orEmpty().filter{it!=tmp&&it!=file(c,id)}.sumOf{it.length()};require(used+tmp.length()<=64L*1024*1024){"Animated cover storage limit reached; remove old covers"}
            java.nio.file.Files.move(tmp.toPath(),file(c,id).toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }finally{tmp.delete()}
    }
    @androidx.annotation.RequiresApi(28)
    private fun decode(f:File)=ImageDecoder.decodeDrawable(ImageDecoder.createSource(f)){decoder,info,_->
        require(info.size.width in 1..2048 && info.size.height in 1..2048){"Maximum image dimensions: 2048 × 2048"}
        val scale=maxOf(1,(maxOf(info.size.width,info.size.height)+511)/512);decoder.setTargetSampleSize(scale)
    }
    fun show(view:ImageView,id:String):Boolean {
        if(Build.VERSION.SDK_INT<28)return false
        val f=file(view.context,id).takeIf{it.isFile}?:remoteFile(view.context,id);if(!f.isFile)return false;if(f.parentFile==remote(view.context))f.setLastModified(System.currentTimeMillis())
        val d=runCatching{decode(f) as? AnimatedImageDrawable}.getOrNull()?:return false
        (view.drawable as? Animatable)?.stop();view.tag="animated:$id";view.rotation=0f;view.setImageDrawable(d);d.repeatCount=AnimatedImageDrawable.REPEAT_INFINITE;d.start();return true
    }
}
