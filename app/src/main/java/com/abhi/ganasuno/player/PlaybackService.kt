package com.abhi.ganasuno.player

import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import com.abhi.ganasuno.data.LibraryStore
import com.abhi.ganasuno.data.Song
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.abhi.ganasuno.data.Streams
import com.abhi.ganasuno.data.YtApi
import java.io.IOException

/**
 * Background playback. Queue items carry a fake "ganasuno://play/<videoId>" URI; the real stream URL
 * is looked up lazily, right when ExoPlayer opens the item.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private var transitions:AndroidTransitions?=null
    private val communitySkipper=CommunitySkipper({PlayerHub.player},{PlayerHub.player?.currentMediaItem?.takeIf{it.mediaMetadata.extras?.getBoolean("live")!=true && it.mediaMetadata.extras?.getString("localUri").isNullOrBlank()}?.mediaId})
    private val mixWorker = java.util.concurrent.Executors.newSingleThreadExecutor()
    private var mixBusy = false
    private var attemptedMix = ""
    private var retriedItem = ""
    @Volatile private var preloaded = ""
    private val preloadBusy=java.util.concurrent.atomic.AtomicBoolean(false)
    @Volatile private var preloadActive = true
    private lateinit var preloadFactory:androidx.media3.datasource.DataSource.Factory
    private val preloadWorker = java.util.concurrent.Executors.newSingleThreadExecutor()
    private fun preload(p: androidx.media3.exoplayer.ExoPlayer){
        if(!p.isPlaying || LibraryStore.option("preload")==0 || !p.hasNextMediaItem()){preloaded="";return}
        val index=p.nextMediaItemIndex;if(index<0||index>=p.mediaItemCount)return
        val item=p.getMediaItemAt(index);val id=item.mediaId
        if(!id.matches(Regex("[A-Za-z0-9_-]{11}"))||item.mediaMetadata.extras?.getBoolean("live")==true)return
        val mode=LibraryStore.option("preload")
        val cm=getSystemService(android.net.ConnectivityManager::class.java)
        val caps=cm?.getNetworkCapabilities(cm.activeNetwork)
        val unmetered=caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED)==true && caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true
        val quality=com.abhi.ganasuno.data.Prefs.audioQuality
        val key="$id:$quality:${com.abhi.ganasuno.data.Prefs.audioLang}:$mode:$unmetered:${LibraryStore.option("media_cache")}" 
        if(key==preloaded||!preloadBusy.compareAndSet(false,true))return
        preloaded=key
        preloadWorker.execute{try{runCatching{
            if(!preloadActive||preloaded!=key)return@runCatching
            val resolved=Streams.resolve(id)
            if(com.abhi.ganasuno.data.Batch20.canPreload(mode==2,LibraryStore.option("media_cache")==1,unmetered,resolved.live,resolved.url.startsWith("https://"))&&preloadActive){
                val currentCaps=cm?.getNetworkCapabilities(cm.activeNetwork)
                if(currentCaps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED)!=true||LibraryStore.option("preload")!=2)return@runCatching
                val source=preloadFactory.createDataSource()
                try {
                    val limit=com.abhi.ganasuno.data.Batch20.PRELOAD_BYTES
                    source.open(DataSpec.Builder().setUri(Uri.parse("$SCHEME://play/$id")).setLength(limit).build())
                    val bytes=ByteArray(32768);var total=0L
                    while(preloadActive&&preloaded==key&&!Thread.currentThread().isInterrupted&&total<limit&&LibraryStore.option("preload")==2&&LibraryStore.option("media_cache")==1){
                        if(cm?.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED)!=true)break
                        val n=source.read(bytes,0,minOf(bytes.size.toLong(),limit-total).toInt());if(n<=0)break;total+=n
                    }
                }finally{source.close()}
            }
        }}finally{preloadBusy.set(false)}}
    }
    private fun extendQueue(p: androidx.media3.exoplayer.ExoPlayer) {
        if(LibraryStore.option("endless")!=1 || mixBusy || !p.isPlaying || p.repeatMode!=Player.REPEAT_MODE_OFF || p.hasNextMediaItem())return
        val item=p.currentMediaItem?:return
        if(item.mediaMetadata.extras?.getBoolean("live")==true)return
        val localUri=item.mediaMetadata.extras?.getString("localUri").orEmpty()
        val offline=localUri.isNotBlank()
        val artist=item.mediaMetadata.artist?.toString().orEmpty()
        if(artist.isBlank()&&item.mediaMetadata.albumTitle.isNullOrBlank())return
        if(p.mediaItemCount>=500 || attemptedMix==item.mediaId)return
        attemptedMix=item.mediaId;mixBusy=true
        val previous=(0 until p.mediaItemCount).map{p.getMediaItemAt(it).mediaId}.toSet()
        mixWorker.execute {
            val seed=Song(item.mediaId,item.mediaMetadata.title?.toString().orEmpty(),artist,"","",localUri=localUri,album=item.mediaMetadata.albumTitle?.toString().orEmpty())
            val found=runCatching {
                val history=LibraryStore.history(30)
                val groups=LibraryStore.names().map{LibraryStore.playlist(it).map{track->track.artist}}+history.groupBy{java.time.Instant.ofEpochMilli(it.at).atZone(java.time.ZoneId.systemDefault()).toLocalDate()}.values.map{day->day.map{it.song.artist}}
                val related=com.abhi.ganasuno.data.Batch17.related(artist,groups)
                val saved=LibraryStore.list("local")+LibraryStore.list("favorites")+history.map{it.song}+LibraryStore.names().flatMap{LibraryStore.playlist(it)}
                val remote=if(!offline&&artist.isNotBlank())runCatching{YtApi.search("$artist songs mix",limit=30)}.getOrDefault(emptyList())else emptyList()
                val tags=LibraryStore.albumTags()
                com.abhi.ganasuno.data.Batch18.eligible(seed.copy(album=tags[seed.id]?:seed.album), (saved+remote).map{it.copy(album=tags[it.id]?:it.album)}, related, previous,{s->
                    LibraryStore.allowed(s.id)&&(!offline||runCatching{contentResolver.openAssetFileDescriptor(Uri.parse(s.localUri),"r")?.use{true}?:false}.getOrDefault(false))
                })
            }.getOrDefault(emptyList())
            handler.post { mixBusy=false;if(PlayerHub.player!==p||p.currentMediaItem?.mediaId!=item.mediaId||p.hasNextMediaItem()||LibraryStore.option("endless")!=1)return@post
                val currentIds=(0 until p.mediaItemCount).map{p.getMediaItemAt(it).mediaId}.toSet()
                val fresh=found.filter{it.id !in currentIds&&LibraryStore.allowed(it.id)}.take((500-p.mediaItemCount).coerceAtLeast(0))
                if(fresh.isNotEmpty()){
                    val firstNew=p.mediaItemCount
                    val resumeEnded=p.playWhenReady&&p.playbackState==Player.STATE_ENDED && !PlayerHub.roomActive && !SleepTimer.untilSongEnd
                    p.addMediaItems(fresh.map{s->
                    val extras=android.os.Bundle().apply{putString("localUri",s.localUri);putBoolean("live",false);p.currentMediaItem?.mediaMetadata?.extras?.getString(com.abhi.ganasuno.ui.PlaylistGreeting.SOURCE_KEY)?.let{putString(com.abhi.ganasuno.ui.PlaylistGreeting.SOURCE_KEY,it)}}
                    androidx.media3.common.MediaItem.Builder().setMediaId(s.id).setUri(s.localUri.ifBlank{"$SCHEME://play/${s.id}"}).setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(s.title).setArtist(s.artist).setAlbumTitle(s.album.takeIf{it.isNotBlank()}).setArtworkUri(s.thumb.takeIf{it.isNotBlank()}?.let{Uri.parse(it)}).setExtras(extras).build()).build()
                    })
                    if(resumeEnded&&p.playWhenReady){p.seekTo(firstNew,0L);if(p.playbackState==Player.STATE_IDLE)p.prepare();p.play()}
                }
            }
        }
    }
    private val handler = Handler(Looper.getMainLooper())
    private var lastTick = 0L
    private var activeSong: Song? = null
    private var accumulated = 0L
    private var playElapsed = 0L
    private var playCounted = false
    private var scrobbleStarted=0L
    private var scrobbled=false
    private var wasPlaying = false
    private fun flush() { activeSong?.let { LibraryStore.record(it, accumulated) }; accumulated = 0L }
    private fun account(now:Long=SystemClock.elapsedRealtime()) {
        val delta=if(lastTick==0L)0L else (now-lastTick).coerceIn(0L,6000L);lastTick=now
        if(wasPlaying){if(scrobbleStarted==0L)scrobbleStarted=System.currentTimeMillis()/1000L;accumulated+=delta;playElapsed+=delta;if(!playCounted&&playElapsed>=com.abhi.ganasuno.data.Batch10.PLAY_THRESHOLD_MS){activeSong?.let{LibraryStore.countPlay(it);playCounted=true}}}
    }
    private fun track(item:androidx.media3.common.MediaItem?){
        account();flush();playElapsed=0;playCounted=false;scrobbleStarted=0L;scrobbled=false
        activeSong=item?.let{Song(it.mediaId,it.mediaMetadata.title?.toString().orEmpty(),it.mediaMetadata.artist?.toString().orEmpty(),"",it.mediaMetadata.artworkUri?.toString().orEmpty(),it.mediaMetadata.extras?.getBoolean("live")==true,it.mediaMetadata.extras?.getString("localUri").orEmpty(),it.mediaMetadata.albumTitle?.toString().orEmpty())}
    }
    private val tracking = object : Runnable {
        override fun run() {
            val p=PlayerHub.player
            account();wasPlaying=p?.isPlaying==true
            if(accumulated>=30000L||!wasPlaying)flush()
            if(p!=null){
                if(wasPlaying&&!scrobbled&&scrobbleStarted>0&&p.currentMediaItem?.mediaId==activeSong?.id){activeSong?.let{scrobbled=com.abhi.ganasuno.data.LastFmScrobble.submit(it,p.duration,playElapsed,scrobbleStarted)}}
                extendQueue(p);preload(p)
            }
            handler.postDelayed(this,5000L)
        }
    }

    override fun onCreate() {
        super.onCreate()

        val http =
            DefaultHttpDataSource
                .Factory()
                .setUserAgent(YtApi.WEB_UA)
                .setAllowCrossProtocolRedirects(true)

        val resolving =
            PlaybackCache.factory(this, DefaultDataSource.Factory(this, http))

        preloadFactory=resolving
        EffectsSettings.refresh()
        val primarySweep=DeckSweep()
        val player =
            ExoPlayer
                .Builder(this, EffectsRenderers(this,primarySweep))
                .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(resolving))
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true,
                ).setHandleAudioBecomingNoisy(true)
                .build()

        PlayerHub.player = player
        PlayerHub.applyAll()
        player.addListener(
            object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    val id=player.currentMediaItem?.mediaId.orEmpty()
                    val failure=PlaybackFailures.inspect(id,error)
                    PlaybackFailures.last=failure
                    com.abhi.ganasuno.data.Diagnostics.event("Playback error: ${failure.summary().replace('\n',' ')}")
                    android.util.Log.w("GANASUNO-Playback",failure.summary().replace('\n',' '))
                    // One retry for temporary transport errors only. Do not loop on access denials.
                    if(id.matches(Regex("[A-Za-z0-9_-]{11}")) && retriedItem!=id && player.playWhenReady && failure.retryable){
                        retriedItem=id;Streams.invalidate(id)
                        handler.postDelayed({ if(PlayerHub.player===player && player.currentMediaItem?.mediaId==id && player.playWhenReady && player.playerError!=null){player.prepare()} },350L)
                    }
                }
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason:Int){
                    track(mediaItem);wasPlaying=player.isPlaying
                    com.abhi.ganasuno.data.Diagnostics.event("Track transition reason $reason")
                    if(mediaItem?.mediaId!=retriedItem)retriedItem=""
                    PlaybackFailures.last=null
                    if(mediaItem!=null&&!LibraryStore.allowed(mediaItem.mediaId))handler.post{if(player.currentMediaItem?.mediaId==mediaItem.mediaId){player.removeMediaItem(player.currentMediaItemIndex)}}
                    com.abhi.ganasuno.widget.TurntableWidget.refresh(this@PlaybackService)
                }
                override fun onIsPlayingChanged(isPlaying:Boolean){if(isPlaying)PlaybackFailures.last=null;account();wasPlaying=isPlaying;if(!isPlaying)flush();com.abhi.ganasuno.data.Diagnostics.event(if(isPlaying)"Playback active" else "Playback inactive")}
                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    PlayerHub.applyNormalize()
                    ReverbControl.apply()
                    com.abhi.ganasuno.ui.EqualizerDialog.apply()
                }
            },
        )

        player.repeatMode = LibraryStore.option("repeat", Player.REPEAT_MODE_OFF).coerceIn(0,2)
        player.shuffleModeEnabled = LibraryStore.option("shuffle") == 1
        transitions=AndroidTransitions(this,player,resolving,primarySweep)
        PlayerHub.cancelTransition={transitions?.cancel()}
        PlayerHub.transitionActive={transitions?.active==true}
        session = MediaSession.Builder(this, player).build()
        lastTick = SystemClock.elapsedRealtime()
        handler.post(tracking)
        communitySkipper.start()
    }

    private fun resolve(spec: DataSpec): DataSpec {
        val uri = spec.uri
        if (uri.scheme != SCHEME) return spec
        val id = uri.lastPathSegment ?: throw IOException("Bad song id")
        val url =
            try {
                Streams.resolve(id).url
            } catch (e: Exception) {
                throw IOException("Stream lookup failed: ${e.message}", e)
            }
        return spec.withUri(Uri.parse(url))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        if(LibraryStore.option("stop_on_exit")==1){PlayerHub.player?.pause();stopSelf()} else super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        transitions?.close();transitions=null;PlayerHub.cancelTransition=null;PlayerHub.transitionActive=null
        communitySkipper.close()
        handler.removeCallbacks(tracking)
        mixWorker.shutdownNow()
        preloadActive=false
        preloadWorker.shutdownNow()
        account();wasPlaying=false;flush()
        ManualFade.cancel()
        SleepTimer.cancel()
        com.abhi.ganasuno.ui.EqualizerDialog.release()
        PlayerHub.release()
        session?.run {
            player.release()
            release()
        }
        session = null
        PlaybackCache.release()
        super.onDestroy()
    }

    companion object {
        const val SCHEME = "ganasuno"
    }
}
