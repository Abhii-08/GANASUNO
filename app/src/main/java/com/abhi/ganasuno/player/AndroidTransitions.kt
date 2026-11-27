package com.abhi.ganasuno.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.abhi.ganasuno.data.LibraryStore

/** Opt-in two-deck audio transition. The media session always owns the primary deck. */
@UnstableApi
class AndroidTransitions(private val context:Context,private val primary:ExoPlayer,private val factory:DataSource.Factory,private val primarySweep:DeckSweep):Player.Listener {
    private val handler=Handler(Looper.getMainLooper())
    private var secondary:ExoPlayer?=null
    private var secondarySweep:DeckSweep?=null
    private var outgoing:MediaItem?=null
    private var incoming:MediaItem?=null
    private var outgoingIndex=-1
    private var incomingIndex=-1
    private var attempted:MediaItem?=null
    private var fadeLength=0L
    private var fading=false
    private var handingOff=false
    private var handoffStart=0L
    private var savedVolume=1f
    private var closed=false
    private val ticker=object:Runnable{override fun run(){if(!closed){runCatching{tick()}.onFailure{cancel()};handler.postDelayed(this,50L)}}}
    init {primary.addListener(this);handler.post(ticker)}
    private fun live(item:MediaItem)=item.mediaMetadata.extras?.getBoolean("live")==true
    private fun blocked()=SleepTimer.isActive||ManualFade.isActive||PlayerHub.roomActive||!primary.playWhenReady||primary.playbackParameters.speed!=1f||primary.playbackParameters.pitch!=1f||primary.skipSilenceEnabled
    private fun same(a:MediaItem,b:MediaItem):Boolean{
        val tags=LibraryStore.albumTags()
        return TransitionMath.sameAlbum(tags[a.mediaId]?:a.mediaMetadata.albumTitle?.toString().orEmpty(),tags[b.mediaId]?:b.mediaMetadata.albumTitle?.toString().orEmpty(),a.mediaMetadata.artist?.toString().orEmpty(),b.mediaMetadata.artist?.toString().orEmpty())
    }
    private fun prepare(a:MediaItem,b:MediaItem,i:Int,j:Int){
        attempted=a;outgoing=a;incoming=b;outgoingIndex=i;incomingIndex=j
        val sweep=DeckSweep().also{it.incoming=true};secondarySweep=sweep
        val p=ExoPlayer.Builder(context,EffectsRenderers(context,sweep)).setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(factory)).setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),false).setHandleAudioBecomingNoisy(false).build()
        secondary=p;p.volume=0f;p.setMediaItem(b);p.prepare()
    }
    private fun tick(){
        val p=secondary
        if(blocked()||LibraryStore.option("crossfade_seconds")==0){if(p!=null)cancel();return}
        if(handingOff){
            if(p==null||!primary.playWhenReady||primary.currentMediaItem!=incoming||p.playerError!=null||!p.isPlaying){cancel();return}
            if(primary.isPlaying||SystemClock.elapsedRealtime()-handoffStart>2500L){cancel()};return
        }
        val a=primary.currentMediaItem?:run{cancel();return}
        val i=primary.currentMediaItemIndex;val j=primary.nextMediaItemIndex
        val duration=primary.duration;val remaining=duration-primary.currentPosition
        if(!primary.isPlaying||duration==C.TIME_UNSET||duration<=0||!primary.isCurrentMediaItemSeekable||j<0||j==i||j>=primary.mediaItemCount){if(p!=null)cancel();return}
        val b=primary.getMediaItemAt(j)
        val seconds=LibraryStore.option("crossfade_seconds").coerceIn(0,15)
        val length=TransitionMath.durationMs(seconds).coerceAtMost(duration/2)
        if(!LibraryStore.allowed(b.mediaId)||!TransitionMath.canOverlap(seconds,remaining,live(a),live(b),same(a,b),LibraryStore.option("keep_album_continuous",1)==1)){if(p!=null)cancel();return}
        if(p==null){if(attempted!=a&&length>=500&&remaining<=length+5000&&remaining>300)prepare(a,b,i,j);return}
        if(a!=outgoing||b!=incoming||i!=outgoingIndex||j!=incomingIndex||p.playerError!=null||remaining>length+5000){cancel();return}
        if(!fading&&remaining<=length&&remaining>=300&&p.playbackState==Player.STATE_READY){
            fadeLength=remaining.coerceAtMost(if(p.duration>0)p.duration/2 else remaining)
            if(fadeLength<300){cancel();return};if(remaining>fadeLength+100)return
            savedVolume=primary.volume;fading=true;p.play()
        }
        if(fading){
            if(p.playbackState!=Player.STATE_READY){cancel();return}
            val t=TransitionMath.progress(remaining,fadeLength);val gains=TransitionMath.gains(t)
            primary.volume=savedVolume*gains.first;p.volume=savedVolume*gains.second
            if(LibraryStore.option("dj_transition")==1){primarySweep.incoming=false;primarySweep.progress=t;secondarySweep?.progress=t}else{primarySweep.clear();secondarySweep?.clear()}
        }
    }
    override fun onMediaItemTransition(mediaItem:MediaItem?,reason:Int){
        attempted=null
        val p=secondary?:return
        if(reason==Player.MEDIA_ITEM_TRANSITION_REASON_AUTO&&fading&&mediaItem==incoming&&primary.currentMediaItemIndex==incomingIndex&&p.isPlaying&&!blocked()){
            handingOff=true;handoffStart=SystemClock.elapsedRealtime();primary.volume=0f;p.volume=savedVolume;primarySweep.clear();secondarySweep?.clear()
            primary.seekTo(p.currentPosition.coerceAtLeast(0L))
        }else cancel()
    }
    override fun onPositionDiscontinuity(oldPosition:Player.PositionInfo,newPosition:Player.PositionInfo,reason:Int){if(reason==Player.DISCONTINUITY_REASON_SEEK&&!handingOff){cancel();attempted=null}}
    override fun onPlayWhenReadyChanged(playWhenReady:Boolean,reason:Int){if(!playWhenReady)cancel()}
    override fun onIsPlayingChanged(isPlaying:Boolean){if(!isPlaying&&!handingOff&&fading)cancel()}
    fun cancel(){val restore=fading||handingOff;fading=false;handingOff=false;primarySweep.clear();secondarySweep?.clear();secondarySweep=null;val p=secondary;secondary=null;p?.release();outgoing=null;incoming=null;if(restore)primary.volume=savedVolume}
    val active:Boolean get()=fading||handingOff
    fun close(){closed=true;handler.removeCallbacks(ticker);primary.removeListener(this);cancel()}
}
