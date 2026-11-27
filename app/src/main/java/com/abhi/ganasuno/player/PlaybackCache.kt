package com.abhi.ganasuno.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.abhi.ganasuno.data.LibraryStore
import com.abhi.ganasuno.data.Streams
import java.io.File

/** Optional transient cache of normal progressive playback; no offline download feature. */
@UnstableApi
object PlaybackCache {
    private var activeLimit:Int?=null
    fun requestedLimit()=com.abhi.ganasuno.data.Batch25.cacheMiB(LibraryStore.option("media_cache_mib",64))
    @Synchronized fun appliedLimit()=activeLimit
    private var cache:SimpleCache?=null
    @Synchronized private fun get(c:Context):SimpleCache {
        cache?.let{return it};val limit=requestedLimit()
        return SimpleCache(File(c.cacheDir,"player-media-v14"),LeastRecentlyUsedCacheEvictor(com.abhi.ganasuno.data.Batch25.cacheBytes(limit)),StandaloneDatabaseProvider(c)).also{cache=it;activeLimit=limit}
    }
    @Synchronized fun size(c:Context):Long=cache?.cacheSpace?:File(c.cacheDir,"player-media-v14").walkTopDown().filter{it.isFile}.sumOf{it.length()}
    @Synchronized fun clear(c:Context){val active=cache;if(active!=null)active.keys.toList().forEach{active.removeResource(it)}else File(c.cacheDir,"player-media-v14").deleteRecursively()}
    @Synchronized fun release(){cache?.release();cache=null;activeLimit=null}
    fun factory(c:Context,upstream:DataSource.Factory):DataSource.Factory=DataSource.Factory { object:DataSource {
        private var delegate:DataSource?=null
        private val listeners=mutableListOf<TransferListener>()
        override fun addTransferListener(listener:TransferListener){listeners.add(listener);delegate?.addTransferListener(listener)}
        override fun open(dataSpec:DataSpec):Long {
            val uri=dataSpec.uri;var spec=dataSpec;var cached=false
            if(uri.scheme==PlaybackService.SCHEME){val id=uri.lastPathSegment?:throw java.io.IOException("Bad song id");val resolved=try{Streams.resolve(id)}catch(e:Exception){throw com.abhi.ganasuno.data.StreamLookupFailure(e)};spec=dataSpec.withUri(Uri.parse(resolved.url));cached=!resolved.live&&LibraryStore.option("media_cache")==1&&spec.uri.scheme=="https"}
            if(cached)spec=spec.buildUpon().setKey(spec.uri.toString()).build()
            val source=if(cached)runCatching{CacheDataSource.Factory().setCache(get(c.applicationContext)).setUpstreamDataSourceFactory(upstream).setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR).createDataSource()}.getOrElse{upstream.createDataSource()}else upstream.createDataSource()
            delegate=source;listeners.forEach{source.addTransferListener(it)};return source.open(spec)
        }
        override fun read(buffer:ByteArray,offset:Int,length:Int):Int=delegate?.read(buffer,offset,length)?:throw java.io.IOException("Source not open")
        override fun getUri():Uri?=delegate?.uri
        override fun getResponseHeaders():Map<String,List<String>> = delegate?.responseHeaders?:emptyMap()
        override fun close(){try{delegate?.close()}finally{delegate=null}}
    }}
}
