package com.abhi.ganasuno.data

import android.content.Context
import android.media.*
import android.net.Uri
import android.os.SystemClock
import java.nio.ByteOrder

/** Decode local audio or explicitly requested seekable online audio. Cache only numeric online summaries, not audio bytes or URLs. */
object LocalTrackAnalyzer {
    data class Track(val duration:Long,val mime:String,val rate:Int,val channels:Int,val bitrate:Int?,val analysis:AudioAnalysis.Result)
    private data class Cached(val track:Track,val at:Long)
    private val cache=object:LinkedHashMap<String,Cached>(16,.75f,true){override fun removeEldestEntry(eldest:MutableMap.MutableEntry<String,Cached>?)=size>12}
    private const val TTL=30*60*1000L
    private fun cacheKey(s:Song)="${s.id}|${Prefs.audioQuality}|${Prefs.audioLang}"
    @Synchronized fun cached(s:Song):Track? {if(s.localUri.isNotBlank()||s.live)return null;val entry=cache[cacheKey(s)]?:return null;if(SystemClock.elapsedRealtime()-entry.at>=TTL){cache.remove(cacheKey(s));return null};return entry.track}
    @Synchronized fun clearSummaries(){cache.clear()}
    fun analyze(c:Context,s:Song,allowOnline:Boolean=false):Track {
        require(s.localUri.startsWith("content://")||(allowOnline&&s.localUri.isBlank()&&!s.live&&s.id.matches(Regex("[A-Za-z0-9_-]{11}")))){"Select seekable audio"}
        if(allowOnline)cached(s)?.let{return it}
        val ext=MediaExtractor();var codec:MediaCodec?=null;var started=false
        try{
            if(s.localUri.isNotBlank())ext.setDataSource(c,Uri.parse(s.localUri),null)else {val source=Streams.resolve(s.id);require(!source.live&&source.url.startsWith("https://")){"Stream is not seekable HTTPS audio"};ext.setDataSource(source.url,mapOf("User-Agent" to YtApi.WEB_UA))}
            val index=(0 until ext.trackCount).firstOrNull{ext.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/")==true}?:error("No audio track")
            val fmt=ext.getTrackFormat(index);val mime=fmt.getString(MediaFormat.KEY_MIME)?:error("Unknown codec");val duration=fmt.getLong(MediaFormat.KEY_DURATION)/1000
            require(duration in 1..900000){"Analysis supports tracks up to 15 minutes"};val bitrate=if(fmt.containsKey(MediaFormat.KEY_BIT_RATE))fmt.getInteger(MediaFormat.KEY_BIT_RATE)else null
            ext.selectTrack(index);val decoder=MediaCodec.createDecoderByType(mime);codec=decoder;decoder.configure(fmt,null,null,0);decoder.start();started=true
            val info=MediaCodec.BufferInfo();var rate=fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE);var channels=fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT);var encoding=AudioFormat.ENCODING_PCM_16BIT
            var core:AudioAnalysis?=null;var fed=false;var done=false;val deadline=SystemClock.elapsedRealtime()+120000
            while(!done){check(!Thread.currentThread().isInterrupted&&SystemClock.elapsedRealtime()<deadline){"Analysis timed out or cancelled"}
                if(!fed){val i=decoder.dequeueInputBuffer(1000);if(i>=0){val b=decoder.getInputBuffer(i)?:error("Missing decoder input");b.clear();val n=ext.readSampleData(b,0);if(n<0){decoder.queueInputBuffer(i,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);fed=true}else{decoder.queueInputBuffer(i,0,n,ext.sampleTime,0);ext.advance()}}}
                val o=decoder.dequeueOutputBuffer(info,1000)
                if(o==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){val f=decoder.outputFormat;val r=f.getInteger(MediaFormat.KEY_SAMPLE_RATE);val ch=f.getInteger(MediaFormat.KEY_CHANNEL_COUNT);require(core==null||(r==rate&&ch==channels)){"Changing audio format unsupported"};rate=r;channels=ch;encoding=if(f.containsKey(MediaFormat.KEY_PCM_ENCODING))f.getInteger(MediaFormat.KEY_PCM_ENCODING)else AudioFormat.ENCODING_PCM_16BIT}
                else if(o>=0){try{if(info.size>0&&(info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0){require(channels in 1..8&&encoding in listOf(AudioFormat.ENCODING_PCM_16BIT,AudioFormat.ENCODING_PCM_FLOAT)){"Unsupported decoded PCM format"};val b=decoder.getOutputBuffer(o)?:error("Missing decoder output");b.order(ByteOrder.nativeOrder());b.position(info.offset);b.limit(info.offset+info.size);val a=core?:AudioAnalysis(rate,duration).also{core=it};val bytes=if(encoding==AudioFormat.ENCODING_PCM_FLOAT)4 else 2;while(b.remaining()>=channels*bytes){var sum=0.0;repeat(channels){sum+=if(bytes==4)b.float.toDouble()else b.short/32768.0};a.sample(sum/channels)}};done=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0}finally{decoder.releaseOutputBuffer(o,false)}}
            }
            val result=core?.result()?:error("No decoded audio");return Track(duration,mime,rate,channels,bitrate,result).also{if(s.localUri.isBlank())synchronized(this){cache[cacheKey(s)]=Cached(it,SystemClock.elapsedRealtime())}}
        }finally{if(started)runCatching{codec?.stop()};runCatching{codec?.release()};ext.release()}
    }
}
