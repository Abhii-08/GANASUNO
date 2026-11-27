package com.abhi.ganasuno.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import java.io.File
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

/** Bounded thumbnail cache; media files are stored separately and never touched here. */
object ImageLoader {
    private val cache=object:LruCache<String,Bitmap>((Runtime.getRuntime().maxMemory()/1024/8).toInt()){
        override fun sizeOf(key:String,value:Bitmap):Int=value.byteCount/1024
    }
    private val pool=Executors.newFixedThreadPool(3)
    private val main=Handler(Looper.getMainLooper())
    private val lock=Any()
    private const val MAX_DISK=32L*1024*1024
    @Volatile private var generation=0L
    private fun dir(c:Context)=File(c.cacheDir,"thumbs-v13").apply{mkdirs()}
    private fun key(url:String)=MessageDigest.getInstance("SHA-256").digest(url.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it.toInt() and 255)}
    fun clear(){synchronized(lock){generation++;cache.evictAll()}}
    fun diskBytes(c:Context):Long=synchronized(lock){dir(c).listFiles().orEmpty().sumOf{it.length()}}
    fun memoryBytes():Long=cache.size().toLong()*1024
    fun clearDisk(c:Context){synchronized(lock){generation++;dir(c).listFiles().orEmpty().forEach{it.delete()}}}
    fun clearAll(c:Context){synchronized(lock){clear();clearDisk(c)}}
    private fun trim(d:File){val files=d.listFiles().orEmpty().sortedBy{it.lastModified()};var total=files.sumOf{it.length()};for(f in files){if(total<=MAX_DISK)break;val n=f.length();if(f.delete())total-=n}}
    private fun read(c:Context,url:String):Bitmap?=synchronized(lock){val f=File(dir(c),key(url));if(!f.isFile)return@synchronized null;BitmapFactory.decodeFile(f.path)?.also{f.setLastModified(System.currentTimeMillis())}?:run{f.delete();null}}
    private fun download(url:String):Bitmap? {
        val connection=URL(url).openConnection() as? javax.net.ssl.HttpsURLConnection?:return null
        try{connection.connectTimeout=10000;connection.readTimeout=10000;connection.instanceFollowRedirects=false;if(connection.responseCode!=200)return null
            val bytes=connection.inputStream.use{input->val out=java.io.ByteArrayOutputStream();val buf=ByteArray(8192);while(true){val n=input.read(buf);if(n<0)break;if(out.size()+n>4*1024*1024)return null;out.write(buf,0,n)};out.toByteArray()}
            val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds);if(bounds.outWidth<=0||bounds.outHeight<=0)return null
            var sample=1;while(maxOf(bounds.outWidth,bounds.outHeight)/sample>1000)sample*=2
            return BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply{inSampleSize=sample})
        }finally{connection.disconnect()}
    }
    fun load(url:String,view:ImageView,placeholder:Int,onLoaded:((Bitmap)->Unit)?=null){
        view.tag=url;if(!url.startsWith("https://")){view.setImageResource(placeholder);return};cache.get(url)?.let{view.setImageBitmap(it);onLoaded?.invoke(it);return};view.setImageResource(placeholder)
        val c=view.context.applicationContext;val token=generation
        pool.execute{val bitmap=runCatching{read(c,url)?:download(url)}.getOrNull()?:return@execute
            synchronized(lock){if(token==generation){cache.put(url,bitmap);runCatching{val d=dir(c);val f=File(d,key(url));if(!f.exists()){val tmp=File.createTempFile("thumb-",".tmp",d);try{tmp.outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.JPEG,85,it))};check(tmp.renameTo(f))}finally{tmp.delete()}};trim(d)}}}
            main.post{if(view.tag==url&&token==generation){view.setImageBitmap(bitmap);onLoaded?.invoke(bitmap)}}
        }
    }
}
