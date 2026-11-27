package com.abhi.ganasuno.player

import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import com.abhi.ganasuno.data.Batch25
import com.abhi.ganasuno.data.CommunityVideo
import com.abhi.ganasuno.data.LibraryStore
import java.util.concurrent.Executors

/** One worker per player; all Player operations on main looper. Opt-in, fail-open. */
class CommunitySkipper(private val player:()->Player?,private val id:()->String?) {
    private val handler=Handler(Looper.getMainLooper())
    private val io=Executors.newSingleThreadExecutor()
    @Volatile private var generation=0L
    @Volatile private var active=false
    private var key:Pair<String,Int>?=null
    private var segments=emptyList<Batch25.Segment>()
    private var lastSeek=0L
    private val tick=object:Runnable { override fun run(){
        if(!active)return
        val p=player();val video=id();val mask=LibraryStore.option("sponsor_categories",1) and Batch25.ALL
        val wanted=if(LibraryStore.option("sponsor_enabled")==1&&video!=null&&Batch25.videoId(video)&&mask!=0&&p!=null&&!p.isCurrentMediaItemLive)video to mask else null
        if(wanted!=key){key=wanted;segments=emptyList();lastSeek=0L;val serial=++generation
            if(wanted!=null)io.execute{if(!active||generation!=serial)return@execute;val result=runCatching{CommunityVideo.segments(wanted.first,wanted.second)}.getOrDefault(emptyList());handler.post{if(active&&generation==serial&&key==wanted)segments=result}}
        }
        if(p!=null&&wanted!=null&&p.isPlaying&&p.isCurrentMediaItemSeekable&&p.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)){
            val now=android.os.SystemClock.elapsedRealtime()
            if(now-lastSeek>=1000L)Batch25.target(p.currentPosition,p.duration,segments,mask)?.let{target->lastSeek=now;p.seekTo(target)}
        }
        handler.postDelayed(this,250L)
    }}
    fun start(){if(!active){active=true;handler.post(tick)}}
    fun close(){active=false;generation++;handler.removeCallbacks(tick);io.shutdownNow()}
}
