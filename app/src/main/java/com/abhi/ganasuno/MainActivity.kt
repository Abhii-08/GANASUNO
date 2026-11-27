@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.abhi.ganasuno

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.EditText
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.ImageView
import android.widget.Button
import android.view.MotionEvent
import android.media.MediaMetadataRetriever
import android.provider.OpenableColumns
import androidx.appcompat.app.AppCompatDelegate
import androidx.media3.common.PlaybackParameters
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.abhi.ganasuno.data.LibraryStore
import com.abhi.ganasuno.data.Batch14
import com.abhi.ganasuno.data.Batch15
import com.abhi.ganasuno.data.RemoteMetadata
import com.abhi.ganasuno.player.PlaybackCache
import com.abhi.ganasuno.ui.Lyrics
import java.util.Calendar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.android.material.chip.Chip
import com.google.common.util.concurrent.ListenableFuture
import com.abhi.ganasuno.data.QueueStore
import com.abhi.ganasuno.data.Shuffler
import com.abhi.ganasuno.data.Song
import com.abhi.ganasuno.data.YtApi
import com.abhi.ganasuno.databinding.ActivityMainBinding
import com.abhi.ganasuno.player.PlaybackService
import com.abhi.ganasuno.ui.MiniPlaybackUi
import com.abhi.ganasuno.ui.ImageLoader
import com.abhi.ganasuno.ui.SettingsDialog
import com.abhi.ganasuno.ui.SongAdapter
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private lateinit var adapter: SongAdapter

    private var songs: List<Song> = emptyList()
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var userSeeking = false
    private var requestSerial = 0
    private var currentPlaylist: String? = null
    private var displayedSource: String? = null
    private var homeGreetingActive = false
    private lateinit var playlistGreeting: com.abhi.ganasuno.ui.AnimatedPlaylistGreeting
    private fun playbackSource(item: MediaItem?): String? = item?.mediaMetadata?.extras?.getString(com.abhi.ganasuno.ui.PlaylistGreeting.SOURCE_KEY)
    private fun refreshPlaylistGreeting() {
        // Page identity alone is insufficient: a local song list can still have page == Home.
        if (!homeGreetingActive || page != "Home") return
        playlistGreeting.bind(com.abhi.ganasuno.ui.PlaylistGreeting.source(playbackSource(controller?.currentMediaItem)))
    }
    private fun stopHomeGreeting() { homeGreetingActive = false; playlistGreeting.unbind() }
    private fun clearPlaylistGreeting() { stopHomeGreeting(); displayedSource = null }

    private var page = "Home"
    private var libraryGroup: String? = null
    private var settingsGroup: String? = null
    private var videoResults=false
    private var pendingSearch: Runnable? = null
    private var dockArtwork: String? = null
    private lateinit var glassDock: com.abhi.ganasuno.ui.LiquidGlassDock
    private var darkGlass = true
    private val tabScroll = mutableMapOf<String,Int>()
    private var restoreTabScroll = false
    private var libraryGrid = false
    private fun cancelSearch() { pendingSearch?.let { ui.removeCallbacks(it) }; pendingSearch = null }
    private fun setupSearchInput() {
        b.btnSearchClear.setOnClickListener { cancelSearch(); openSearch() }
        b.search.addTextChangedListener(object: android.text.TextWatcher {
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int) = Unit
            override fun afterTextChanged(s:android.text.Editable?) = Unit
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) {
                cancelSearch()
                b.btnSearchClear.visibility=if(s.isNullOrEmpty()) View.GONE else View.VISIBLE
                if(page!="Search" || !b.search.hasFocus()) return
                ++requestSerial
                b.progress.visibility=View.GONE
                val query=s.toString().trim()
                b.suggestRow.removeAllViews()
                val matches=(LibraryStore.searches()+catalogueTracks().map{it.title}+quick).distinct().filter{query.isNotEmpty()&&it.contains(query,true)}.take(6)
                matches.forEach { name -> b.suggestRow.addView(Chip(this@MainActivity).apply{text=name;setOnClickListener{doSearch(name)}}) }
                b.suggestScroll.visibility=if(matches.isEmpty())View.GONE else View.VISIBLE
                if(query.length>=2) {
                    val job=Runnable { if(!isDestroyed&&page=="Search"&&b.search.hasFocus()&&b.search.text.toString().trim()==query) doSearch(query) }
                    pendingSearch=job; ui.postDelayed(job,850)
                }
            }
        })
    }
    private fun updateDock(url:String) {
        if(dockArtwork==url)return
        dockArtwork=url
        glassDock.artwork(null)
        com.abhi.ganasuno.ui.ImageLoader.load(url,b.miniThumb,R.drawable.ic_placeholder){ bitmap ->
            if(!isDestroyed&&dockArtwork==url) { glassDock.artwork(bitmap); if(android.animation.ValueAnimator.areAnimatorsEnabled()&&LibraryStore.option("glass_reduced")==0){b.miniThumb.alpha=.65f;b.miniThumb.animate().alpha(1f).setDuration(220).start()} }
        }
    }
    private val importAudio = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }.onSuccess {
                    io.execute {
                        runCatching {
                            val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use { if(it.moveToFirst()) it.getString(0) else null } ?: "Local audio"
                            val retriever = MediaMetadataRetriever()
                            val song = try {
                                retriever.setDataSource(this,uri)
                                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                                Song("local:"+uri.toString(),retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: name,retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Local audio",formatTime(duration),"",false,uri.toString(),retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty()).also{track->retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.take(4)?.toIntOrNull()?.takeIf{it in 1900..2100}?.let{year->if(LibraryStore.releaseYear(track.id)==null)LibraryStore.setReleaseYear(track.id,year)}}
                            } finally { retriever.release() }
                            LibraryStore.addLocal(song)
                        }.onFailure { ui.post { toast("Could not import audio: ${it.message}") } }
                        ui.post { if(!isDestroyed) showLocalList(LibraryStore.list("local"),"Offline audio") }
                    }
                }.onFailure { toast("File access was not granted") }
            }
        }
    }
    private var coverPlaylist:String?=null
    private var animatedCoverId:String?=null
    private val animatedCoverPicker=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        val id=animatedCoverId;animatedCoverId=null
        if(uri!=null&&id!=null){io.execute{val r=runCatching{com.abhi.ganasuno.ui.AnimatedCover.save(this,id,uri)};ui.post{if(!isDestroyed)r.onSuccess{LibraryStore.option("animated_art",2,true);toast("Animated cover saved; reopen Now Playing")}.onFailure{toast(it.message?:"Cannot save animated cover")}}}}
    }
    private val coverPicker=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->val name=coverPlaylist;coverPlaylist=null;if(uri!=null&&name!=null)editCover(name,uri)}
    private fun editCover(name:String,uri:Uri){
        io.execute{val result=runCatching{com.abhi.ganasuno.ui.PlaylistCover.decode(this,uri)};ui.post{if(!isDestroyed)result.onSuccess{bitmap->choose("Square crop position",listOf("Centre","Top / left","Bottom / right")){n->val anchor=listOf(.5f,0f,1f)[n];val cropped=com.abhi.ganasuno.ui.PlaylistCover.crop(bitmap,anchor);val preview=ImageView(this).apply{setImageBitmap(cropped);adjustViewBounds=true;maxHeight=(260*resources.displayMetrics.density).toInt()};MaterialAlertDialogBuilder(this).setTitle("Save playlist cover?").setView(preview).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->runCatching{LibraryStore.setCover(name,com.abhi.ganasuno.ui.PlaylistCover.encode(cropped))}.onSuccess{playlistDetails(name)}.onFailure{toast("Cover could not be saved")}}.show()}}.onFailure{toast("Image could not be opened")}}}
    }
    private var pendingVideo:com.abhi.ganasuno.data.VideoOffline.Download?=null
    private val exportVideo=registerForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")){dest->
        val download=pendingVideo;pendingVideo=null
        if(download!=null){if(dest==null)download.file.delete()else io.execute{val result=runCatching{contentResolver.openOutputStream(dest)?.use{out->download.file.inputStream().use{it.copyTo(out)}}?:error("Cannot write selected document")};download.file.delete();ui.post{if(!isDestroyed)toast(if(result.isSuccess)"Video saved" else "Video export failed; remove any incomplete document")}}}
    }
    private fun authorizedVideo(){
        choose("Choose offered video quality",listOf("360p","480p","720p","1080p","2160p")){n->val selected=listOf(360,480,720,1080,2160)[n];MaterialAlertDialogBuilder(this).setTitle("Authorized MP4 · ${selected}p").setMessage("Enter the direct URL of the matching offered variant only if you have permission to download it. No service-stream extraction or conversion. Max 200 MB; actual height is checked before export.").setNegativeButton("Cancel",null).setPositiveButton("Continue"){_,_->ask("Direct HTTPS MP4 URL"){raw->val token=com.abhi.ganasuno.data.OfflineFiles.Cancellation();val dialog=MaterialAlertDialogBuilder(this).setTitle("Downloading authorized video").setMessage("Checking direct file…").setCancelable(false).setNegativeButton("Cancel"){_,_->token.cancel()}.show();io.execute{val result=runCatching{com.abhi.ganasuno.data.VideoOffline.fetch(this,raw,token)};ui.post{if(isDestroyed||isFinishing){result.getOrNull()?.file?.delete();return@post};dialog.dismiss();result.onSuccess{download->MaterialAlertDialogBuilder(this).setTitle("Actual video: ${download.height}p").setMessage("Requested ${selected}p; decoded height ${download.height}p · ${download.bytes/1048576} MB. No conversion. Save to a location you choose?").setNegativeButton("Discard"){_,_->download.file.delete()}.setOnCancelListener{download.file.delete()}.setPositiveButton("Save"){_,_->pendingVideo?.file?.delete();pendingVideo=download;exportVideo.launch("ganasuno-${download.height}p.mp4")}.show()}.onFailure{toast(if(token.isCancelled())"Download cancelled" else it.message?:"Video download failed")}}}}}.show()}
    }
    private var pendingCard:Uri?=null
    private val saveCard = registerForActivityResult(ActivityResultContracts.CreateDocument("image/png")){dest->
        val source=pendingCard;pendingCard=null
        if(dest!=null&&source!=null)io.execute{val result=runCatching{contentResolver.openInputStream(source)?.use{input->contentResolver.openOutputStream(dest)?.use{out->input.copyTo(out)}?:error("Cannot save image")}?:error("Image unavailable")};ui.post{if(!isDestroyed)toast(if(result.isSuccess)"Image saved" else "Image save failed")}}
    }
    private fun saveShareImage(uri:Uri){pendingCard=uri;saveCard.launch("ganasuno-lyrics.png")}
    private val exportLibrary = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if(uri != null) io.execute { val result=runCatching { contentResolver.openOutputStream(uri)?.use { it.write(LibraryStore.backup().toByteArray(Charsets.UTF_8)) } ?: error("Cannot open document") };ui.post { toast(if(result.isSuccess) "Library backed up" else "Backup failed") } }
    }
    private val restoreLibrary = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri != null) MaterialAlertDialogBuilder(this).setTitle("Replace local library?").setMessage("This replaces favourites, playlists, history and saved lyrics. Audio files and device permissions are not included.").setNegativeButton("Cancel",null).setPositiveButton("Restore") { _,_ -> io.execute { val result=runCatching { val raw=contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Cannot read backup"); LibraryStore.restore(raw) };ui.post { toast(if(result.isSuccess) "Library restored" else "Invalid backup");if(result.isSuccess)showLocalList(LibraryStore.list("favorites"),"Favourites") } } }.show()
    }

    private val exportMediaArchive=registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")){uri->
        if(uri!=null)io.execute{val r=runCatching{contentResolver.openOutputStream(uri)?.use{com.abhi.ganasuno.data.MediaArchive.write(this,it,com.abhi.ganasuno.data.TransferSettings.export())}?:error("Cannot open archive")};ui.post{if(!isDestroyed)toast(if(r.isSuccess)"Audio archive saved" else "Backup failed: ${r.exceptionOrNull()?.message}")}}
    }
    private val importMediaArchive=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)MaterialAlertDialogBuilder(this).setTitle("Restore audio archive?").setMessage("Library metadata and included portable settings will be replaced. Existing audio is kept, and conflicting files are rejected. External file permissions cannot be restored. Restart the app to apply settings.").setNegativeButton("Cancel",null).setPositiveButton("Restore"){_,_->io.execute{val r=runCatching{contentResolver.openInputStream(uri)?.use{com.abhi.ganasuno.data.MediaArchive.restore(this,it,{raw->com.abhi.ganasuno.data.TransferSettings.restore(raw,false)},{raw->com.abhi.ganasuno.data.TransferSettings.restore(raw)})}?:error("Cannot open archive")};ui.post{if(!isDestroyed){r.onSuccess{toast("Restored $it audio files");showLocalList(LibraryStore.list("local"),"Offline audio")}.onFailure{toast("Restore failed: ${it.message}")}}}}}.show()
    }
    private val io = Executors.newSingleThreadExecutor()
    private val analysisWorker = Executors.newSingleThreadExecutor()
    private var analysisBusy=false
    private val ui = Handler(Looper.getMainLooper())
    private val rooms by lazy { com.abhi.ganasuno.ui.ListeningRooms(this,{controller},{toMediaItem(it)}) }
    private val musicCatalogue by lazy { com.abhi.ganasuno.ui.MusicCatalogueTools(this,io,{q,kind->publicCatalogue.search(q,kind)},{id,title->publicCatalogue.channel(id,title)}){q->b.search.setText(q);doSearch(q)} }
    private val publicCatalogue by lazy { com.abhi.ganasuno.ui.PublicCatalogueTools(this,io) }
    private val advanced by lazy { com.abhi.ganasuno.ui.AdvancedTools(this,io,{doSearch(it)},{showLocalList(LibraryStore.playlist(it),it,it)},{saveShareImage(it)},{controller}) }

    private val ticker =
        object : Runnable {
            override fun run() {
                updateProgress()
                playlistGreeting.visibilityChanged()
                refreshSettingIndicators()
                ui.postDelayed(this, 500)
            }
        }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val playerListener =
        object : Player.Listener {
            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int,
            ) {
                syncMini()
                saveQueue()
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) { refreshPlaylistGreeting() }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) { refreshPlaylistGreeting() }

            override fun onIsPlayingChanged(isPlaying: Boolean) = syncMini()

            override fun onPlaybackStateChanged(playbackState: Int) = syncMini()

            // Preparing a restored queue is not a request to play. Also update when
            // Play/Pause changes while the player is still in STATE_BUFFERING.
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = syncMini()

            override fun onPlayerError(error: PlaybackException) {
                syncMini()
                val id=controller?.currentMediaItem?.mediaId.orEmpty()
                ui.postDelayed({
                    val c=controller
                    if(!isFinishing && !isDestroyed && c!=null && c.currentMediaItem?.mediaId==id && c.playerError!=null){
                        val failure=com.abhi.ganasuno.player.PlaybackFailures.last?.takeIf{it.item==id}
                            ?:com.abhi.ganasuno.player.PlaybackFailures.inspect(id,error)
                        toast(failure.text+" Tap Play for options.")
                        syncMini()
                    }
                },850L)
            }
        }

    private val quick =
        listOf("Hindi", "Bollywood", "Punjabi", "Romantic", "Lofi", "Old Hits", "Bhojpuri", "English")

    override fun onCreate(savedInstanceState: Bundle?) {
        val palette=LibraryStore.option("palette")
        when(palette){2->setTheme(R.style.Theme_Ganasuno_Green);3->setTheme(R.style.Theme_Ganasuno_Orange);4->setTheme(R.style.Theme_Ganasuno_Pink);5->setTheme(R.style.Theme_Ganasuno_Red)}
        super.onCreate(savedInstanceState)
        coverPlaylist=savedInstanceState?.getString("cover_playlist")
        animatedCoverId=savedInstanceState?.getString("animated_cover_id")
        pendingCard=savedInstanceState?.getString("pending_card")?.let{Uri.parse(it)}
        if(palette==1)com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(this)
        if(palette==6&&android.os.Build.VERSION.SDK_INT>=31)com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(this,com.google.android.material.color.DynamicColorsOptions.Builder().setContentBasedSource(LibraryStore.option("custom_color",0xff7c3aed.toInt())).build())
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        playlistGreeting = com.abhi.ganasuno.ui.AnimatedPlaylistGreeting(b.headerFlipCard, b.sectionTitle, b.sectionSubtitle) { LibraryStore.option("glass_reduced") == 1 }
        if(palette==6&&android.os.Build.VERSION.SDK_INT<31){val color=LibraryStore.option("custom_color",0xff7c3aed.toInt());val tint=android.content.res.ColorStateList.valueOf(color);b.btnPlayAll.backgroundTintList=tint;b.btnShuffle.backgroundTintList=tint;b.seek.progressTintList=tint;b.seek.thumbTintList=tint;b.miniPlay.imageTintList=tint;val foreground=if(androidx.core.graphics.ColorUtils.calculateLuminance(color)>.45)android.graphics.Color.BLACK else android.graphics.Color.WHITE;b.btnPlayAll.setTextColor(foreground);b.btnShuffle.setTextColor(foreground)}
        b.root.layoutDirection=when(LibraryStore.option("rtl_preview")){1->View.LAYOUT_DIRECTION_LTR;2->View.LAYOUT_DIRECTION_RTL;else->View.LAYOUT_DIRECTION_LOCALE}

        darkGlass=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        glassDock=com.abhi.ganasuno.ui.LiquidGlassDock(b.glassContent,b.playerDock,b.bottomNav,b.mini,listOf(b.list,b.toolsScroll),darkGlass)
        glassDock.configure(LibraryStore.option("glass_reduced")==1)
        val controlColor=if(darkGlass)android.graphics.Color.WHITE else 0xff202020.toInt()
        val controlTint=android.content.res.ColorStateList.valueOf(controlColor)
        listOf(b.miniPrev,b.miniPlay,b.miniNext,b.miniVideo).forEach{it.imageTintList=controlTint}
        b.miniTitle.setTextColor(controlColor);b.miniArtist.setTextColor(if(darkGlass)0xffc4c4c4.toInt() else 0xff595959.toInt())
        b.seek.progressTintList=controlTint;b.seek.thumbTintList=controlTint;b.seek.progressBackgroundTintList=android.content.res.ColorStateList.valueOf(if(darkGlass)0x55ffffff else 0x55000000)
        b.miniBuffering.indeterminateTintList=android.content.res.ColorStateList.valueOf(0xff8e8e93.toInt())
        val searchIconTint=android.content.res.ColorStateList.valueOf(if(darkGlass)0xffc4c4c4.toInt() else 0xff595959.toInt())
        listOf(b.btnSearchGo,b.btnSearchClear,b.btnSettings).forEach{it.imageTintList=searchIconTint}
        b.miniPrev.visibility=if(resources.configuration.screenWidthDp<380)View.GONE else View.VISIBLE
        // Preserve 48dp touch targets. Only add the first gap if Previous is visible.
        b.miniPlay.layoutParams=(b.miniPlay.layoutParams as LinearLayout.LayoutParams).apply { marginStart=if(b.miniPrev.visibility==View.VISIBLE)dp(6) else 0 }
        b.miniNext.layoutParams=(b.miniNext.layoutParams as LinearLayout.LayoutParams).apply { marginStart=dp(if(resources.configuration.screenWidthDp<360)4 else 6) }
        if(resources.configuration.screenWidthDp>640)b.playerDock.layoutParams=b.playerDock.layoutParams.apply{width=dp(640)}
        b.mini.setOnClickListener{nowPlaying(true)}
        ViewCompat.setOnApplyWindowInsetsListener(b.container) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            b.playerDock.visibility=if(insets.isVisible(WindowInsetsCompat.Type.ime()))View.GONE else View.VISIBLE
            glassDock.clearance();glassDock.refresh()
            insets
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        adapter = SongAdapter({ index -> if(videoResults&&LibraryStore.option("video_mode")==1) songs.getOrNull(index)?.let{playVideo(it)} else startQueue(songs, index, displayedSource) }, { index -> songs.getOrNull(index)?.let { songActions(it) } })
        b.list.adapter = adapter

        b.search.setOnEditorActionListener { view, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch(view.text.toString())
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(view.windowToken, 0)
                true
            } else {
                false
            }
        }

        // "Live" chip first, then the quick searches
        val liveChip = Chip(this)
        liveChip.text = "Live"
        liveChip.setOnClickListener { doLive() }
        b.chips.addView(liveChip)
        b.chips.addView(Chip(this).apply{text="Videos";setOnClickListener{val q=b.search.text.toString().trim().ifBlank{"music videos"};runSearch(R.string.no_results,video=true){YtApi.searchVideos(q)}}})
        quick.forEach { label ->
            val chip = Chip(this)
            chip.text = label
            chip.setOnClickListener {
                b.search.setText(label)
                doSearch("$label songs", label)
            }
            b.chips.addView(chip)
        }

        b.btnSettings.setOnClickListener { openSettings() }
        b.btnSettings.contentDescription=getString(R.string.settings)
        setupNavigation()
        setupSearchInput()
        b.miniTitle.isSelected=android.animation.ValueAnimator.areAnimatorsEnabled()
        b.root.requestFocus()
        b.miniThumb.setOnClickListener { nowPlaying(true) }
        b.miniTitle.setOnClickListener { nowPlaying(true) }
        b.miniArtist.setOnClickListener { nowPlaying(true) }
        for (target in listOf(b.miniThumb,b.miniTitle,b.miniArtist)) {
            var x=0f;var y=0f
            target.setOnTouchListener { v,event ->
                when(event.actionMasked){
                    MotionEvent.ACTION_DOWN -> {x=event.rawX;y=event.rawY;v.parent.requestDisallowInterceptTouchEvent(true);true}
                    MotionEvent.ACTION_UP -> {v.parent.requestDisallowInterceptTouchEvent(false);if(event.rawY-y < -dp(48) && kotlin.math.abs(event.rawY-y)>kotlin.math.abs(event.rawX-x)) nowPlaying(true) else when(com.abhi.ganasuno.data.BatchCore.swipe(event.rawX-x,event.rawY-y,36*resources.displayMetrics.density)){
                        1->skipTrack(false);2->skipTrack(true);3->closeMini();else->v.performClick()
                    };true}
                    MotionEvent.ACTION_CANCEL -> {v.parent.requestDisallowInterceptTouchEvent(false);true}
                    else -> true
                }
            }
            target.setOnLongClickListener { choose("Mini player",listOf("Previous","Next","Close player")){when(it){0->skipTrack(false);1->skipTrack(true);2->closeMini()}};true }
        }
        fun shortcut(label:String, action:()->Unit) { val chip=Chip(this);chip.text=label;chip.setOnClickListener{action()};b.chips.addView(chip,0) }
        shortcut("Discover") { discoverMenu() }
        shortcut("Quick picks") { personalized(0) }
        shortcut("Daily discover") { dailyCards() }
        shortcut("History") { historyMenu() }
        shortcut("Library") { openLibrary() }
        shortcut("Home") { home() }
        b.search.setOnLongClickListener { searchHistory();true }
        b.btnPlayAll.setOnClickListener { if (songs.isNotEmpty()) startQueue(songs.filter{LibraryStore.allowed(it.id)}, 0, displayedSource) }
        b.btnShuffle.setOnClickListener {
            if (songs.isNotEmpty()) startQueue(Shuffler.smart(songs.filter{LibraryStore.allowed(it.id)}), 0, displayedSource)
        }

        b.miniPlay.setOnClickListener {
            controller?.let { togglePlayback(it) }
        }
        b.miniNext.setOnClickListener { skipTrack(true) }
        b.miniPrev.setOnClickListener { skipTrack(false) }
        b.miniVideo.setOnClickListener { openVideo() }

        b.seek.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) = Unit

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    userSeeking = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    controller?.seekTo((seekBar?.progress ?: 0) * 1000L)
                    userSeeking = false
                }
            },
        )

        if (!handleShortcut(intent) && !handleMusicLink(intent)) when(savedInstanceState?.getString("ui_page")){"Settings"->{val section=savedInstanceState?.getString("settings_group");if(section==null)openSettings()else openSettingsGroup(section)};"Library"->openLibrary();"Search"->openSearch();else->home()}
        onBackPressedDispatcher.addCallback(this,object:androidx.activity.OnBackPressedCallback(true){override fun handleOnBackPressed(){when{page=="Library"&&b.list.visibility==View.VISIBLE->{val group=libraryGroup;if(group!=null)openLibraryGroup(group)else openLibrary()};page=="Library"&&libraryGroup!=null->openLibrary();page=="Search"&&b.list.visibility==View.VISIBLE->openSearch();page=="Settings"&&settingsGroup!=null->openSettings();page!="Home"->home();else->{isEnabled=false;onBackPressedDispatcher.onBackPressed();isEnabled=true}}}})
        advanced.checkUpdate(true)
        if(savedInstanceState==null)com.abhi.ganasuno.data.CrashRecord.pending(this)?.let{report->
            val fingerprint=com.abhi.ganasuno.data.Batch20.digest(report+com.abhi.ganasuno.data.CrashRecord.stamp(this))
            val seen=getSharedPreferences("crash_recovery",MODE_PRIVATE)
            if(seen.getString("last",null)!=fingerprint){seen.edit().putString("last",fingerprint).apply();b.root.post{if(!isFinishing&&!isDestroyed)showCrashReport(report)}}
        }
    }

    override fun onSaveInstanceState(outState:Bundle){outState.putString("ui_page",page);outState.putString("settings_group",settingsGroup);animatedCoverId?.let{outState.putString("animated_cover_id",it)};coverPlaylist?.let{outState.putString("cover_playlist",it)};pendingCard?.let{outState.putString("pending_card",it.toString())};super.onSaveInstanceState(outState)}

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!handleShortcut(intent)) handleMusicLink(intent)
    }

    private var pendingPlaylist:String?=null
    private fun playWidgetPlaylist(name:String){if(name !in LibraryStore.names()){toast("Playlist no longer exists");return};val list=LibraryStore.playlist(name);showLocalList(list,name,name);if(controller==null)pendingPlaylist=name else if(list.any{LibraryStore.allowed(it.id)})startQueue(list,list.indexOfFirst{LibraryStore.allowed(it.id)},name)else toast("No playable playlist tracks")}
    private fun handleShortcut(intent: Intent): Boolean = when(intent.action) {
        "com.abhi.ganasuno.PLAY_WIDGET_PLAYLIST" -> {intent.getStringExtra("playlist")?.let{playWidgetPlaylist(it)};true}
        "com.abhi.ganasuno.LIBRARY" -> { openLibrary(); true }
        "com.abhi.ganasuno.HISTORY" -> { home(); historyMenu(); true }
        "com.abhi.ganasuno.TURNTABLE" -> { home(); if(controller?.currentMediaItem!=null)nowPlaying() else showLocalList(LibraryStore.history().asReversed().map{it.song}.distinctBy{it.id}.take(30),"Recently played"); true }
        else -> false
    }
    private fun handleMusicLink(intent:Intent): Boolean {
        val uri=intent.data ?: return false
        val host=uri.host ?: return false
        val id=when(host){"youtu.be"->uri.pathSegments.firstOrNull();"www.youtube.com","youtube.com","music.youtube.com"->if(uri.path=="/watch")uri.getQueryParameter("v") else null;else->null} ?: return false
        if(!id.matches(Regex("[A-Za-z0-9_-]{11}")))return false
        showLocalList(listOf(Song(id,"Linked video","YouTube","","https://i.ytimg.com/vi/$id/hqdefault.jpg")),"Shared link")
        return true
    }

    override fun onStart() {
        super.onStart()
        rooms.foreground(true)
        glassDock.start()
        playlistGreeting.setForeground(true)
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    val c = future.get()
                    if (controllerFuture !== future) return@addListener
                    controller = c
                    c.addListener(playerListener)
                    if (c.mediaItemCount == 0) restoreQueue(c)
                    syncMini()
                    pendingPlaylist?.let{pendingPlaylist=null;playWidgetPlaylist(it)}
                } catch (e: Exception) {
                    // service not ready; the next tap will tell the user
                }
            },
            ContextCompat.getMainExecutor(this),
        )
        ui.post(ticker)
    }

    override fun onStop() {
        com.abhi.ganasuno.widget.LibraryWidget.refresh(this)
        rooms.foreground(false)
        glassDock.stop()
        playlistGreeting.setForeground(false)
        cancelSearch()
        ui.removeCallbacks(ticker)
        saveQueue()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
        b.miniBuffering.visibility = View.GONE
        super.onStop()
    }

    override fun onDestroy() {
        playlistGreeting.dispose()
        rooms.stop()
        io.shutdown()
        analysisWorker.shutdownNow()
        super.onDestroy()
    }


    private data class ToolRow(val group:String,val title:String,val detail:String,val action:()->Unit)
    private fun dp(value:Int)=(value*resources.displayMetrics.density).toInt()

    // Resolve the linked theme attribute without referencing a dependency's generated resource class.
    // This also respects palette and dynamic theme overlays when present.
    private fun primaryUiColor(view: View): Int {
        val context = view.context
        val fallback = ContextCompat.getColor(context, R.color.brand_primary)
        val attrId = context.resources.getIdentifier("colorPrimary", "attr", context.packageName)
        if (attrId == 0) return fallback
        val value = android.util.TypedValue()
        if (!context.theme.resolveAttribute(attrId, value, true)) return fallback
        return when {
            value.type in android.util.TypedValue.TYPE_FIRST_COLOR_INT..android.util.TypedValue.TYPE_LAST_COLOR_INT -> value.data
            value.resourceId != 0 -> runCatching { ContextCompat.getColor(context, value.resourceId) }.getOrDefault(fallback)
            else -> fallback
        }
    }
    private fun setupNavigation(){
        b.navHome.setOnClickListener{b.navHome.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);home()}
        b.navSearch.setOnClickListener{b.navSearch.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);openSearch()}
        b.navLibrary.setOnClickListener{b.navLibrary.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);openLibrary()}
        b.navPlayer.setOnClickListener{if(controller?.currentMediaItem==null)toast("Play a song first")else nowPlaying(true)}
        b.navSettings.setOnClickListener{openSettings()}
        b.btnSearchGo.setOnClickListener{doSearch(b.search.text.toString());(getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(b.search.windowToken,0)}
        b.btnSectionMore.setOnClickListener{when(page){"Home"->discoverMenu();"Search"->suggestions();"Library"->libraryMenu();else->toolsMenu()}}
    }
    private fun selectPage(next:String){
        clearPlaylistGreeting()
        cancelSearch()
        b.suggestScroll.visibility=View.GONE
        restoreTabScroll=next!=page
        if(restoreTabScroll&&b.toolsScroll.visibility==View.VISIBLE)tabScroll[page]=b.toolsScroll.scrollY
        page=next
        if(next!="Library")libraryGroup=null
        if(next!="Settings")settingsGroup=null
        b.glassContent.background=if(next=="Library"||next=="Settings") com.abhi.ganasuno.ui.GlassFeatureStyle.ambient(this) else null
        if(next!="Search"){currentFocus?.let{(getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(it.windowToken,0)};b.search.clearFocus()}
        glassDock.tabs(listOf(b.navHome,b.navSearch,b.navLibrary),listOf("Home","Search","Library").indexOf(next))
        b.searchBox.visibility=if(next=="Search")View.VISIBLE else View.GONE
        b.chipScroll.visibility=if(next=="Home"||next=="Search")View.VISIBLE else View.GONE
        b.btnSectionMore.visibility=if(next=="Home")View.VISIBLE else View.GONE
        b.sectionSubtitle.visibility=if(next=="Search")View.GONE else View.VISIBLE
        b.sectionSubtitle.text=when(next){"Home"->"Your music, your way";"Search"->"Songs, artists and videos";"Library"->"Your music, organized in one place.";else->"Playback, appearance, integrations and privacy"}
    }
    private fun showSongContent(){stopHomeGreeting();adapter.gridMode=page=="Library"&&libraryGrid;b.list.layoutManager=if(adapter.gridMode) androidx.recyclerview.widget.GridLayoutManager(this,2) else androidx.recyclerview.widget.LinearLayoutManager(this);b.toolsScroll.visibility=View.GONE;b.list.visibility=View.VISIBLE;if(page=="Library")b.btnSectionMore.visibility=View.VISIBLE;if(page=="Search"){b.chipScroll.visibility=View.VISIBLE;b.sectionSubtitle.visibility=View.VISIBLE}}
    private fun showToolsContent(){clearPlaylistGreeting();settingCards.clear();++requestSerial;b.progress.visibility=View.GONE;b.empty.visibility=View.GONE;b.actions.visibility=View.GONE;b.list.visibility=View.GONE;b.toolsScroll.visibility=View.VISIBLE;b.toolsContent.removeAllViews();b.toolsScroll.scrollTo(0,0);if(restoreTabScroll){val target=page;val y=tabScroll[target]?:0;b.toolsScroll.post{if(page==target&&b.toolsScroll.visibility==View.VISIBLE)b.toolsScroll.scrollTo(0,y)}};restoreTabScroll=false}
    private fun openSearch(){
        selectPage("Search");showToolsContent();b.sectionTitle.text="Search";b.search.setText("");b.search.hint="Artists, Albums, Playlists, and More";b.search.clearFocus();b.root.isFocusableInTouchMode=true;b.root.requestFocus();(getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(b.search.windowToken,0);b.chipScroll.visibility=View.GONE
        val saved=(LibraryStore.list("favorites")+LibraryStore.list("local")+LibraryStore.history().asReversed().map{it.song}).distinctBy{it.id}
        val browse = com.abhi.ganasuno.ui.SearchBrowseView(this,b.toolsContent)
        browse.render(LibraryStore.frequentSearches(),saved,{query->doSearch(query,browse.categoryName(query))},{searchHistory()})
        renderRows(listOf(ToolRow("Browse by type","Videos","Search music videos"){ask("Search videos"){q->runSearch(R.string.no_results,video=true){YtApi.searchVideos(q)}}},ToolRow("More ways to explore","Suggestions","Saved titles and local suggestions"){suggestions()},ToolRow("More ways to explore","Online catalogue","Public videos, channels and playlists"){publicCatalogue.show()},ToolRow("More ways to explore","Songs, artists and albums","MusicBrainz metadata"){musicCatalogue.show(null)},ToolRow("More ways to explore","Live stations","Search available live streams"){doLive()},ToolRow("More ways to explore","Moods and genres","Discover music by mood"){discoverMenu()}),false)
    }
    private fun openLibrary(){
        libraryGroup=null
        selectPage("Library");showToolsContent();b.sectionTitle.text="Library";b.root.isFocusableInTouchMode=true;b.root.requestFocus()
        libraryGrid=LibraryStore.option("ui_grid")==1
        val rows=listOf(
            ToolRow("Library","Collection","Favourites, artists, albums and podcasts"){openLibraryGroup("Collection")},
            ToolRow("Library","Playlists","${LibraryStore.names().size} playlists · create, import and share"){openLibraryGroup("Playlists")},
            ToolRow("Library","Listening","History, most played and statistics"){openLibraryGroup("Listening")},
            ToolRow("Library","Storage","Offline audio, downloads and backups"){openLibraryGroup("Storage")}
        )
        renderRows(rows,false,showHeadings=false)
    }
    private fun openLibraryGroup(group:String){
        libraryGroup=group
        selectPage("Library");showToolsContent();b.sectionTitle.text=group;b.sectionSubtitle.text="Library · $group";b.root.isFocusableInTouchMode=true;b.root.requestFocus()
        val back=Button(this).apply{text="‹ Back to Library";isAllCaps=false;minimumHeight=dp(48);setOnClickListener{openLibrary()}}
        b.toolsContent.addView(back,LinearLayout.LayoutParams(-1,-2).apply{marginStart=dp(16);marginEnd=dp(16);bottomMargin=dp(8)})
        val rows=when(group){
            "Collection"->listOf(
            ToolRow("Collection","Favourites","${LibraryStore.list("favorites").size} saved songs"){showLocalList(LibraryStore.list("favorites"),"Favourites")},
            ToolRow("Collection","Artists and Albums","Saved artists, albums and collection search"){localCatalogue()},
            ToolRow("Collection","Favourite Collections","Saved albums, playlists and podcasts"){favouriteCollections()},
            ToolRow("Collection","Followed Artists","Manage followed artists"){advanced.artists()},
            ToolRow("Collection","Podcasts","Public RSS feeds and episodes"){podcastMenu()},
            ToolRow("Collection","Search Library","Find songs and artists in your music"){ask("Search your library"){q->showLocalList(catalogueTracks().filter{it.title.contains(q,true)||it.artist.contains(q,true)},"Library results")}},
            ToolRow("Collection","Manage library","List / grid view, sort and bulk actions"){libraryMenu()}
            )
            "Playlists"->listOf(
                ToolRow(group,"Create or manage playlists","${LibraryStore.names().size} playlists"){playlistsMenu()},
                ToolRow(group,"Import playlist links","Import a supported playlist"){advanced.menuAction(3)},
                ToolRow(group,"Export playlist links","Export saved playlists"){advanced.menuAction(4)},
                ToolRow(group,"Shareable playlist links","Create and manage shareable links"){sharedPlaylists().show()},
                ToolRow(group,"Saved YouTube playlists","Manage saved online playlists"){v8MenuAction(6)},
                ToolRow(group,"Share a saved online playlist link","Share an existing online link"){playlistLinkMenu()}
            )
            "Listening"->listOf(
                ToolRow(group,"Recently Played","Your listening history"){showLocalList(LibraryStore.history().asReversed().map{it.song}.distinctBy{it.id},"Recently played")},
                ToolRow(group,"Most Played","Rank by play count or listening time"){choose("Most played",listOf("Ranked songs","Play counts","Listening time")){n->when(n){0->showLocalList(LibraryStore.mostPlayed().map{it.first},"Most played");1->mostPlayedMenu();2->showLocalList(LibraryStore.history().groupBy{it.song.id}.values.sortedByDescending{g->g.sumOf{it.ms}}.map{it.first().song},"Most listened")}}},
                ToolRow(group,"Listening analytics","7, 30, 90 days, all time or custom dates"){choose("Listening analytics",listOf("7 days","30 days / fingerprint","90 days","All time","This year","Custom date range")){n->when(n){4->{val start=java.time.LocalDate.now().withDayOfYear(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();showInsights(LibraryStore.history().filter{it.at>=start},emptyList(),0,"This year")};5->dateInsights();else->analytics(intArrayOf(7,30,90,0)[n])}}},
                ToolRow(group,"Listening recap","Monthly playlist and yearly Wrapped"){choose("Listening recap",listOf("Monthly recap playlist","Yearly Wrapped")){n->if(n==0)monthlyRecap()else wrapped()}},
                ToolRow(group,"Music by decade","Browse your listening by decade"){decadeMenu()},
                ToolRow(group,"Clear listening history","Remove recorded listening history"){MaterialAlertDialogBuilder(this).setTitle("Clear listening history?").setNegativeButton("Cancel",null).setPositiveButton("Clear"){_,_->LibraryStore.clearHistory();toast("History cleared")}.show()}
            )
            "Storage"->listOf(
                ToolRow(group,"Offline Audio","${LibraryStore.list("local").size} saved or imported tracks · import and downloads"){offlineMenu()},
                ToolRow(group,"Backup and restore","Library, settings, audio archives and automatic backups"){backupHub()}
            )
            else->emptyList()
        }
        renderRows(rows,true,showHeadings=false)
    }
    private val settingCards=mutableListOf<Pair<String,com.abhi.ganasuno.ui.GlassFeatureCard>>()
    private fun refreshSettingIndicators(){
        if(page!="Settings"||b.toolsScroll.visibility!=View.VISIBLE)return
        settingCards.forEach{(title,card)->card.setCurrentStatus(com.abhi.ganasuno.ui.SmartSettingIndicators.summary(title,controller))}
    }
    private fun renderRows(rows:List<ToolRow>,searchable:Boolean,showHeadings:Boolean=true,searchRows:List<ToolRow> = rows){
        val target=b.toolsContent
        val glassRows=page=="Library"||page=="Settings"||page=="Search"
        val reduced=LibraryStore.option("glass_reduced")==1
        val results=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        fun render(query:String){
            results.removeAllViews()
            settingCards.clear()
            val terms=query.trim().split(Regex("\\s+")).filter{it.isNotBlank()}
            val candidates=if(terms.isEmpty())rows else searchRows
            val matches=candidates.filter{row->terms.all{term->("${row.group} ${row.title} ${row.detail}").contains(term,true)}}
            if(matches.isEmpty())results.addView(TextView(this).apply{text="No matching controls";setPadding(dp(16),dp(20),dp(16),dp(20))})
            matches.groupBy{it.group}.forEach{(group,items)->
                if(showHeadings || (terms.isNotEmpty() && searchRows !== rows))results.addView(TextView(this).apply{text=group.uppercase(java.util.Locale.ROOT);textSize=12f;setTypeface(null,android.graphics.Typeface.BOLD);if(glassRows)setTextColor(com.abhi.ganasuno.ui.GlassFeatureStyle.secondary(this@MainActivity)) else setTextColor(primaryUiColor(this));setPadding(dp(16),dp(20),dp(16),dp(8))})
                items.forEach rowLoop@{row->
                    if(glassRows){
                        val card=com.abhi.ganasuno.ui.GlassFeatureCard(this,row.group,row.title,row.detail,reduced,row.action)
                        if(page=="Settings"){settingCards.add(row.title to card);card.setCurrentStatus(com.abhi.ganasuno.ui.SmartSettingIndicators.summary(row.title,controller))}
                        results.addView(card,LinearLayout.LayoutParams(-1,-2).apply{marginStart=dp(16);marginEnd=dp(16);bottomMargin=dp(10)})
                        return@rowLoop
                    }
                    val item=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(12));minimumHeight=dp(64);isClickable=true;isFocusable=true;val a=android.util.TypedValue();if(context.theme.resolveAttribute(android.R.attr.selectableItemBackground,a,true))setBackgroundResource(a.resourceId);setOnClickListener{row.action()}}
                    item.addView(TextView(this).apply{text=row.title+"  ›";textSize=16f;setTypeface(null,android.graphics.Typeface.BOLD)})
                    item.addView(TextView(this).apply{text=row.detail;textSize=13f;setTextColor(com.google.android.material.color.MaterialColors.getColor(this,com.google.android.material.R.attr.colorOnSurfaceVariant))})
                    results.addView(item,LinearLayout.LayoutParams(-1,-2))
                    results.addView(View(this).apply{setBackgroundColor(com.google.android.material.color.MaterialColors.getColor(this,com.google.android.material.R.attr.colorOutlineVariant))},LinearLayout.LayoutParams(-1,dp(1)).apply{marginStart=dp(16);marginEnd=dp(16)})
                }
            }
        }
        if(searchable){
            val input=EditText(this).apply{
                hint=if(page=="Library") "Find a library option" else "Find a setting, e.g. crossfade"
                setSingleLine(true);minimumHeight=dp(48);setPadding(dp(16),dp(12),dp(16),dp(12))
                if(glassRows){background=com.abhi.ganasuno.ui.GlassFeatureStyle.surface(this@MainActivity,28);setTextColor(com.abhi.ganasuno.ui.GlassFeatureStyle.primary(this@MainActivity));setHintTextColor(com.abhi.ganasuno.ui.GlassFeatureStyle.secondary(this@MainActivity))}
                imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            }
            target.addView(input,LinearLayout.LayoutParams(-1,-2).apply{if(glassRows){marginStart=dp(16);marginEnd=dp(16);topMargin=dp(4);bottomMargin=dp(4)}})
            input.addTextChangedListener(object:android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){};override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){render(s.toString())};override fun afterTextChanged(s:android.text.Editable?){}})
        }
        target.addView(results);render("")
    }
    private data class SettingsSection(val title:String,val detail:String,val groups:Set<String>)
    private val settingsSections = listOf(
        SettingsSection("Appearance","Theme, glass, artwork, player style and lyrics",setOf("Appearance","Player & appearance","Lyrics")),
        SettingsSection("Playback","Quality, queue controls and transitions",setOf("Playback")),
        SettingsSection("Sound & Effects","8D audio, equalizers, reverb and output",setOf("Sound")),
        SettingsSection("Discovery","Recommendations, metadata and search history",setOf("Discovery","Listening insights")),
        SettingsSection("Storage","Playback, artwork and waveform caches",setOf("Storage")),
        SettingsSection("Account & Integrations","Accounts, shared listening, community and AI",setOf("Integrations")),
        SettingsSection("System & Privacy","Diagnostics, updates, privacy and support",setOf("System & privacy"))
    )
    private fun openSettings(){
        settingsGroup=null
        selectPage("Settings");showToolsContent();b.sectionTitle.text="Settings";b.root.isFocusableInTouchMode=true;b.root.requestFocus()
        val all=settingsRows()
        val rows=settingsSections.map{section->ToolRow("Settings",section.title,section.detail){openSettingsGroup(section.title)}}
        renderRows(rows,true,showHeadings=false,searchRows=all)
    }
    private fun openSettingsGroup(group:String){
        val section=settingsSections.firstOrNull{it.title==group}?:return openSettings()
        settingsGroup=group
        selectPage("Settings");showToolsContent();b.sectionTitle.text=section.title;b.sectionSubtitle.text="Settings · ${section.title}"
        b.root.isFocusableInTouchMode=true;b.root.requestFocus()
        val back=Button(this).apply{text="‹ Back to Settings";isAllCaps=false;minimumHeight=dp(48);background=com.abhi.ganasuno.ui.GlassFeatureStyle.surface(this@MainActivity,20);setTextColor(com.abhi.ganasuno.ui.GlassFeatureStyle.primary(this@MainActivity));setOnClickListener{openSettings()}}
        b.toolsContent.addView(back,LinearLayout.LayoutParams(-1,-2).apply{marginStart=dp(16);marginEnd=dp(16);bottomMargin=dp(8)})
        val rows=settingsRows().filter{it.group in section.groups}
        renderRows(rows,true,showHeadings=false)
    }
    private fun settingsRows():List<ToolRow>{
        return listOf(
            ToolRow("Appearance","Liquid Glass effects","Choose backdrop sampling or lightweight glass"){choose("Liquid Glass",listOf("Full effects","Reduced effects")){n->LibraryStore.option("glass_reduced",n,true);glassDock.configure(n==1);toast("Glass settings saved")}},
            ToolRow("Playback","Audio & video quality","Bitrate, audio language, video resolution, silence skipping, normalization, sleep timer"){SettingsDialog.show(this)},
            ToolRow("Playback","Playback controls","Repeat, shuffle, speed, pitch, queue, endless mix, preloading"){playbackControls()},
            ToolRow("Playback","Crossfade & DJ transitions","Crossfade duration, album continuity, filter sweeps · experimental"){transitionMenu()},
            ToolRow("Playback","Manual fade","Fade in, fade out and duration"){fadeMenu()},
            ToolRow("Sound","Audio output","Preferred speaker, wired or Bluetooth route"){outputPicker()},
            ToolRow("Playback","Explicit content","Skip labelled tracks"){v8MenuAction(1)},
            ToolRow("Playback","320 kbps preference","Preferred bitrate and authorized direct audio"){v8MenuAction(0)},
            ToolRow("Playback","Video playback mode","Audio only or video player"){batch14MenuAction(0)},
            ToolRow("Sound","Equalizer","Device equalizer and presets"){com.abhi.ganasuno.ui.EqualizerDialog.show(this)},
            ToolRow("Sound","10-band software EQ","Preamp, custom curve and headphone profiles"){com.abhi.ganasuno.ui.TenBandDialog.show(this)},
            ToolRow("Sound","Stereo rotation & delay","8D-style audio and delay presets"){dspMenu()},
            ToolRow("Sound","Reverb and preamp","Room, Hall, Plate, Cathedral and gain"){advanced.menuAction(0)},
            ToolRow("Sound","System equalizer","Android audio effect control panel"){toolsMenuAction(9)},
            ToolRow("Sound","Track waveform and music tags","BPM, key, scale, notes and reactions"){moreFeaturesAction(10)},
            ToolRow("Player & appearance","Player style","Classic, expressive, mesh or waveform"){advanced.menuAction(1)},
            ToolRow("Player & appearance","Now playing","Open fullscreen player"){if(controller?.currentMediaItem==null)toast("Play a song first")else nowPlaying(true)},
            ToolRow("Player & appearance","Artwork queue","Up next and playback order"){artworkQueue()},
            ToolRow("Player & appearance","Animated artwork","Rotation, local GIF / WebP"){v8MenuAction(3)},
            ToolRow("Player & appearance","Download animated artwork","Authorized HTTPS image for current track"){batch22MenuAction(4)},
            ToolRow("Player & appearance","Theme","System, light and dark"){toolsMenuAction(3)},
            ToolRow("Player & appearance","Theme colours","Wallpaper, colour presets and custom colour"){moreFeaturesAction(0)},
            ToolRow("Player & appearance","Language and RTL","App language and layout direction"){v8MenuAction(8)},
            ToolRow("Lyrics","Current song lyrics","Synced lyrics, local LRC, providers, translation, voting and sharing"){controller?.currentMediaItem?.let{lyricsDialog(fromMediaItem(it))}?:toast("Play a song first")},
            ToolRow("Discovery","Curated playlists","Search-based collection discovery"){v8MenuAction(4)},
            ToolRow("Discovery","Related artists","Local history or Last.fm"){v8MenuAction(5)},
            ToolRow("Discovery","Country & home feed","Content country, source and fetch limit"){toolsMenuAction(8)},
            ToolRow("Discovery","Current track metadata","MusicBrainz artist, album and release metadata"){moreFeaturesAction(16)},
            ToolRow("Discovery","Curator channels","Browse saved channels and playlists"){batch22MenuAction(2)},
            ToolRow("Discovery","Alternative song versions","Search playable alternatives"){batch22MenuAction(0)},
            ToolRow("Storage","Playback cache","Enable, clear and size limit"){mediaCacheMenu()},
            ToolRow("Storage","Thumbnail & artwork cache","Storage overview and clear selected caches"){thumbnailStorage()},
            ToolRow("Storage","Downloaded animated artwork cache","Inspect or clear downloaded images"){batch22MenuAction(5)},
            ToolRow("Storage","Waveform cache","Clear cached waveform summaries"){batch15MenuAction(4)},
            ToolRow("Listening insights","Search history","Review or clear search history"){searchHistory()},
            ToolRow("Integrations","Listen Together","Private room and host controls · server required"){rooms.open()},
            ToolRow("Integrations","Community & charts","SponsorBlock, comments, dislike estimates, regional lists"){moreFeaturesAction(18)},
            ToolRow("Integrations","YouTube account & blog","Playlist sync, ratings, liked videos, blog alerts"){moreFeaturesAction(19)},
            ToolRow("Integrations","Artist subscriptions","Official YouTube subscription controls"){batch22MenuAction(3)},
            ToolRow("Integrations","AI playlist suggestions","Provider key required"){advanced.menuAction(5)},
            ToolRow("Integrations","AI listening recap","Review data before sending to AI provider"){advanced.menuAction(6)},
            ToolRow("System & privacy","Stop service on exit","Choose whether playback continues"){advanced.menuAction(8)},
            ToolRow("System & privacy","System sound settings","Open Android settings"){advanced.menuAction(9)},
            ToolRow("System & privacy","Diagnostics","Live logs, filter and export"){diagnostics()},
            ToolRow("Playback","Playback troubleshooting","Retry, inspect errors or import local audio"){showPlaybackHelp()},
            ToolRow("System & privacy","Developer diagnostics","Build, audio session and crash report"){moreFeaturesAction(5)},
            ToolRow("System & privacy","Update checks","Manifest, stable/nightly and automatic checks"){advanced.updates()},
            ToolRow("System & privacy","Current playback format","Decoded codec, bitrate and channels"){currentFormat()},
            ToolRow("System & privacy","Combined song information","Public metadata and stream format"){batch22MenuAction(1)},
            ToolRow("System & privacy","Look up release year","MusicBrainz recording metadata"){batch14MenuAction(2)},
            ToolRow("System & privacy","About & privacy","Data handling and external services"){toolsMenuAction(7)},
            ToolRow("System & privacy","Release notes and support","Documentation and dependency information"){releaseNotes()},
            ToolRow("System & privacy","Developer information","Package, playback engine and dependencies"){advanced.menuAction(7)})
    }

    // ------------------------------------------------------------------ search

    private fun showResults(list: List<Song>) {
        songs = list
        adapter.submit(list)
        b.actions.visibility = if (list.isEmpty()||b.list.visibility!=View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun doSearch(raw: String, source: String? = null) {
        val query = raw.trim()
        if (query.isEmpty()) return
        if(b.search.text.toString()!=query)b.search.setText(query)
        cancelSearch()
        currentPlaylist = null
        LibraryStore.query(query)
        runSearch(R.string.no_results, source=source ?: com.abhi.ganasuno.ui.SearchBrowseView(this,b.toolsContent).categoryName(query)) { YtApi.search(query) }
    }

    private fun doLive() {
        runSearch(R.string.no_live) { YtApi.searchLive() }
    }

    private fun runSearch(
        emptyText: Int,
        video:Boolean=false,
        source:String?=null,
        task: () -> List<Song>,
    ) {
        selectPage("Search")
        showSongContent()
        displayedSource = source
        b.sectionTitle.text = if (video) "Video results" else "Search results"
        val serial = ++requestSerial
        videoResults=video
        currentPlaylist = null
        showResults(emptyList())
        b.progress.visibility = View.VISIBLE
        b.empty.visibility = View.GONE
        io.execute {
            val result = runCatching { task() }
            ui.post {
                if (isDestroyed || serial != requestSerial) return@post
                b.progress.visibility = View.GONE
                result
                    .onSuccess { list ->
                        showResults(list)
                        if (list.isEmpty()) {
                            b.empty.setText(emptyText)
                            b.empty.visibility = View.VISIBLE
                        }
                    }.onFailure { e ->
                        b.empty.text = getString(R.string.error_network, e.message ?: "")
                        b.empty.visibility = View.VISIBLE
                    }
            }
        }
    }

    // ---------------------------------------------------------------- playback

    private fun toMediaItem(s: Song, source: String? = null): MediaItem {
        val extras = Bundle()
        extras.putBoolean(EXTRA_LIVE, s.live)
        extras.putString("localUri", s.localUri)
        source?.let { extras.putString(com.abhi.ganasuno.ui.PlaylistGreeting.SOURCE_KEY, it) }
        val builder =
            MediaItem
                .Builder()
                .setMediaId(s.id)
                .setUri(s.localUri.ifBlank { "${PlaybackService.SCHEME}://play/${s.id}" })
                .setMediaMetadata(
                    MediaMetadata
                        .Builder()
                        .setTitle(s.title)
                        .setArtist(s.artist)
                        .setAlbumTitle(s.album.takeIf{it.isNotBlank()})
                        .setArtworkUri(Uri.parse(s.thumb))
                        .setExtras(extras)
                        .build(),
                )
        // a live broadcast is an HLS playlist, so ExoPlayer must be told up front
        if (s.live) builder.setMimeType(MimeTypes.APPLICATION_M3U8)
        return builder.build()
    }

    private fun fromMediaItem(item: MediaItem): Song {
        val md = item.mediaMetadata
        return Song(
            id = item.mediaId,
            title = md.title?.toString().orEmpty(),
            artist = md.artist?.toString().orEmpty(),
            duration = "",
            thumb = md.artworkUri?.toString().orEmpty(),
            live = md.extras?.getBoolean(EXTRA_LIVE, false) == true,
            localUri = md.extras?.getString("localUri").orEmpty(),
            album = md.albumTitle?.toString().orEmpty(),
        )
    }

    private fun startQueue(
        list: List<Song>,
        index: Int,
        source: String? = null,
    ) {
        val c = controller
        if (c == null) {
            Toast.makeText(this, R.string.player_not_ready, Toast.LENGTH_SHORT).show()
            return
        }
        if (list.isEmpty()) return
        val requested=list[index.coerceIn(0,list.lastIndex)]
        val permitted=list.filter{LibraryStore.allowed(it.id)}
        if(permitted.isEmpty()||!LibraryStore.allowed(requested.id)){toast("Explicit track skipped");return}
        c.setMediaItems(permitted.map { toMediaItem(it, source) }, permitted.indexOfFirst{it.id==requested.id}.coerceAtLeast(0), 0L)
        c.prepare()
        c.play()
        refreshPlaylistGreeting()
    }

    /** Remember queue + position so the app resumes where you left off. */
    private fun saveQueue() {
        val c = controller ?: return
        val count = c.mediaItemCount
        if (count == 0) { com.abhi.ganasuno.data.Prefs.queueJson = null; return }
        val list = (0 until count).map { fromMediaItem(c.getMediaItemAt(it)) }
        QueueStore.save(list, c.currentMediaItemIndex, c.currentPosition, (0 until count).map { playbackSource(c.getMediaItemAt(it)) })
    }

    private fun restoreQueue(c: MediaController) {
        val saved = QueueStore.load() ?: return
        val original=saved.songs[saved.index.coerceIn(0,saved.songs.lastIndex)]
        val permitted=saved.songs.withIndex().filter{LibraryStore.allowed(it.value.id)}
        if(permitted.isEmpty())return
        val restored=permitted.indexOfFirst{it.value.id==original.id}
        c.setMediaItems(permitted.map { toMediaItem(it.value, saved.sources.getOrNull(it.index)) },restored.coerceAtLeast(0),if(restored<0)0L else saved.positionMs)
        c.prepare() // ready to resume, but not auto-playing
    }

    private fun playVideo(song:Song){if(!LibraryStore.allowed(song.id)){toast("Explicit track skipped");return};if(song.localUri.isNotBlank()||!song.id.matches(Regex("[A-Za-z0-9_-]{11}"))){toast("Online video unavailable");return};controller?.pause();startActivity(Intent(this,VideoActivity::class.java).putExtra(VideoActivity.EXTRA_ID,song.id).putExtra(VideoActivity.EXTRA_TITLE,song.title))}

    private fun openVideo() {
        val c = controller ?: return
        val item = c.currentMediaItem ?: return
        if (fromMediaItem(item).localUri.isNotBlank()) { toast("Video is not available for local audio"); return }
        c.pause()
        startActivity(
            Intent(this, VideoActivity::class.java)
                .putExtra(VideoActivity.EXTRA_ID, item.mediaId)
                .putExtra(VideoActivity.EXTRA_TITLE, item.mediaMetadata.title?.toString().orEmpty()),
        )
    }

    private var lastSkipAt = -1000L
    private fun skipTrack(forward:Boolean){
        val c=controller?:return toast("Player not ready")
        if(c.mediaItemCount==0)return toast("Select a track first")
        val now=android.os.SystemClock.elapsedRealtime()
        if(now-lastSkipAt<350L)return
        val command=if(forward)Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM else Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
        val decision=com.abhi.ganasuno.player.TransportPolicy.skip(forward,c.hasNextMediaItem(),c.hasPreviousMediaItem(),c.playbackState==Player.STATE_IDLE,c.playbackState==Player.STATE_ENDED,c.playerError!=null)
        if (!decision.seek) {
            val refill = com.abhi.ganasuno.player.PlayerHub.extendQueueForNext
            if (forward && refill != null) {
                lastSkipAt = now
                refill { advanced ->
                    if (!isDestroyed && controller === c) {
                        if (advanced) syncMini() else toast("End of queue")
                    }
                }
            } else toast(if (forward) "End of queue" else "First track in queue")
            return
        }
        if(!c.isCommandAvailable(command))return toast("Track switching unavailable")
        lastSkipAt=now
        com.abhi.ganasuno.player.PlayerHub.cancelTransition?.invoke()
        if(forward)c.seekToNextMediaItem()else c.seekToPreviousMediaItem()
        if(decision.prepare || c.playbackState==Player.STATE_IDLE)c.prepare()
        c.play()
        syncMini()
    }
    private fun togglePlayback(c: MediaController){
        when(com.abhi.ganasuno.player.TransportPolicy.toggle(c.mediaItemCount>0,c.playWhenReady,c.playbackState==Player.STATE_IDLE,c.playbackState==Player.STATE_ENDED,c.playerError!=null)){
            com.abhi.ganasuno.player.TransportPolicy.Action.EMPTY->toast("Select a track first")
            com.abhi.ganasuno.player.TransportPolicy.Action.ERROR->showPlaybackHelp()
            com.abhi.ganasuno.player.TransportPolicy.Action.PAUSE->c.pause()
            com.abhi.ganasuno.player.TransportPolicy.Action.RESTART->{c.seekToDefaultPosition();c.play()}
            com.abhi.ganasuno.player.TransportPolicy.Action.PREPARE->{c.prepare();c.play()}
            com.abhi.ganasuno.player.TransportPolicy.Action.PLAY->c.play()
        }
    }
    private fun showPlaybackHelp(){
        val c=controller?:return toast("The player is not ready")
        val item=c.currentMediaItem?:return toast("Select a track first")
        val id=item.mediaId
        val failure=com.abhi.ganasuno.player.PlaybackFailures.last?.takeIf{it.item==id}
            ?:c.playerError?.let{com.abhi.ganasuno.player.PlaybackFailures.inspect(id,it)}
        val online=id.matches(Regex("[A-Za-z0-9_-]{11}")) && item.mediaMetadata.extras?.getString("localUri").isNullOrBlank()
        val actions=mutableListOf("Import audio from device","View diagnostics","Save and play authorized audio URL")
        if(online)actions.add("Open on YouTube")
        MaterialAlertDialogBuilder(this).setTitle("Playback unavailable")
            .setMessage((failure?.summary()?:"No current playback error.")+"\n\nSearch results do not guarantee that a playable stream is available. Your queue has been preserved.")
            .setPositiveButton("Retry"){_,_->if(c.currentMediaItem?.mediaId==id){if(online)com.abhi.ganasuno.data.Streams.invalidate(id);c.prepare();c.play()}}
            .setNeutralButton("More options"){_,_->choose("Playback options",actions){n->when(n){
                0->importAudio.launch(arrayOf("audio/*"))
                1->diagnostics()
                2->ask("Authorized direct HTTPS audio URL"){raw->offlineTask(true){token->com.abhi.ganasuno.data.OfflineFiles.fetch(this,raw,token)}}
                3->openHttps("https://youtube.com/watch?v=$id")
            }}}
            .setNegativeButton("Close",null).show()
    }

    private fun closeMini(){controller?.let{it.pause();it.clearMediaItems();saveQueue();syncMini()}}
    private fun syncMini() {
        val c = controller ?: run { b.miniBuffering.visibility = View.GONE; return }
        val item = c.currentMediaItem
        refreshPlaylistGreeting()
        if (item == null) {
            b.miniBuffering.visibility = View.GONE
            b.mini.visibility = View.GONE
            dockArtwork=null
            b.miniThumb.tag=null
            glassDock.artwork(null);glassDock.refresh()
            adapter.playingId = null
            return
        }
        b.mini.visibility = View.VISIBLE
        val md = item.mediaMetadata
        b.miniTitle.text = md.title ?: ""
        b.miniArtist.text = md.artist?.toString()?.takeIf{it.isNotBlank()} ?: "Now Playing"
        updateDock(md.artworkUri?.toString().orEmpty())
        val state = MiniPlaybackUi.resolve(
            hasItem = true,
            playWhenReady = c.playWhenReady,
            buffering = c.playbackState == Player.STATE_BUFFERING,
            isPlaying = c.isPlaying || (c.playWhenReady && c.playbackState==Player.STATE_READY),
            hasError = c.playerError != null,
        )
        b.miniPlay.contentDescription=if(state.showPause)"Pause" else "Play"
        val previousIcon=b.miniPlay.tag as? Boolean
        if(previousIcon!=state.showPause){
            b.miniPlay.tag=state.showPause
            b.miniPlay.setImageResource(if (state.showPause) R.drawable.ic_pause else R.drawable.ic_play)
            if(previousIcon!=null&&android.animation.ValueAnimator.areAnimatorsEnabled()&&LibraryStore.option("glass_reduced")==0){b.miniPlay.animate().cancel();b.miniPlay.scaleX=.88f;b.miniPlay.scaleY=.88f;b.miniPlay.animate().scaleX(1f).scaleY(1f).setDuration(160).start()}
        }
        b.miniBuffering.visibility = if (state.showLoading) View.VISIBLE else View.GONE
        glassDock.refresh()
        adapter.playingId = item.mediaId
    }

    private fun updateProgress() {
        val c = controller ?: return
        val duration = c.duration
        if (duration > 0 && duration != C.TIME_UNSET) {
            b.seek.max = (duration / 1000).toInt()
            if (!userSeeking) b.seek.progress = (c.currentPosition / 1000).toInt()
        } else if (!userSeeking) {
            b.seek.progress = 0
        }
    }

    private fun toast(text:String) = Toast.makeText(this,text,Toast.LENGTH_SHORT).show()
    private fun formatTime(ms:Long):String { val sec=ms.coerceAtLeast(0L)/1000;return "%d:%02d".format(sec/60,sec%60) }
    private fun featureDialog(): MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(this).apply {
        if(page=="Library"||page=="Settings")setBackground(com.abhi.ganasuno.ui.GlassFeatureStyle.dialog(this@MainActivity))
    }
    private fun choose(title:String,labels:List<String>, action:(Int)->Unit) {
        if(labels.isEmpty()){toast("Nothing to show yet");return}
        if(com.abhi.ganasuno.ui.SmartSettingIndicators.show(featureDialog(),this,title,labels,com.abhi.ganasuno.ui.SmartSettingIndicators.selection(this,title,controller),action))return
        featureDialog().setTitle(title).setItems(labels.toTypedArray()){_,i->action(i)}.setNegativeButton("Close",null).show().window?.decorView?.layoutDirection=b.root.layoutDirection
    }
    private fun ask(title:String,initial:String="", action:(String)->Unit) {
        val field=EditText(this).apply{setText(initial);setSingleLine(false)}
        featureDialog().setTitle(title).setView(field).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->action(field.text.toString())}.show()
    }
    private fun home() {
        selectPage("Home");showToolsContent();b.chipScroll.visibility=View.GONE
        b.sectionSubtitle.text="Listen Now · GANASUNO"
        homeGreetingActive = true
        refreshPlaylistGreeting()
        val history=LibraryStore.history();val fav=LibraryStore.list("favorites");val pool=localPool()
        val premium=com.abhi.ganasuno.ui.PremiumUi(this,b.toolsContent)
        fun shelf(title:String,tracks:List<Song>){premium.shelf(title,tracks.filter{LibraryStore.allowed(it.id)},{showLocalList(tracks,title)},{track->startQueue(tracks.filter{LibraryStore.allowed(it.id)},tracks.filter{LibraryStore.allowed(it.id)}.indexOfFirst{it.id==track.id}.coerceAtLeast(0),title)})}
        shelf("Recently Played",history.asReversed().map{it.song}.distinctBy{it.id})
        shelf("Quick Picks",com.abhi.ganasuno.data.Batch11.quick(history,fav))
        shelf("Made For You",com.abhi.ganasuno.data.Batch11.mix(history,fav,pool))
        shelf("Your Top Songs",LibraryStore.mostPlayed().map{it.first})
        shelf("Favourite Songs",fav)
        premium.links("Discover New Music",listOf("Daily Mixes" to {dailyCards()},"New Releases" to {doSearch("new music releases", "New Releases")},"Mood-Based Playlists" to {discoverMenu()},"Favourite Artists" to {advanced.artists()},"Trending Albums" to {musicCatalogue.show(null)},"Online catalogue" to {publicCatalogue.home()}))
        val cm=getSystemService(android.net.ConnectivityManager::class.java)
        val connected=cm?.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true
        if(!connected){premium.message("Offline · Your downloaded music is available in Library.");shelf("Downloaded Music",LibraryStore.list("local"));return}
        if(LibraryStore.option("home_source")==1){shelf("Saved music",pool);return}
        val serial=++requestSerial
        val loading=premium.message("Loading recommendations…")
        io.execute{val result=runCatching{YtApi.search("latest hindi songs",limit=LibraryStore.option("home_limit",30).coerceIn(10,100))};ui.post{
            if(isDestroyed||serial!=requestSerial||page!="Home")return@post
            val tracks=result.getOrNull().orEmpty();loading.text=if(tracks.isEmpty())"Online recommendations unavailable. Try again from Listen Now." else "Discover fresh music"
            shelf("Recommended Songs",tracks)
        }}
    }
    private fun offlineHome(){
        showLocalList(LibraryStore.list("local"),"Offline · Listen to downloaded music")
        if(songs.isEmpty()){b.empty.text="Offline · No downloaded music. Open Library to import audio.";b.empty.visibility=View.VISIBLE}
        else toast("Offline · Listen to downloaded music")
    }
    private fun showLocalList(list:List<Song>,title:String,playlist:String?=null){
        clearPlaylistGreeting();++requestSerial;videoResults=false;currentPlaylist=playlist;b.progress.visibility=View.GONE;if(page=="Settings"||playlist!=null||title in listOf("Favourites","Offline audio","Library results"))selectPage("Library");showSongContent();displayedSource=title;b.sectionTitle.text=title;b.sectionSubtitle.text="${list.size} tracks · tap a song to play";b.search.setText("");b.search.hint="Search songs, artists and videos";showResults(list);b.empty.text="No songs yet";b.empty.visibility=if(list.isEmpty()) View.VISIBLE else View.GONE
    }
    private fun sharedPlaylists()=com.abhi.ganasuno.ui.SharedPlaylists(this,io){tracks,name->showLocalList(tracks,name,name)}
    private fun catalogueTracks()=(LibraryStore.list("favorites")+LibraryStore.list("local")+LibraryStore.names().flatMap{LibraryStore.playlist(it)}+LibraryStore.history().map{it.song}).distinctBy{it.id}
    private fun localCatalogue(){choose("Browse saved music",listOf("Artists","Albums","Search artists and albums")){i->when(i){0->catalogueGroups(false);1->catalogueGroups(true);2->ask("Search saved artists and albums"){q->choose("Search type",listOf("Artists","Albums")){n->catalogueGroups(n==1,q)}}}}}
    private fun catalogueGroups(albums:Boolean,q:String=""){val tracks=catalogueTracks();val groups=Batch15.matching(if(albums)Batch15.albums(tracks,LibraryStore.albumTags())else Batch15.artists(tracks),q);choose(if(albums)"Saved albums" else "Saved artists",groups.map{g->g.title+(if(g.artist.isBlank())"" else " · ${g.artist}")+" · ${g.tracks.size} tracks"}){n->val g=groups[n];if(albums)albumActions(g)else artistPage(g.title,g.tracks)}}
    private fun albumActions(g:Batch15.Group){
        val saved=LibraryStore.savedAlbums().any{com.abhi.ganasuno.data.Batch18.albumKey(it.title,it.artist)==com.abhi.ganasuno.data.Batch18.albumKey(g.title,g.artist)}
        collectionDetails(g.title,g.tracks,actions=listOf("Open album tracks" to {showLocalList(g.tracks,g.title)},(if(saved)"Remove favourite" else "Favourite album") to {runCatching{LibraryStore.toggleSavedAlbum(g.title,g.artist)}.onSuccess{toast(if(it)"Album saved" else "Album removed")}.onFailure{toast("Unable to save album")};Unit}))
    }
    private fun favouriteCollections(){choose("Favourite collections",listOf("Local albums","Local playlists","Online album links","Online playlist links","Podcast feeds")){n->when(n){0->{val saved=LibraryStore.savedAlbums();choose("Favourite local albums",saved.map{it.title+" · "+it.artist}){i->val ref=saved[i];val key=com.abhi.ganasuno.data.Batch18.albumKey(ref.title,ref.artist);val g=Batch15.albums(catalogueTracks(),LibraryStore.albumTags()).firstOrNull{com.abhi.ganasuno.data.Batch18.albumKey(it.title,it.artist)==key};if(g!=null)albumActions(g)else choose(ref.title,listOf("No matching tracks — remove favourite")){LibraryStore.toggleSavedAlbum(ref.title,ref.artist);favouriteCollections()}}};1->{val saved=LibraryStore.savedPlaylists();choose("Favourite local playlists",saved){i->playlistMenu(saved[i])}};2->collectionMenu("Album",false);3->collectionMenu("Playlist",false);4->collectionMenu("Podcast",false)}}}
    private fun artistPage(artist:String,tracks:List<Song> = catalogueTracks().filter{com.abhi.ganasuno.data.Batch13.key(it.artist)==com.abhi.ganasuno.data.Batch13.key(artist)}){
        collectionDetails(artist,tracks,actions=listOf("Saved songs" to {showLocalList(tracks,artist)},"Saved albums" to {val albums=Batch15.albums(tracks,LibraryStore.albumTags());choose("Albums · $artist",albums.map{it.title}){i->albumActions(albums[i])}},"Search online songs" to {doSearch("$artist songs")},"Search online videos" to {runSearch(R.string.no_results,true){YtApi.searchVideos(artist)}},"Related artists" to {relatedArtists(artist)}))
    }
    private fun libraryMenu(){
        choose("Manage library",listOf("List / grid view","Sort visible songs","Bulk actions")){i->when(i){
            0->choose("Library layout",listOf("List","Grid")){libraryGrid=it==1;LibraryStore.option("ui_grid",it,true);toast("Layout saved");openLibrary()}
            1->choose("Sort songs",com.abhi.ganasuno.data.BatchCore.sortLabels){n->val scope=currentPlaylist?.let{"playlist:$it"}?:"";showSongContent();showResults(LibraryStore.ordered(songs,n,scope))}
            2->bulkMenu()
        }}
    }
    private fun playlistHub(){
        choose("Playlists",listOf("Create or manage playlists","Import playlist links","Export playlist links","Shareable playlist links","Saved YouTube playlists","Share a saved online playlist link")){i->when(i){
            0->playlistsMenu();1->advanced.menuAction(3);2->advanced.menuAction(4);3->sharedPlaylists().show();4->v8MenuAction(6);5->playlistLinkMenu()
        }}
    }
    private fun backupHub(){
        choose("Backup and restore",listOf("Export library metadata","Restore library metadata","Automatic local backups","Export library + audio","Restore audio archive","Export settings","Import settings")){i->when(i){
            0->exportLibrary.launch("ganasuno-library.json");1->restoreLibrary.launch(arrayOf("application/json","text/plain"));2->backupMenu();3->moreFeaturesAction(3);4->moreFeaturesAction(4);5->moreFeaturesAction(1);6->moreFeaturesAction(2)
        }}
    }
    private fun playlistsMenu(){val names=LibraryStore.names();choose("Playlists",listOf("+ New playlist")+names){i->if(i==0)ask("Playlist name"){if(LibraryStore.create(it))playlistsMenu()else toast("Invalid or duplicate name")}else playlistMenu(names[i-1])}}
    private fun playlistMenu(name:String){choose(name,listOf("Open playlist","Rename playlist","Delete playlist","Save sorted order","Playlist details","Change cover","Remove cover","Create shareable URL",if(name in LibraryStore.savedPlaylists())"Remove from favourite playlists" else "Add to favourite playlists")){i->when(i){0->showLocalList(LibraryStore.playlist(name),name,name);1->ask("Rename playlist",name){if(LibraryStore.rename(name,it))playlistsMenu()else toast("Invalid or duplicate name")};2->MaterialAlertDialogBuilder(this).setTitle("Delete $name?").setNegativeButton("Cancel",null).setPositiveButton("Delete"){_,_->LibraryStore.delete(name);playlistsMenu()}.show();3->choose("Save playlist order",com.abhi.ganasuno.data.BatchCore.sortLabels){n->LibraryStore.sortPlaylistMode(name,n);showLocalList(LibraryStore.playlist(name),name,name)};4->playlistDetails(name);5->{coverPlaylist=name;coverPicker.launch(arrayOf("image/*"))};6->{LibraryStore.setCover(name,"");playlistDetails(name)};7->sharedPlaylists().create(name);8->runCatching{LibraryStore.toggleSavedPlaylist(name)}.onSuccess{toast(if(it)"Playlist saved" else "Playlist removed")}.onFailure{toast("Playlist not found")}}}}
    private fun collectionDetails(title:String,tracks:List<Song>,customCover:android.graphics.Bitmap?=null,actions:List<Pair<String,()->Unit>>){
        val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val art=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP;background=android.graphics.drawable.GradientDrawable().apply{cornerRadius=dp(18).toFloat();setColor(0xff27272a.toInt())};clipToOutline=true;contentDescription="Collection artwork"}
        body.addView(art,LinearLayout.LayoutParams(dp(220),dp(220)).apply{gravity=android.view.Gravity.CENTER_HORIZONTAL;topMargin=dp(12)})
        if(customCover!=null)art.setImageBitmap(customCover)else ImageLoader.load(tracks.firstOrNull()?.thumb.orEmpty(),art,R.drawable.ic_placeholder)
        val premium=com.abhi.ganasuno.ui.PremiumUi(this,body)
        premium.message("${tracks.size} tracks · ${tracks.map{it.artist}.distinct().size} artists")
        val dialog=MaterialAlertDialogBuilder(this).setTitle(title).setView(ScrollView(this).apply{addView(body)}).setPositiveButton("Close",null).create()
        val playable=tracks.filter{LibraryStore.allowed(it.id)}
        premium.links("Listen",listOf("Play all" to {if(playable.isNotEmpty()){dialog.dismiss();startQueue(playable,0,title)}else toast("No playable tracks")},"Shuffle" to {if(playable.isNotEmpty()){dialog.dismiss();startQueue(Shuffler.smart(playable),0,title)}else toast("No playable tracks")})+actions.map{(label,action)->label to {dialog.dismiss();action()}})
        premium.shelf("Tracks",playable,{dialog.dismiss();showLocalList(tracks,title)},{song->dialog.dismiss();startQueue(playable,playable.indexOfFirst{it.id==song.id}.coerceAtLeast(0),title)})
        dialog.show()
    }
    private fun playlistDetails(name:String){
        val tracks=LibraryStore.playlist(name)
        collectionDetails(name,tracks,com.abhi.ganasuno.ui.PlaylistCover.fromStored(LibraryStore.cover(name)),listOf("Open tracks" to {showLocalList(tracks,name,name)},"Manage playlist" to {playlistMenu(name)},"Share playlist" to {sharedPlaylists().create(name)}))
    }
    private fun addPlaylist(song:Song){val names=LibraryStore.names();choose("Add to playlist",listOf("+ New playlist")+names){i->if(i==0)ask("Playlist name"){if(LibraryStore.create(it)){LibraryStore.add(it.trim(),song);toast("Added")}else toast("Invalid or duplicate name")}else{LibraryStore.add(names[i-1],song);toast("Added")}}}
    private fun songActions(song:Song,fromPlayer:Boolean=false){
        val labels=mutableListOf("Play next","Add to queue","Toggle favourite","Add to playlist","Share","Lyrics","Start artist mix","Song details","Follow / unfollow artist","Private song note","Set release year","Set album name","Mark / unmark explicit","Related artists locally","Artist page")
        val playlist=currentPlaylist
        if(playlist!=null)labels.addAll(listOf("Remove from playlist","Move up in playlist","Move down in playlist"))
        if(song.localUri.isNotBlank()) { labels.add("Analyze audio and waveform"); labels.add("Remove from offline library"); if(!com.abhi.ganasuno.data.OfflineFiles.owned(this,song))labels.add("Save offline copy") }
        if(song.localUri.isBlank()) { labels.add("Other versions"); labels.add("More by artist");labels.add("Play video");labels.add("Find release year online");labels.add("Online stream info");labels.add("Public video metadata");labels.add("Public alternative versions") }
        if(fromPlayer)labels.removeAll(listOf("Toggle favourite","Lyrics","Analyze audio and waveform","Play video"))
        choose(song.title,labels){i->when(labels[i]){
            "Mark / unmark explicit"->{val marked=LibraryStore.toggleExplicit(song.id);if(marked)pruneExplicitQueue();toast(if(marked)"Marked explicit" else "Explicit tag removed")}
            "Related artists locally"->relatedArtists(song.artist)
            "Artist page"->artistPage(song.artist)
            "Play next"->if(!LibraryStore.allowed(song.id))toast("Marked explicit: skipped")else controller?.let{it.addMediaItem(if(it.mediaItemCount==0)0 else it.currentMediaItemIndex+1,toMediaItem(song, playbackSource(it.currentMediaItem)));if(it.playbackState==Player.STATE_IDLE)it.prepare();saveQueue()}?:toast("Player not ready")
            "Add to queue"->if(!LibraryStore.allowed(song.id))toast("Marked explicit: skipped")else controller?.let{it.addMediaItem(toMediaItem(song, playbackSource(it.currentMediaItem)));if(it.playbackState==Player.STATE_IDLE)it.prepare();saveQueue()}?:toast("Player not ready")
            "Toggle favourite"->toast(if(LibraryStore.toggleFavorite(song))"Added to favourites" else "Removed from favourites")
            "Add to playlist"->addPlaylist(song)
            "Share"->share(if(song.localUri.isBlank())"${song.title} – https://music.youtube.com/watch?v=${song.id}" else "${song.title} – ${song.artist}")
            "Lyrics"->lyricsDialog(song)
            "Follow / unfollow artist"->advanced.artist(song)
            "Private song note"->advanced.note(song)
            "Set album name"->ask("Album name (blank clears override)",LibraryStore.albumTags()[song.id]?:song.album){LibraryStore.setAlbum(song.id,it);toast("Album tag saved")}
            "Set release year"->ask("Release year (1900–2100; blank clears)",LibraryStore.releaseYear(song.id)?.toString().orEmpty()){raw->val v=raw.trim();val y=v.toIntOrNull();if(v.isBlank()||(y!=null&&y in 1900..2100)){LibraryStore.setReleaseYear(song.id,y);toast("Release year saved locally")}else toast("Enter a valid year")}
            "Song details"->MaterialAlertDialogBuilder(this).setTitle(song.title).setMessage("Artist: ${song.artist}\nDuration: ${song.duration.ifBlank { "Unknown" }}\nSource: ${if(song.localUri.isNotBlank()) "Local audio" else "Online"}\nID: ${song.id}").setPositiveButton("Close",null).show()
            "Start artist mix"->startSeedRadio(song)
            "Move up in playlist","Move down in playlist"->playlist?.let{if(LibraryStore.move(it,song.id,if(labels[i]=="Move up in playlist")-1 else 1))showLocalList(LibraryStore.playlist(it),it,it)else toast("Already at playlist boundary")}
            "Remove from playlist"->playlist?.let{LibraryStore.remove(it,song);showLocalList(LibraryStore.playlist(it),it,it)}
            "Remove from offline library"->MaterialAlertDialogBuilder(this).setTitle("Remove offline audio?").setMessage("App-owned copies will be deleted. Original external files will not be touched.").setNegativeButton("Cancel",null).setPositiveButton("Remove"){_,_->com.abhi.ganasuno.data.OfflineFiles.remove(this,song);showLocalList(LibraryStore.list("local"),"Offline audio")}.show()
            "Analyze audio and waveform"->analyzeTrack(song)
            "Save offline copy"->offlineTask { token -> com.abhi.ganasuno.data.OfflineFiles.copyLocal(this,song,token) }
            "Public video metadata"->publicCatalogue.info(song)
            "Public alternative versions"->publicCatalogue.alternatives(song)
            "Other versions"->runSearch(R.string.no_results){Batch14.alternateVersions(song,YtApi.search("${song.title} ${song.artist}")+YtApi.search("${song.title} live cover remix"))}
            "Play video"->playVideo(song)
            "Find release year online"->lookupYear(song)
            "Online stream info"->onlineInfo(song)
            "More by artist"->doSearch("${song.artist} songs")
        }}
    }
    private fun share(text:String){startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,text),"Share"))}
    private fun searchHistory(){val items=LibraryStore.searches();choose("Search history",listOf("Clear history")+items){i->if(i==0){LibraryStore.clearSearch();toast("Search history cleared");if(page=="Search"&&b.toolsScroll.visibility==View.VISIBLE)openSearch()}else{b.search.setText(items[i-1]);doSearch(items[i-1])}}}
    private fun historyMenu(){choose("Listening",listOf("Recently played","Most listened","Analytics · 7 days","Analytics · 30 days / listening fingerprint","Analytics · 90 days","Analytics · all time","Clear listening history","This year","Monthly recap playlist","Custom date range","Most played (counts)","Yearly Wrapped","Most played tracks","Music by decade")){i->when(i){
        11->wrapped()
        12->showLocalList(LibraryStore.mostPlayed().map{it.first},"Most played")
        13->decadeMenu()
        0->showLocalList(LibraryStore.history().asReversed().map{it.song}.distinctBy{it.id},"Recently played")
        10->mostPlayedMenu()
        1->showLocalList(LibraryStore.history().groupBy{it.song.id}.values.sortedByDescending{g->g.sumOf{it.ms}}.map{it.first().song},"Most listened")
        6->MaterialAlertDialogBuilder(this).setTitle("Clear listening history?").setNegativeButton("Cancel",null).setPositiveButton("Clear"){_,_->LibraryStore.clearHistory();toast("History cleared")}.show()
        7-> { val start=java.time.LocalDate.now().withDayOfYear(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(); showInsights(LibraryStore.history().filter{it.at>=start},emptyList(),0,"This year") }
        8->monthlyRecap()
        9->dateInsights()
        else->analytics(intArrayOf(7,30,90,0)[i-2])
    }}}
    private fun wrapped(){ask("Recap year",java.time.Year.now().value.toString()){raw->val year=raw.toIntOrNull();if(year==null||year !in 1900..java.time.Year.now().value){toast("Enter a valid year");return@ask};val r=com.abhi.ganasuno.data.Batch11.recap(LibraryStore.history(),year,java.time.ZoneId.systemDefault());if(r==null){toast("No tracked listening that year");return@ask};val pages=listOf("Your year in music" to "${r.minutes} minutes\n${r.tracks} tracks\n${r.artists} artists\n${r.days} active days", "Top tracks" to r.topSongs.joinToString("\n"){"${it.first} · ${it.second/60000} min"},"Top artists" to r.topArtists.joinToString("\n"){"${it.first} · ${it.second/60000} min"},"Month by month" to r.months.joinToString("\n"){"${java.time.Month.of(it.first).name.take(3)} · ${it.second/60000} min"});fun page(n:Int){val entry=pages[n];MaterialAlertDialogBuilder(this).setTitle("GANASUNO Wrapped $year · ${n+1}/${pages.size} · ${entry.first}").setMessage(entry.second).setNegativeButton(if(n==0)"Close" else "Previous"){_,_->if(n>0)page(n-1)}.setNeutralButton("Share card"){_,_->advanced.card("$year · ${entry.first}",entry.second)}.setPositiveButton(if(n==pages.lastIndex)"Done" else "Next"){_,_->if(n<pages.lastIndex)page(n+1)}.show()};page(0)}}
    private fun mostPlayedMenu(){val ranked=LibraryStore.mostPlayed();if(ranked.isEmpty()){toast("No play counts yet. Counts start in v10 after five seconds of tracked playback.");return};choose("Most played · counts since v10",ranked.map{"${it.first.title} · ${it.second} plays"}){n->startQueue(ranked.map{it.first},n,"Most Played")}}
    private fun analytics(days:Int){val all=LibraryStore.history();val now=System.currentTimeMillis();val cut=now-days*86400000L;val h=if(days==0)all else all.filter{it.at>=cut};val previous=if(days==0)emptyList()else all.filter{it.at in (cut-days*86400000L) until cut};showInsights(h,previous,days,if(days==0)"All-time analytics" else "Last $days days")}
    private fun showInsights(h:List<LibraryStore.Listen>,prev:List<LibraryStore.Listen>,days:Int,title:String){val text=com.abhi.ganasuno.data.Insights.report(h,prev,days,LibraryStore.albumTags());MaterialAlertDialogBuilder(this).setTitle(title).setMessage(text).setNeutralButton("Share image"){_,_->advanced.card(title,text)}.setNegativeButton("Share text"){_,_->share("$title\n$text")}.setPositiveButton("Close",null).show()}
    private fun dateInsights(){ask("Start date (YYYY-MM-DD)"){a->ask("End date (YYYY-MM-DD)"){z->runCatching{val zone=java.time.ZoneId.systemDefault();val first=java.time.LocalDate.parse(a.trim());val last=java.time.LocalDate.parse(z.trim());require(!last.isBefore(first));val start=first.atStartOfDay(zone).toInstant().toEpochMilli();val end=last.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();val days=java.time.temporal.ChronoUnit.DAYS.between(first,last).toInt()+1;require(days in 1..36500);showInsights(LibraryStore.history().filter{it.at in start until end},LibraryStore.history().filter{it.at in (first.minusDays(days.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()) until start},days,"$first — $last")}.onFailure{toast("Invalid date range")}}}}
    private fun monthlyRecap(){val month=java.time.YearMonth.now();val start=month.atDay(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();val tracks=LibraryStore.history().filter{it.at>=start}.groupBy{it.song.id}.values.sortedByDescending{g->g.sumOf{it.ms}}.take(25).map{it.first().song};if(tracks.isEmpty()){toast("No listening history this month");return};val name="Recap $month";LibraryStore.create(name);tracks.forEach{LibraryStore.add(name,it)};showLocalList(LibraryStore.playlist(name),name,name)}
    private fun localPool():List<Song> = (LibraryStore.list("favorites")+LibraryStore.list("local")+LibraryStore.names().flatMap{LibraryStore.playlist(it)}+LibraryStore.history().map{it.song}).distinctBy{it.id}.filter{LibraryStore.allowed(it.id)}
    private fun startSeedRadio(seed:Song){if(!LibraryStore.allowed(seed.id)){toast("Explicit track skipped");return};toast("Building song radio…");val serial=++requestSerial;io.execute{val saved=localPool();val groups=LibraryStore.names().map{LibraryStore.playlist(it).map{track->track.artist}}+LibraryStore.history(30).groupBy{java.time.Instant.ofEpochMilli(it.at).atZone(java.time.ZoneId.systemDefault()).toLocalDate()}.values.map{day->day.map{it.song.artist}};val related=com.abhi.ganasuno.data.Batch17.related(seed.artist,groups);val remote=if(seed.localUri.isBlank()&&seed.artist.isNotBlank())runCatching{YtApi.search("${seed.artist} songs")}.getOrDefault(emptyList())else emptyList();val tracks=com.abhi.ganasuno.data.Batch17.radio(seed,saved+remote,related,emptySet()).filter{LibraryStore.allowed(it.id)};ui.post{if(!isDestroyed&&serial==requestSerial){if(tracks.isEmpty())toast("No similar tracks found; add more music to your library")else{showLocalList(listOf(seed)+tracks,"Radio · ${seed.title}");startQueue(listOf(seed)+tracks,0,"Radio · ${seed.title}")}}}}}
    private fun personalized(mode:Int){val history=LibraryStore.history();val fav=LibraryStore.list("favorites");val pool=localPool();val list=if(mode==0)com.abhi.ganasuno.data.Batch11.quick(history,fav).filter{LibraryStore.allowed(it.id)}else com.abhi.ganasuno.data.Batch11.mix(history,fav,pool);if(list.isEmpty()){toast("Listen to or save a few tracks first");return};showLocalList(list,if(mode==0)"Quick picks · for you" else "Your personalized mix")}
    private fun dailyCards(){val cards=com.abhi.ganasuno.data.Batch11.daily(LibraryStore.history(),LibraryStore.list("favorites").filter{LibraryStore.allowed(it.id)},localPool(),java.time.LocalDate.now());if(cards.isEmpty()){toast("Save or listen to songs to build your daily discovery");return};val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,12,24,12)};val d=MaterialAlertDialogBuilder(this).setTitle("Daily discover · ${java.time.LocalDate.now()}").setView(panel).setPositiveButton("Close",null).create();cards.forEach{card->val view=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,12,12,12)};view.addView(TextView(this).apply{text=card.title;textSize=20f});view.addView(TextView(this).apply{text=card.subtitle+" · ${card.tracks.size} tracks"});view.addView(Button(this).apply{text="Open mix";setOnClickListener{d.dismiss();showLocalList(card.tracks.filter{LibraryStore.allowed(it.id)},card.title)}});panel.addView(view)};d.show()}
    private fun discoverMenu(){val labels=listOf("Relax","Sleep","Energize","Workout","Party","Focus","Commute","Podcasts","New releases","Trending videos","Daily discover","Your mix");choose("Discover · search-based",labels){i->val top=LibraryStore.history(30).groupBy{it.song.artist}.maxByOrNull{e->e.value.sumOf{it.ms}}?.key;if(i==7){podcastMenu();return@choose};if(i==10){dailyCards();return@choose};if(i==11){personalized(1);return@choose};val query=when(i){7->"music podcasts";8->"new music releases";9->"trending music videos";10->"${listOf("indie","acoustic","jazz","classical","lofi","pop","folk")[Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-1]} songs";11->"${top?.takeIf{it.isNotBlank()}?:"popular"} music mix";else->"${labels[i]} music"};b.search.setText(query);if(i==9)runSearch(R.string.no_results,video=true){YtApi.searchVideos(query)}else doSearch(query,labels[i])}}
    private fun offlineTask(play:Boolean=false,confirmQuality:Boolean=false,task:(com.abhi.ganasuno.data.OfflineFiles.Cancellation)->Song){
        val token=com.abhi.ganasuno.data.OfflineFiles.Cancellation()
        val dialog=MaterialAlertDialogBuilder(this).setTitle("Saving offline audio").setMessage("Copying an authorized file…").setCancelable(false).setNegativeButton("Cancel"){_,_->token.cancel()}.show()
        io.execute{val result=runCatching{task(token)};val rate=if(play)result.getOrNull()?.let{com.abhi.ganasuno.data.OfflineFiles.estimatedBitrate(this,it)} else null;ui.post{if(!isDestroyed){dialog.dismiss();result.onSuccess{toast("Saved: ${it.title}"+if(play)" · ${com.abhi.ganasuno.data.Batch20.rateLabel(rate)}" else "");if(play&&confirmQuality)MaterialAlertDialogBuilder(this).setTitle("Custom audio saved").setMessage(com.abhi.ganasuno.data.Batch20.rateLabel(rate)+". No transcoding or bitrate upgrade was performed.").setNegativeButton("Keep in library",null).setPositiveButton("Play this file"){_,_->startQueue(listOf(it),0)}.show()else if(play)startQueue(listOf(it),0)else showLocalList(LibraryStore.list("local"),"Offline audio")}.onFailure{toast(if(token.isCancelled())"Download cancelled" else it.message?:"Save failed")}}}}
    }
    private fun offlineMenu(){val bytes=com.abhi.ganasuno.data.OfflineFiles.size(this);choose("Offline storage · ${bytes/1048576} MB owned",listOf("Show offline library","Import device audio","Save authorized direct audio (optional checksum)","Downloads and smart downloads","Remove all app-owned audio","Verify saved audio integrity")){i->when(i){0->showLocalList(LibraryStore.list("local"),"Offline audio");1->importAudio.launch(arrayOf("audio/*"));2->batch22MenuAction(6);3->moreFeaturesAction(20);4->MaterialAlertDialogBuilder(this).setTitle("Delete all app-owned audio copies?").setNegativeButton("Cancel",null).setPositiveButton("Delete"){_,_->io.execute{LibraryStore.list("local").filter{com.abhi.ganasuno.data.OfflineFiles.owned(this,it)}.forEach{com.abhi.ganasuno.data.OfflineFiles.remove(this,it)};ui.post{toast("Offline copies removed")}}}.show();5->offlineAudit()}}}
    private fun animatedStorage(){val count=com.abhi.ganasuno.ui.AnimatedCover.count(this);val bytes=com.abhi.ganasuno.ui.AnimatedCover.storedBytes(this);MaterialAlertDialogBuilder(this).setTitle("Imported animated covers").setMessage("$count saved covers · ${bytes/1024} KB. These are user-imported files, not a thumbnail or Spotify Canvas cache. Removing them leaves offline songs untouched.").setNegativeButton("Close",null).setPositiveButton("Remove all covers"){_,_->MaterialAlertDialogBuilder(this).setTitle("Delete imported artwork?").setMessage("This permanently removes all imported animated covers. Static album images and music remain.").setNegativeButton("Cancel",null).setPositiveButton("Delete"){_,_->io.execute{val removed=com.abhi.ganasuno.ui.AnimatedCover.clearImported(this);ui.post{if(!isDestroyed)toast("Removed $removed covers")}}}.show()}.show()}
    private fun offlineAudit(){val songs=LibraryStore.list("local").filter{com.abhi.ganasuno.data.OfflineFiles.owned(this,it)};if(songs.isEmpty()){toast("No app-owned audio");return};choose("Verify audio integrity · select file",songs.map{it.title+" · "+it.artist}){n->val song=songs[n];io.execute{val r=runCatching{com.abhi.ganasuno.data.OfflineFiles.audit(this,song)};ui.post{if(!isDestroyed&&!isFinishing)MaterialAlertDialogBuilder(this).setTitle(song.title).setMessage(r.getOrDefault("Could not read file")).setPositiveButton("Close",null).show()}}}}
    private fun thumbnailStorage(){val mem=ImageLoader.memoryBytes();val disk=ImageLoader.diskBytes(this);val owned=com.abhi.ganasuno.data.OfflineFiles.size(this);choose("Storage · offline ${owned/1048576} MB · thumbnails ${disk/1024} KB disk / ${mem/1024} KB RAM",listOf("Clear memory thumbnails","Clear disk thumbnails","Clear both thumbnail caches","Playback media cache","Manage imported animated covers")){n->if(n==3)mediaCacheMenu()else if(n==4)animatedStorage()else{when(n){0->ImageLoader.clear();1->ImageLoader.clearDisk(this);2->ImageLoader.clearAll(this)};toast("Selected thumbnail cache cleared; offline music untouched")}}}
    private fun backupMenu(){choose("Automatic local backups",listOf("Schedule (off / daily / weekly / 30 days)","Retained copies","Create metadata snapshot now","Restore metadata snapshot","Scheduled audio archives","Create audio archive now","Restore audio archive snapshot")){i->when(i){
        0->choose("Schedule",listOf("Off","Daily","Weekly","Every 30 days")){n->LibraryStore.option("backup_days",intArrayOf(0,1,7,30)[n],true);toast(if(com.abhi.ganasuno.data.AutoBackup.schedule(this))"Schedule saved; Android may defer jobs" else "Schedule failed")}
        1->choose("Keep snapshots",listOf("3","5","10","20")){n->LibraryStore.option("backup_keep",intArrayOf(3,5,10,20)[n],true)}
        2->io.execute{val r=runCatching{com.abhi.ganasuno.data.AutoBackup.save(this)};ui.post{toast(if(r.isSuccess)"Snapshot created" else "Snapshot failed")}}
        3->{val files=com.abhi.ganasuno.data.AutoBackup.files(this);choose("Restore a private snapshot",files.map{java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it.name.substringBefore('.').toLong()))}){n->MaterialAlertDialogBuilder(this).setTitle("Replace library metadata?").setMessage("Audio files and external permissions are not restored.").setNegativeButton("Cancel",null).setPositiveButton("Restore"){_,_->io.execute{val r=runCatching{LibraryStore.restore(files[n].readText())};ui.post{toast(if(r.isSuccess)"Restored" else "Restore failed");if(r.isSuccess)showLocalList(LibraryStore.list("favorites"),"Favourites")}}}.show()}}
        4->choose("Include app-owned audio in scheduled archives? · up to 1 GB each",listOf("Metadata only","Metadata + audio + portable settings")){n->LibraryStore.option("backup_audio",n,true);toast("Backup preference saved")}
        5->io.execute{val r=runCatching{com.abhi.ganasuno.data.AutoBackup.saveAudio(this)};ui.post{toast(if(r.isSuccess)"Audio snapshot created" else r.exceptionOrNull()?.message?:"Snapshot failed")}}
        6->{val files=com.abhi.ganasuno.data.AutoBackup.audioFiles(this);choose("Restore private audio snapshot",files.map{java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it.name.substringBefore('.').toLong()))}){n->MaterialAlertDialogBuilder(this).setTitle("Restore library, owned audio and settings?").setMessage("Existing audio is retained; conflicts are rejected. Restart app to apply all settings.").setNegativeButton("Cancel",null).setPositiveButton("Restore"){_,_->io.execute{val r=runCatching{files[n].inputStream().use{com.abhi.ganasuno.data.MediaArchive.restore(this,it,{raw->com.abhi.ganasuno.data.TransferSettings.restore(raw,false)},{raw->com.abhi.ganasuno.data.TransferSettings.restore(raw)})}};ui.post{toast(if(r.isSuccess)"Restored; restart app to apply all settings" else r.exceptionOrNull()?.message?:"Restore failed")}}}.show()}}
    }}}

    private fun bulkMenu(){val list=songs.toList();if(list.isEmpty()){toast("Open a song list first");return};val checked=BooleanArray(list.size);MaterialAlertDialogBuilder(this).setTitle("Select songs").setMultiChoiceItems(list.map{it.title}.toTypedArray(),checked){_,i,v->checked[i]=v}.setNegativeButton("Cancel",null).setPositiveButton("Actions"){_,_->val selected=list.filterIndexed{i,_->checked[i]};if(selected.isEmpty())return@setPositiveButton;choose("${selected.size} songs",listOf("Add to favourites","Add to queue","Add to playlist")){i->when(i){0->{LibraryStore.addFavorites(selected);toast("Saved")};1->controller?.let{c->c.addMediaItems(selected.map{toMediaItem(it, playbackSource(c.currentMediaItem))});if(c.playbackState==Player.STATE_IDLE)c.prepare();saveQueue()};2->{val names=LibraryStore.names();choose("Select playlist",names){n->selected.forEach{LibraryStore.add(names[n],it)};toast("Added")}}}}}.show()}
    private fun queueMenu(){val c=controller?:return;val entries=(0 until c.mediaItemCount).map{fromMediaItem(c.getMediaItemAt(it))};choose("Queue",entries.mapIndexed{i,s->"${if(i==c.currentMediaItemIndex)"▶ " else ""}${s.title}"}){i->choose(entries[i].title,listOf("Play now","Move up","Move down","Move to play next","Remove")){a->if(i>=c.mediaItemCount){toast("Queue changed");return@choose};when(a){0->{c.seekTo(i,0);c.play()};1->if(i>0)c.moveMediaItem(i,i-1);2->if(i<c.mediaItemCount-1)c.moveMediaItem(i,i+1);3->{val current=c.currentMediaItemIndex;if(i!=current){val target=(if(i<current)current else current+1).coerceIn(0,c.mediaItemCount-1);c.moveMediaItem(i,target)}};4->c.removeMediaItem(i)};saveQueue();queueMenu()}}}
    private fun playbackControls(advancedOnly:Boolean=false){val c=controller?:return toast("Player is connecting; try again") ;val labels=listOf("Repeat: ${listOf("Off","One","All")[c.repeatMode]}","Shuffle: ${if(c.shuffleModeEnabled)"On" else "Off"}","Speed: ${c.playbackParameters.speed}x","Pitch: ${c.playbackParameters.pitch}x","Queue","Endless artist mix · ${com.abhi.ganasuno.ui.SmartSettingIndicators.onOff(LibraryStore.option("endless")==1)}","Sleep fade duration · ${LibraryStore.option("sleep_fade",8000)/1000}s","Preload next stream · ${listOf("Off","URL only","Buffer").getOrElse(LibraryStore.option("preload")){"Custom"}}","Close mini-player and clear queue","Manual fade in / out","Crossfade and DJ transitions");val indices=labels.indices.filter{!advancedOnly||it !in listOf(0,1,4)};choose("Playback",indices.map{labels[it]}){selected->val i=indices[selected];when(i){0->{c.repeatMode=(c.repeatMode+1)%3;LibraryStore.option("repeat",c.repeatMode,true);playbackControls()};1->{c.shuffleModeEnabled=!c.shuffleModeEnabled;LibraryStore.option("shuffle",if(c.shuffleModeEnabled)1 else 0,true);playbackControls()};2,3->{val values=floatArrayOf(0.5f,0.75f,1f,1.25f,1.5f,2f);choose(if(i==2)"Speed" else "Pitch",values.map{"${it}x"}){n->c.playbackParameters=if(i==2)PlaybackParameters(values[n],c.playbackParameters.pitch)else PlaybackParameters(c.playbackParameters.speed,values[n])}};4->queueMenu();5->choose("Endless artist mix",listOf("Off","On")){v->LibraryStore.option("endless",v,true)};6->choose("Sleep fade",listOf("2 seconds","5 seconds","8 seconds","15 seconds")){v->LibraryStore.option("sleep_fade",intArrayOf(2000,5000,8000,15000)[v],true)};7->choose("Next-track preloading",listOf("Off","Resolve next stream URL only","Buffer up to 512 KiB · unmetered + media cache")){v->LibraryStore.option("preload",v,true);toast(if(v==2)"Enable playback media cache in Settings → Storage" else "Saved")};8->closeMini();9->fadeMenu();10->transitionMenu()}}}
    private fun fadeMenu(){choose("Manual fade · ${LibraryStore.option("manual_fade",1000)} ms",listOf("Fade in and play","Fade out and pause","Set fade duration (ms)","Cancel fade")){i->when(i){0,1->com.abhi.ganasuno.player.ManualFade.start(i==0,LibraryStore.option("manual_fade",1000).coerceIn(100,30000));2->ask("Duration in ms (100–30000)",LibraryStore.option("manual_fade",1000).toString()){v->val n=v.toIntOrNull();if(n!=null&&n in 100..30000){LibraryStore.option("manual_fade",n,true);fadeMenu()}else toast("Invalid duration")};3->com.abhi.ganasuno.player.ManualFade.cancel()}}}
    private fun transitionMenu(){choose("Audio transitions · experimental",listOf("Crossfade · ${LibraryStore.option("crossfade_seconds")} seconds","Keep same album continuous · ${if(LibraryStore.option("keep_album_continuous",1)==1)"On" else "Off"}","DJ filter sweeps · ${if(LibraryStore.option("dj_transition")==1)"On" else "Off"}")){n->when(n){0->ask("Crossfade seconds (0–15; 0 = off)",LibraryStore.option("crossfade_seconds").toString()){v->val secs=v.toIntOrNull();if(secs==null||secs !in 0..15)toast("Enter 0–15")else{com.abhi.ganasuno.player.PlayerHub.cancelTransition?.invoke();LibraryStore.option("crossfade_seconds",secs,true);toast("Saved · seekable audio only")}};1->{LibraryStore.option("keep_album_continuous",1-LibraryStore.option("keep_album_continuous",1),true);com.abhi.ganasuno.player.PlayerHub.cancelTransition?.invoke();transitionMenu()};2->{LibraryStore.option("dj_transition",1-LibraryStore.option("dj_transition"),true);transitionMenu()}}}}
    private fun toolsMenu(){choose("GANASUNO",listOf("Audio and video settings","Playback controls","Equalizer","Theme","Library and backup","Search history","Listening analytics","About and privacy","Discovery settings","Device audio effects","Search suggestions","Storage and backups","Diagnostics","Advanced tools","More features (v5–28)","Listen Together (self-hosted)","New features (v8)")){i->toolsMenuAction(i)}}
    private fun toolsMenuAction(i:Int){when(i){0->SettingsDialog.show(this);1->playbackControls();2->com.abhi.ganasuno.ui.EqualizerDialog.show(this);3->choose("Theme",listOf("System","Light","Dark")){n->val mode=intArrayOf(-1,1,2)[n];LibraryStore.option("theme",mode,true);AppCompatDelegate.setDefaultNightMode(mode)};4->libraryMenu();5->searchHistory();6->historyMenu();8->choose("Discovery settings",listOf("Country","Home result limit","Home source")){n->if(n==0)choose("Content country",listOf("India","USA","UK","Japan","South Korea","Germany")){v->LibraryStore.option("country",v,true);home()}else if(n==2)choose("Home source",listOf("Existing music search","Official public catalogue · bounded results")){v->LibraryStore.option("home_source",v,true);home()}else choose("Home result limit",listOf("10","20","30","50","100")){v->LibraryStore.option("home_limit",intArrayOf(10,20,30,50,100)[v],true);home()}};9->{val session=com.abhi.ganasuno.player.PlayerHub.player?.audioSessionId;if(session==null||session<=0)toast("Start a song first")else runCatching{startActivity(Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION,session).putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME,packageName).putExtra(android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE,android.media.audiofx.AudioEffect.CONTENT_TYPE_MUSIC))}.onFailure{toast("System equalizer unavailable")}};10->suggestions();11->choose("Storage and backups",listOf("Offline storage","Automatic backups")){v->if(v==0)offlineMenu()else backupMenu()};12->diagnostics();13->advanced.show();14->moreFeatures();15->rooms.open();16->v8Menu();7->MaterialAlertDialogBuilder(this).setTitle("GANASUNO ${BuildConfig.VERSION_NAME}").setMessage("Library and listening history stay on this device. Network search and streaming use existing third-party endpoints. Selected online lyrics providers receive track and artist names only when you fetch lyrics. Metadata backups exclude credentials and audio files; full audio archive is available separately. This build has no analytics tracker.").setPositiveButton("Close",null).show()}}

    private fun suggestions(){val q=b.search.text.toString().trim();val matches=(LibraryStore.searches()+LibraryStore.list("favorites").map{it.title}+quick).distinct().filter{q.isBlank()||it.contains(q,true)}.take(30);choose("Local search suggestions",matches){i->b.search.setText(matches[i]);doSearch(matches[i])}}
    private fun outputPicker(){
        val player=com.abhi.ganasuno.player.PlayerHub.player?:return toast("Start playback first")
        val manager=getSystemService(android.media.AudioManager::class.java)
        val devices=manager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS).filter{it.isSink}
        fun type(d:android.media.AudioDeviceInfo)=when(d.type){2->"Speaker";3,4->"Wired";8,7,23,26,27,30->"Bluetooth / hearing device";9,10,29->"HDMI";11,12,22->"USB";else->"Output ${d.type}"}
        choose("Preferred output · device may override",listOf("System default")+devices.map{"${type(it)} · ${it.productName}"}){n->
            runCatching{player.setPreferredAudioDevice(if(n==0)null else devices[n-1])}.onSuccess{toast("Output preference set; Android controls availability")}.onFailure{toast("Output unavailable")}
        }
    }
    private fun dspMenu(includeRotation:Boolean=true){
        fun save(key:String,value:Int){LibraryStore.option(key,value,true);com.abhi.ganasuno.player.EffectsSettings.refresh();toast("Effect saved · stereo PCM playback")}
        val labels=listOf("8D stereo rotation · ${com.abhi.ganasuno.ui.SmartSettingIndicators.onOff(LibraryStore.option("rotate")==1)}","Rotation period · ${LibraryStore.option("rotate_period",8)}s","Rotation direction · ${if(LibraryStore.option("rotate_direction",1)==1)"Right first" else "Left first"}","Delay presets · ${if(LibraryStore.option("delay_ms")==0)"Off" else "${LibraryStore.option("delay_ms")} ms"}","Custom delay time · ${LibraryStore.option("delay_ms")} ms","Feedback · ${LibraryStore.option("delay_feedback",20)}%","Wet mix · ${LibraryStore.option("delay_mix",20)}%")
        val indices=labels.indices.filter{includeRotation||it!=0}
        choose("Stereo rotation / delay · 16-bit PCM",indices.map{labels[it]}){selected->val i=indices[selected];when(i){
            0->choose("Rotation",listOf("Off","On")){save("rotate",it)}
            1->ask("Rotation seconds (3–30)",LibraryStore.option("rotate_period",8).toString()){val v=it.toIntOrNull();if(v!=null&&v in 3..30)save("rotate_period",v)else toast("Invalid duration")}
            2->choose("Direction",listOf("Right first","Left first")){save("rotate_direction",if(it==0)1 else -1)}
            3->choose("Delay",listOf("Off","Slapback","Quarter-style","Dub")){n->val presets=arrayOf(intArrayOf(0,0,0),intArrayOf(95,12,15),intArrayOf(350,28,22),intArrayOf(480,53,30));val v=presets[n];LibraryStore.option("delay_feedback",v[1],true);LibraryStore.option("delay_mix",v[2],true);save("delay_ms",v[0])}
            4,5,6->{val key=listOf("delay_ms","delay_feedback","delay_mix")[i-4];val max=intArrayOf(500,65,45)[i-4];ask("Value (0–$max)",LibraryStore.option(key,if(i==4)0 else 20).toString()){val v=it.toIntOrNull();if(v!=null&&v in 0..max)save(key,v)else toast("Invalid value")}}
        }}
    }
    private fun moreFeatures(){choose("GANASUNO ${BuildConfig.VERSION_NAME}",listOf("Theme colour presets","Export settings","Import settings","Backup library + owned audio","Restore audio archive","Developer diagnostics","Support and documentation","Preferred audio output","Stereo rotation and delay","10-band software EQ","Current track waveform and music tags","Listening fingerprint","Music by decade","Batch 4–5 tools","Batch 6 tools","Batch 7 · Public catalogue","MusicBrainz · songs, artists and albums","Batch 22–23 · catalogue, artwork and sync","Batch 25 · community and charts","Batch 27 · account and blog","Batch 28 · offline automation and release alerts")){i->moreFeaturesAction(i)}}
    private fun moreFeaturesAction(i:Int){when(i){
        0->choose("Theme colours",listOf("Default","Wallpaper (Android 12+)","Spotify-inspired green","SoundCloud-inspired orange","Apple Music-inspired pink","Titanium Grey","Custom colour")){n->if(n==6)ask("Custom colour (#RRGGBB)","#%06X".format(LibraryStore.option("custom_color",0xff7c3aed.toInt()) and 0xffffff)){value->val color=com.abhi.ganasuno.data.BatchCore.color(value);if(color==null)toast("Enter a six-digit hex colour")else{LibraryStore.option("custom_color",color,true);LibraryStore.option("palette",6,true);recreate()}}else{LibraryStore.option("palette",n,true);recreate()}}
        1->share(com.abhi.ganasuno.data.TransferSettings.export())
        2->ask("Paste exported settings JSON"){raw->runCatching{com.abhi.ganasuno.data.TransferSettings.restore(raw)}.onSuccess{val p=com.abhi.ganasuno.player.PlayerHub.player;p?.repeatMode=LibraryStore.option("repeat");p?.shuffleModeEnabled=LibraryStore.option("shuffle")==1;com.abhi.ganasuno.player.PlayerHub.applyAll();com.abhi.ganasuno.player.EffectsSettings.refresh();val mode=LibraryStore.option("theme",-1);if(AppCompatDelegate.getDefaultNightMode()!=mode)AppCompatDelegate.setDefaultNightMode(mode)else recreate();toast("Settings imported")}.onFailure{toast("Invalid settings document")}}
        3->MaterialAlertDialogBuilder(this).setTitle("Full audio backup").setMessage("Includes private library data, portable settings and app-owned audio up to 1 GB. External linked files, account credentials and permissions are excluded. Save to a private location.").setNegativeButton("Cancel",null).setPositiveButton("Choose file"){_,_->exportMediaArchive.launch("ganasuno-audio.zip")}.show()
        4->importMediaArchive.launch(arrayOf("application/zip","application/octet-stream"))
        5->choose("Developer diagnostics",listOf("App and SDK information","View and export local errors","Clear thumbnail memory cache","Previous crash report")){n->when(n){0->MaterialAlertDialogBuilder(this).setTitle("Build information").setMessage("GANASUNO ${BuildConfig.VERSION_NAME}\nPackage: $packageName\nAndroid API: ${Build.VERSION.SDK_INT}\nAudio session: ${com.abhi.ganasuno.player.PlayerHub.player?.audioSessionId ?: 0}").setPositiveButton("Close",null).show();1->diagnostics();2->{ImageLoader.clear();toast("Thumbnail memory cache cleared")};3->com.abhi.ganasuno.data.CrashRecord.pending(this)?.let{showCrashReport(it)}?:toast("No saved crash report")}}
        7->outputPicker()
        8->dspMenu()
        9->com.abhi.ganasuno.ui.TenBandDialog.show(this)
        10->controller?.currentMediaItem?.let{analyzeTrack(fromMediaItem(it))}?:toast("Play a track first")
        11->analytics(30)
        12->decadeMenu()
        13->batch14Menu()
        14->batch15Menu()
        15->publicCatalogue.show()
        16->musicCatalogue.show(controller?.currentMediaItem?.let{fromMediaItem(it)})
        17->batch22Menu()
        18->com.abhi.ganasuno.ui.CommunityTools(this,io,{controller?.currentMediaItem?.let{fromMediaItem(it)}},{pos->controller?.seekTo(pos)},{mediaCacheMenu()},publicCatalogue).show()
        19->com.abhi.ganasuno.ui.AccountTools(this,io){controller?.currentMediaItem?.let{fromMediaItem(it)}}.show()
        20->com.abhi.ganasuno.ui.Batch28Tools(this,io,{controller?.currentMediaItem?.let{fromMediaItem(it)}},{id->offlineTask(true,true){token->com.abhi.ganasuno.data.SmartOffline.save(this,id,token)}},{authorizedVideo()}).show()
        6->releaseNotes()
    }}

    private fun v8Menu(){choose("GANASUNO · Additional tools",listOf("Prefer 320 kbps / authorized audio link","Explicit-content filter","Up next · artwork queue","Animated artwork","Curated discovery playlists","Related artists · local or Last.fm","Saved YouTube playlists","Favourite albums / playlists / podcasts","Right-to-left layout","Support links")){i->v8MenuAction(i)}}
    private fun v8MenuAction(i:Int){when(i){
        0->choose("Audio quality and authorized files",listOf("Prefer 320 kbps when available","Save and play an authorized direct audio file · verify actual bitrate")){n->if(n==0){com.abhi.ganasuno.data.Prefs.audioQuality=com.abhi.ganasuno.data.Prefs.Q_320;toast("Preferred; actual bitrate depends on the source")}else ask("Authorized direct HTTPS audio URL"){raw->offlineTask(true,true){token->com.abhi.ganasuno.data.OfflineFiles.fetch(this,raw,token)}}}
        1->choose("Explicit filter · provider badges and manual tags",listOf("Allow all","Skip marked explicit tracks")){n->LibraryStore.option("skip_explicit",n,true);if(n==1)pruneExplicitQueue();toast("Filter saved. Provider explicit badges and manual tags are skipped; unlabelled content is not classified.")}
        2->artworkQueue()
        3->choose("Animated cover artwork",listOf("Off","Slow turntable rotation while playing","Import animated GIF / WebP for current track (Android 9+)","Remove current animated cover")){n->when(n){0,1->{LibraryStore.option("animated_art",n,true);toast("Reopen Now Playing")};2->{if(Build.VERSION.SDK_INT<28)toast("Requires Android 9+")else{val id=controller?.currentMediaItem?.mediaId;if(id==null)toast("Play a track first")else{animatedCoverId=id;animatedCoverPicker.launch(arrayOf("image/gif","image/webp"))}}};3->{controller?.currentMediaItem?.mediaId?.let{com.abhi.ganasuno.ui.AnimatedCover.remove(this,it);toast("Animated cover removed; reopen Now Playing")}}}}
        4->curatedDiscovery()
        5->relatedArtists(controller?.currentMediaItem?.mediaMetadata?.artist?.toString().orEmpty())
        6->collectionMenu("Playlist",true)
        7->choose("Favourite collections",listOf("Album","Playlist","Podcast")){n->collectionMenu(listOf("Album","Playlist","Podcast")[n],false)}
        8->choose("Layout direction",listOf("Follow device language","Left to right","Right to left","App language (Arabic / Hebrew / System)")){n->if(n==3)choose("App language",listOf("System","English","Arabic · العربية","Hebrew · עברית")){v->LibraryStore.option("rtl_preview",0,true);androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(listOf("","en","ar","he")[v]))}else{LibraryStore.option("rtl_preview",n,true);recreate()}}
        9->supportLinks()
    }}

    private fun lookupYear(song:Song){MaterialAlertDialogBuilder(this).setTitle("Look up release date?").setMessage("MusicBrainz receives this track title and artist. Choose the correct recording; no year is guessed.").setNegativeButton("Cancel",null).setPositiveButton("Search"){_,_->io.execute{val result=runCatching{RemoteMetadata.years(song)};ui.post{if(!isDestroyed)result.onSuccess{rows->choose("Choose matching recording · MusicBrainz",rows.map{"${it.title} — ${it.artist} · ${it.year}"}){n->LibraryStore.setReleaseYear(song.id,rows[n].year);toast("Release year saved")}}.onFailure{toast(it.message?:"Release lookup failed")}}}}.show()}
    private fun onlineInfo(song:Song){io.execute{val result=runCatching{com.abhi.ganasuno.data.Streams.describe(song.id)};ui.post{if(!isDestroyed)result.onSuccess{MaterialAlertDialogBuilder(this).setTitle(song.title).setMessage(it).setPositiveButton("Close",null).show()}.onFailure{toast(it.message?:"Stream info unavailable")}}}}
    private fun batch22Menu(){choose("Batch 22 tools",listOf("Find playable versions of current song","Combined stream and public video details","Curator channel playlists","YouTube channel follow / subscription sync","Import animated art from authorized HTTPS URL","Animated artwork download cache","Save direct audio with checksum and length validation")){n->batch22MenuAction(n)}}
    private fun batch22MenuAction(n:Int){when(n){
        0->{val seed=controller?.currentMediaItem?.let{fromMediaItem(it)};if(seed==null||seed.artist.isBlank())toast("Play a track with known artist first")else{toast("Searching alternate recordings…");io.execute{val r=runCatching{com.abhi.ganasuno.data.Batch22.versions(seed,YtApi.search("${seed.title} ${seed.artist} live acoustic remix" )+YtApi.search("${seed.title} ${seed.artist}"))};ui.post{if(!isDestroyed)r.onSuccess{if(it.isEmpty())toast("No same-artist candidate found")else showLocalList(it,"Possible alternate recordings · verify edition")}.onFailure{toast("Search unavailable")}}}}}
        1->{val song=controller?.currentMediaItem?.let{fromMediaItem(it)};if(song==null)toast("Play a track first")else publicCatalogue.combinedInfo(song)}
        2->publicCatalogue.curator()
        3->com.abhi.ganasuno.ui.SubscriptionTools(this,io).show()
        4->{val song=controller?.currentMediaItem?.let{fromMediaItem(it)};if(song==null)toast("Play a track first")else ask("Authorized direct HTTPS GIF / WebP URL"){raw->MaterialAlertDialogBuilder(this).setTitle("Download animated artwork?").setMessage("Only use artwork you have permission to save. The image server receives your request. 8 MiB limit; Android 9+.").setNegativeButton("Cancel",null).setPositiveButton("Download"){_,_->io.execute{val r=runCatching{com.abhi.ganasuno.ui.AnimatedCover.fetch(this,song.id,raw)};ui.post{if(!isDestroyed)r.onSuccess{LibraryStore.option("animated_art",2,true);toast("Artwork cached; reopen Now Playing")}.onFailure{toast("Animated artwork download failed")}}}}.show()}}
        5->{val stats=com.abhi.ganasuno.ui.AnimatedCover.remoteStats(this);MaterialAlertDialogBuilder(this).setTitle("Downloaded animated artwork cache").setMessage("${stats.first} images · ${stats.second/1024} KB. Imported covers and offline audio are not affected.").setNegativeButton("Close",null).setPositiveButton("Clear downloaded artwork"){_,_->io.execute{val count=com.abhi.ganasuno.ui.AnimatedCover.clearRemote(this);ui.post{if(!isDestroyed)toast("Cleared $count cached images")}}}.show()}
        6->MaterialAlertDialogBuilder(this).setTitle("Authorized direct audio only").setMessage("File length is validated, SHA-256 is calculated, and the audio is checked before saving. No streaming-service downloads.").setNegativeButton("Cancel",null).setPositiveButton("Continue"){_,_->ask("Direct HTTPS audio URL"){url->ask("Expected SHA-256 (optional)","none"){hash->offlineTask{token->com.abhi.ganasuno.data.OfflineFiles.fetch(this,url,token,hash.trim().takeUnless{it.equals("none",true)||it.isBlank()})}}}}.show()
    }}

    private fun batch15Menu(){choose("Batch 6 tools",listOf("Browse saved artists and albums","Create or import playlist URL","Current playback format","Release notes and help","Clear online waveform summaries")){n->batch15MenuAction(n)}}
    private fun batch15MenuAction(n:Int){when(n){0->localCatalogue();1->sharedPlaylists().show();2->currentFormat();3->releaseNotes();4->{com.abhi.ganasuno.data.LocalTrackAnalyzer.clearSummaries();toast("Waveform summaries cleared")}}}

    private fun currentFormat(){val p=com.abhi.ganasuno.player.PlayerHub.player?:return toast("Start audio playback first");val f=p.audioFormat;val text=if(f==null)"No decoded audio format available" else "Codec: ${f.codecs?:"Unknown"}\nMIME: ${f.sampleMimeType?:"Unknown"}\nBitrate: ${(f.averageBitrate.takeIf{it>0}?:f.peakBitrate).takeIf{it>0}?.let{"${it/1000} kbps"}?:"Unknown"}\nSample rate: ${f.sampleRate.takeIf{it>0}?:"Unknown"}\nChannels: ${f.channelCount.takeIf{it>0}?:"Unknown"}";MaterialAlertDialogBuilder(this).setTitle("Current playback format").setMessage(text).setPositiveButton("Close",null).show()}
    private fun releaseNotes(){val notes=runCatching{assets.open("release_notes.txt").bufferedReader().use{it.readText()}}.getOrDefault("Release notes unavailable");MaterialAlertDialogBuilder(this).setTitle("GANASUNO ${BuildConfig.VERSION_NAME} · Release notes").setMessage(notes).setPositiveButton("Close",null).setNeutralButton("Support links"){_,_->supportLinks()}.show()}
    private fun batch14Menu(){choose("Batch 4–5 tools",listOf("Video results playback mode","Playback media cache","Lookup current track release year","Podcast RSS library","Share linked playlist URL","Automatic backup options","Project blog and support")){n->batch14MenuAction(n)}}
    private fun batch14MenuAction(n:Int){when(n){0->choose("Video results",listOf("Audio only","Open video player")){v->LibraryStore.option("video_mode",v,true)};1->mediaCacheMenu();2->controller?.currentMediaItem?.let{lookupYear(fromMediaItem(it))}?:toast("Play a track first");3->podcastMenu();4->playlistLinkMenu();5->backupMenu();6->supportLinks()}}

    private fun mediaCacheMenu(){val bytes=PlaybackCache.size(this);val applied=PlaybackCache.appliedLimit();val requested=PlaybackCache.requestedLimit();choose("Media cache · ${bytes/1024} KB · limit $requested MiB"+(if(applied!=null&&applied!=requested)" (active $applied MiB)" else ""),listOf("Enable transient cache"+if(LibraryStore.option("media_cache")==1)" · Current" else "","Disable cache"+if(LibraryStore.option("media_cache")!=1)" · Current" else "","Clear cached media","Change size limit")){n->when(n){0,1->{LibraryStore.option("media_cache",if(n==0)1 else 0,true);toast("Applies to newly opened streams")};2->{io.execute{val r=runCatching{PlaybackCache.clear(this)};ui.post{toast(if(r.isSuccess)"Media cache cleared; offline files untouched" else "Cache clear failed")}}};3->{val choices=com.abhi.ganasuno.data.Batch25.cacheChoices;choose("Cache size · takes effect when playback service restarts",choices.map{"$it MiB"}){i->LibraryStore.option("media_cache_mib",choices[i],true);toast("Limit saved. Stop playback and restart the app/service to apply.")}}}}}
    private fun podcastMenu(){val feeds=LibraryStore.collections().filter{it.kind=="Podcast"};choose("Podcast library · public RSS",listOf("+ Add public RSS feed","Saved offline podcast episodes")+feeds.map{it.title}){i->when(i){0->ask("Podcast name"){title->ask("Public HTTPS RSS feed URL"){raw->val url=com.abhi.ganasuno.data.V8Core.httpsUrl(raw);if(url==null)toast("Invalid HTTPS URL")else io.execute{val r=runCatching{val episodes=com.abhi.ganasuno.data.PodcastFeed.fetch(url);require(episodes.isNotEmpty()){ "No audio episodes" };LibraryStore.saveCollection("Podcast",title.trim(),url);episodes};ui.post{if(!isDestroyed)r.onSuccess{showEpisodes(title,it)}.onFailure{toast(it.message?:"Feed unavailable")}}}}};1->showLocalList(LibraryStore.playlist("Podcast episodes"),"Podcast episodes","Podcast episodes");else->{val feed=feeds[i-2];io.execute{val r=runCatching{com.abhi.ganasuno.data.PodcastFeed.fetch(feed.url)};ui.post{if(!isDestroyed)r.onSuccess{showEpisodes(feed.title,it)}.onFailure{toast(it.message?:"Feed unavailable")}}}}}}}
    private fun showEpisodes(title:String,episodes:List<com.abhi.ganasuno.data.PodcastFeed.Episode>){choose(title,episodes.map{it.title+if(it.published.isNotBlank())" · ${it.published}"else ""}){n->val ep=episodes[n];MaterialAlertDialogBuilder(this).setTitle(ep.title).setMessage("Save this public audio enclosure only if you have permission. Direct HTTPS audio, max 100 MB; redirects not supported.").setNegativeButton("Cancel",null).setPositiveButton("Save and play"){_,_->offlineTask(true){token->com.abhi.ganasuno.data.OfflineFiles.fetch(this,ep.url,token).also{track->LibraryStore.create("Podcast episodes");LibraryStore.add("Podcast episodes",track)}}}.show()}}
    private fun playlistLinkMenu(){val refs=LibraryStore.collections().filter{it.kind=="Playlist"};choose("Share a saved playlist link",refs.map{it.title}){i->share(refs[i].title+"\n"+refs[i].url)}}
    private fun pruneExplicitQueue(){val c=controller?:return;for(i in c.mediaItemCount-1 downTo 0){if(!LibraryStore.allowed(c.getMediaItemAt(i).mediaId))c.removeMediaItem(i)};saveQueue()}
    private fun curatedDiscovery(){val names=listOf("Acoustic unwind","Hindi throwbacks","Instrumental focus","Indie discovery","Workout energy","Late night jazz");val queries=listOf("acoustic chill songs","classic Hindi songs","instrumental focus music","indie music songs","workout upbeat songs","late night jazz songs");choose("Curated themes · search-based",names+"Browse public playlists by theme (API key)"){n->if(n==names.size){publicCatalogue.featured();return@choose};val name="Discover · ${names[n]}";toast("Finding tracks…");io.execute{val result=runCatching{YtApi.search(queries[n]).filter{LibraryStore.allowed(it.id)}.distinctBy{it.id}.take(25)};ui.post{if(!isDestroyed)result.onSuccess{tracks->if(tracks.isEmpty())toast("No results")else{LibraryStore.create(name);tracks.forEach{LibraryStore.add(name,it)};showLocalList(LibraryStore.playlist(name),name,name)}}.onFailure{toast(it.message?:"Discovery failed")}}}}}
    private fun relatedArtists(initial:String){choose("Related artist source",listOf("My listening connections","Last.fm similar artists · API key required")){source->if(source==0)localRelatedArtists(initial)else ask("Artist name",initial){artist->
        if(artist.isBlank()||artist.length>200){toast("Enter an artist name");return@ask}
        val field=EditText(this).apply{hint="Last.fm API key";isSaveEnabled=false;inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD}
        MaterialAlertDialogBuilder(this).setTitle("Fetch similar artists from Last.fm").setMessage("Sends the artist name and your API key to Last.fm. The key is not saved. Results are provider similarity estimates.").setView(field).setNegativeButton("Cancel",null).setPositiveButton("Fetch"){_,_->val key=field.text.toString().trim();field.text.clear();if(!key.matches(Regex("[a-fA-F0-9]{32}")))toast("Invalid API key format")else io.execute{val result=runCatching{com.abhi.ganasuno.data.SimilarArtists.fetch(artist.trim(),key)};ui.post{if(!isDestroyed&&!isFinishing)result.onSuccess{rows->choose("Last.fm · similar artists",rows.map{it.name+" · "+(it.score*100).toInt()+"% similarity"}){n->doSearch(rows[n].name+" songs")}}.onFailure{toast("Last.fm unavailable; check key, network or quota")}}}}.show()
    }}}
    private fun localRelatedArtists(initial:String){ask("Artist name · local listening connections",initial){artist->val groups=LibraryStore.names().map{LibraryStore.playlist(it).map{track->track.artist}}+LibraryStore.history().groupBy{java.time.Instant.ofEpochMilli(it.at).atZone(java.time.ZoneId.systemDefault()).toLocalDate()}.values.map{day->day.map{it.song.artist}};val related=com.abhi.ganasuno.data.Batch17.related(artist,groups);if(related.isEmpty())toast("No related artists in your playlists or listening days yet")else choose("Artists appearing together locally",related){n->doSearch("${related[n]} songs")}}}
    private fun collectionMenu(kind:String,youtubeOnly:Boolean){val refs=LibraryStore.collections().filter{it.kind==kind&&(!youtubeOnly||com.abhi.ganasuno.data.V8Core.playlistId(it.url)!=null)};choose("Saved ${kind.lowercase()}s · offline bookmarks",listOf("+ Save a link")+refs.map{it.title}){n->if(n==0)ask("$kind title"){title->ask(if(youtubeOnly)"YouTube playlist URL or ID" else "HTTPS collection URL"){raw->val url=if(youtubeOnly)com.abhi.ganasuno.data.V8Core.playlistId(raw)?.let{"https://www.youtube.com/playlist?list=$it"}else com.abhi.ganasuno.data.V8Core.httpsUrl(raw);if(url==null)toast("Invalid link")else runCatching{LibraryStore.saveCollection(kind,title.trim(),url)}.onSuccess{collectionMenu(kind,youtubeOnly)}.onFailure{toast("Enter a title up to 120 characters")}}}else{val ref=refs[n-1];choose(ref.title,listOf("Open online in browser","Share link","Remove saved reference","Browse collection in app")){a->when(a){0->openHttps(ref.url);1->share(ref.title+"\n"+ref.url);2->{LibraryStore.deleteCollection(ref);collectionMenu(kind,youtubeOnly)};3->{val id=com.abhi.ganasuno.data.V8Core.playlistId(ref.url);if(kind=="Playlist"&&id!=null)publicCatalogue.playlist(id,ref.title)else if(kind=="Podcast"){toast("Fetching public RSS…");io.execute{val result=runCatching{com.abhi.ganasuno.data.PodcastFeed.fetch(ref.url)};ui.post{if(!isDestroyed)result.onSuccess{showEpisodes(ref.title,it)}.onFailure{toast("RSS unavailable")}}}}else toast("No supported in-app catalogue for this collection; use browser")}}}}}}
    private fun openHttps(raw:String){val url=com.abhi.ganasuno.data.V8Core.httpsUrl(raw)?:return;runCatching{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}.onFailure{toast("No browser available")}}
    private fun supportLinks(){val prefs=getSharedPreferences("project_links",MODE_PRIVATE);val names=listOf("AndroidX Media3 documentation","Stream extractor upstream","LRCLIB provider","Open saved developer blog","Open saved support link","Set developer blog URL","Set support URL","Read bundled dependency notices","Read setup and troubleshooting guide","Read bundled developer blog","Read resolved dependency inventory");choose("Documentation and external support",names){n->when(n){0->openHttps("https://developer.android.com/media/media3");1->openHttps("https://github.com/maxrave-dev/BravePipeExtractor");2->openHttps("https://lrclib.net/");3,4->{val url=prefs.getString(if(n==3)"blog" else "support","").orEmpty();if(url.isBlank())toast("Set a project link first")else openHttps(url)};7,8,9,10->{val file=when(n){7->"dependency_notices.txt";8->"setup_help.txt";9->"developer_blog.txt";else->"resolved_dependencies.txt"};val body=runCatching{assets.open(file).bufferedReader().use{it.readText()}}.getOrDefault("Document unavailable");MaterialAlertDialogBuilder(this).setTitle(names[n]).setMessage(body).setPositiveButton("Close",null).show()};5,6->{val key=if(n==5)"blog" else "support";ask("HTTPS URL · blank clears",prefs.getString(key,"").orEmpty()){raw->val url=com.abhi.ganasuno.data.V8Core.httpsUrl(raw);if(raw.isBlank())prefs.edit().remove(key).apply()else if(url==null)toast("Invalid HTTPS link")else prefs.edit().putString(key,url).apply()}}}}}
    private fun artworkQueue(){val c=controller?:return toast("Player is connecting; try again");val layout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,12,20,12)};val scroll=ScrollView(this).apply{addView(layout)};val dialog=MaterialAlertDialogBuilder(this).setTitle("Playing next").setView(scroll).setPositiveButton("Close",null).create();val current=c.currentMediaItemIndex;val timeline=c.currentTimeline;val ahead=com.abhi.ganasuno.data.Batch17.upcoming(c.mediaItemCount,current){i->timeline.getNextWindowIndex(i,if(c.repeatMode==androidx.media3.common.Player.REPEAT_MODE_ALL)androidx.media3.common.Player.REPEAT_MODE_ALL else androidx.media3.common.Player.REPEAT_MODE_OFF,c.shuffleModeEnabled)}.toSet();val order=com.abhi.ganasuno.data.Batch17.queueOrder(c.mediaItemCount,current){i->timeline.getNextWindowIndex(i,if(c.repeatMode==androidx.media3.common.Player.REPEAT_MODE_ALL)androidx.media3.common.Player.REPEAT_MODE_ALL else androidx.media3.common.Player.REPEAT_MODE_OFF,c.shuffleModeEnabled)};order.take(500).forEach{index->val song=fromMediaItem(c.getMediaItemAt(index));val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL;setPadding(0,8,0,8)};val img=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP};val size=(50*resources.displayMetrics.density).toInt();row.addView(img,LinearLayout.LayoutParams(size,size));ImageLoader.load(song.thumb,img,R.drawable.ic_placeholder);val label=TextView(this).apply{text=(if(index==current)"NOW PLAYING\n" else if(index !in ahead)"EARLIER / OUTSIDE UP NEXT\n" else "UP NEXT\n")+song.title+"\n"+song.artist;setPadding(16,0,8,0);maxLines=3};row.addView(label,LinearLayout.LayoutParams(0,-2,1f));row.setOnClickListener{if(index<c.mediaItemCount&&c.getMediaItemAt(index).mediaId==song.id){dialog.dismiss();choose(song.title,listOf("Play now","Move up in queue","Move down in queue","Remove","Move to play next (shuffle off)")){n->if(index>=c.mediaItemCount||c.getMediaItemAt(index).mediaId!=song.id){toast("Queue changed");return@choose};when(n){0->{c.seekTo(index,0);c.play()};1->if(index>0)c.moveMediaItem(index,index-1);2->if(index<c.mediaItemCount-1)c.moveMediaItem(index,index+1);3->c.removeMediaItem(index);4->{val now=c.currentMediaItemIndex;val target=com.abhi.ganasuno.data.Batch17.nextPosition(index,now,c.mediaItemCount);if(c.shuffleModeEnabled||target==null)toast("Disable shuffle and choose another track")else c.moveMediaItem(index,target)}};saveQueue();artworkQueue()}}else toast("Queue changed")};layout.addView(row)};if(order.isEmpty())layout.addView(TextView(this).apply{text="Queue is empty"});dialog.show()}
    private fun decadeMenu(){val h=LibraryStore.history();if(h.isEmpty()){toast("No listening history yet");return};val years=LibraryStore.releaseYears();val totals=com.abhi.ganasuno.data.Batch13.decades(h,years);val known=totals.keys.filterNotNull().sorted();val text=buildString{appendLine("Based on saved release years and actual listening time.");known.forEach{d->appendLine("${d}s · ${totals[d]!!/60000} min")};appendLine("Unknown release year · ${(totals[null]?:0L)/60000} min");appendLine("Scan local audio tags or set a release year manually. For online tracks, choose Find release year online in song actions.")};MaterialAlertDialogBuilder(this).setTitle("Music by decade").setMessage(text).setNeutralButton("Share image"){_,_->advanced.card("Music by decade",text)}.setNegativeButton("Scan local tags"){_,_->scanLocalYears(h.map{it.song}.distinctBy{it.id})}.setPositiveButton("Close",null).show()}
    private fun scanLocalYears(tracks:List<Song>){toast("Reading local audio metadata…");io.execute{var found=0;tracks.filter{it.localUri.startsWith("content://")&&LibraryStore.releaseYear(it.id)==null}.take(500).forEach{song->runCatching{val r=MediaMetadataRetriever();try{r.setDataSource(this,Uri.parse(song.localUri));val year=com.abhi.ganasuno.data.Batch13.year(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR))?:com.abhi.ganasuno.data.Batch13.year(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE));if(year!=null&&LibraryStore.releaseYear(song.id)==null){LibraryStore.setReleaseYear(song.id,year);found++}}finally{r.release()}}};ui.post{if(!isDestroyed){toast("Found $found tagged release years");decadeMenu()}}}}
    private fun analyzeTrack(song:Song){
        if(song.live){toast("Live streams cannot be analyzed");return}
        if(song.localUri.isBlank()){
            com.abhi.ganasuno.data.LocalTrackAnalyzer.cached(song)?.let{showTrackAnalysis(song,it);return}
            MaterialAlertDialogBuilder(this).setTitle("Analyze online audio?").setMessage("This explicitly decodes a seekable stream to estimate its waveform, tempo and key. It may use mobile data and fail on unsupported streams. Only a numeric summary is cached.").setNegativeButton("Cancel",null).setPositiveButton("Analyze"){_,_->runAnalysis(song,true)}.show()
        }else runAnalysis(song,false)
    }
    private fun runAnalysis(song:Song,online:Boolean){
        if(analysisBusy){toast("Another track is being analyzed");return};analysisBusy=true
        toast("Analyzing audio…")
        analysisWorker.execute{val r=runCatching{com.abhi.ganasuno.data.LocalTrackAnalyzer.analyze(this,song,online)};ui.post{analysisBusy=false;if(!isDestroyed&&!isFinishing)r.onSuccess{showTrackAnalysis(song,it)}.onFailure{toast("Audio analysis failed or unsupported")}}}
    }
    private fun editMusicTags(song:Song,estimate:Int?,estimatedKey:String?,done:()->Unit){
        val tags=LibraryStore.musicTags(song.id);val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,12,24,12)}
        val bpm=EditText(this).apply{hint="BPM (20–300; blank clears)";inputType=android.text.InputType.TYPE_CLASS_NUMBER;setText((tags.bpm?:estimate)?.toString().orEmpty())};box.addView(bpm)
        val key=EditText(this).apply{hint="Key and scale e.g. C major or D dorian";setSingleLine();setText(tags.key?:estimatedKey.orEmpty())};box.addView(key)
        val dialog=MaterialAlertDialogBuilder(this).setTitle("Personal music tags · not verified metadata").setView(box).setNegativeButton("Cancel",null).setNeutralButton("Clear"){_,_->LibraryStore.setMusicTags(song.id,null,null);done()}.setPositiveButton("Save",null).create();dialog.show()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener{val b=bpm.text.toString();val k=key.text.toString();val bv=com.abhi.ganasuno.data.Batch19.bpm(b);val kv=com.abhi.ganasuno.data.Batch19.key(k);if(b.isNotBlank()&&bv==null)bpm.error="Use 20–300" else if(k.isNotBlank()&&kv==null)key.error="Use e.g. C major, A minor, D dorian" else {LibraryStore.setMusicTags(song.id,bv,kv);done();dialog.dismiss()}}
    }
    private fun showTrackAnalysis(song:Song,track:com.abhi.ganasuno.data.LocalTrackAnalyzer.Track){
        val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,16,24,16)}
        val duration=track.duration.coerceAtLeast(1L);val analysis=track.analysis
        val detail=TextView(this).apply{text="${song.artist}\n${track.mime} · ${track.rate} Hz · ${track.channels} ch\nBitrate: ${track.bitrate?.let{"${it/1000} kbps"}?:"Unknown"}\nEstimated BPM: ${analysis.bpm?:"Uncertain"} · Key: ${analysis.key?:"Uncertain"}\nTempo/key use first 90 seconds; estimates may be wrong."};panel.addView(detail)
        val tagView=TextView(this);panel.addView(tagView)
        fun refreshTags(){val t=LibraryStore.musicTags(song.id);tagView.text="Saved BPM: ${t.bpm?:"None"} · Key: ${t.key?:"None"} · Scale: ${com.abhi.ganasuno.data.Batch19.scale(t.key?:analysis.key)?:"Unknown"}"};refreshTags()
        panel.addView(Button(this).apply{text="Choose estimated tempo / half-time / double-time";setOnClickListener{val estimates=analysis.tempoCandidates;val options=(com.abhi.ganasuno.data.Batch22.bpmAlternatives(analysis.bpm)+estimates.map{it.bpm}).distinct();choose("Tempo candidates · verify by ear",options.map{bpm->"$bpm BPM"+(estimates.firstOrNull{it.bpm==bpm}?.let{" · onset correlation %.2f".format(it.correlation)}?:"")}){i->LibraryStore.setMusicTags(song.id,options[i],LibraryStore.musicTags(song.id).key?:com.abhi.ganasuno.data.Batch19.key(analysis.key.orEmpty()));refreshTags()}}})
        panel.addView(Button(this).apply{text="Edit BPM / key / scale tags";setOnClickListener{editMusicTags(song,analysis.bpm,analysis.key){refreshTags()}}})
        fun seek(ms:Long){val c=controller;if(c?.currentMediaItem?.mediaId==song.id)c.seekTo(ms.coerceIn(0L,duration))else toast("Play this track first")}
        val wave=com.abhi.ganasuno.ui.TrackWaveform(this,analysis.peaks){f->seek((f*duration).toLong())};panel.addView(wave,LinearLayout.LayoutParams(-1,(100*resources.displayMetrics.density).toInt()))
        val slider=SeekBar(this).apply{max=(duration/1000).toInt().coerceAtLeast(1)};panel.addView(slider)
        var seeking=false;slider.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(b:SeekBar?,p:Int,fromUser:Boolean){};override fun onStartTrackingTouch(b:SeekBar?){seeking=true};override fun onStopTrackingTouch(b:SeekBar?){seek((b?.progress?:0)*1000L);seeking=false}})
        val notes=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};panel.addView(notes)
        fun refreshMarkers(){val items=LibraryStore.markers(song.id);wave.markers=items.map{it.ms.toFloat()/duration};notes.removeAllViews();items.forEach{m->notes.addView(Button(this).apply{text="${formatTime(m.ms)} ${m.reaction} ${m.text}";setOnClickListener{choose("Timestamp note",listOf("Seek to note","Delete note")){n->if(n==0)seek(m.ms)else {LibraryStore.removeMarker(song.id,m);refreshMarkers()}}}})}}
        panel.addView(Button(this).apply{text="Play this track";setOnClickListener{if(controller?.currentMediaItem?.mediaId!=song.id)startQueue(listOf(song),0)else controller?.play()}})
        panel.addView(Button(this).apply{text="Add timestamp note / reaction";setOnClickListener{val c=controller;if(c?.currentMediaItem?.mediaId!=song.id){toast("Play this track first");return@setOnClickListener};val ms=c.currentPosition;ask("Note at ${formatTime(ms)}"){note->choose("Reaction",listOf("None","♥","🔥","★")){i->runCatching{LibraryStore.addMarker(song.id,ms,note,listOf("","♥","🔥","★")[i])}.onSuccess{refreshMarkers()}.onFailure{toast(it.message?:"Cannot save marker")}}}}})
        panel.addView(Button(this).apply{text="Clear private timestamp notes";setOnClickListener{MaterialAlertDialogBuilder(this@MainActivity).setTitle("Delete all markers for this track?").setNegativeButton("Cancel",null).setPositiveButton("Delete"){_,_->LibraryStore.clearMarkers(song.id);refreshMarkers()}.show()}})
        refreshMarkers();val dialog=MaterialAlertDialogBuilder(this).setTitle(song.title).setView(ScrollView(this).apply{addView(panel)}).setPositiveButton("Close",null).create()
        val tick=object:Runnable{override fun run(){if(!dialog.isShowing)return;val c=controller;val ms=if(c?.currentMediaItem?.mediaId==song.id)c.currentPosition else 0L;wave.progress=ms.toFloat()/duration;if(!seeking)slider.progress=(ms/1000).toInt().coerceIn(0,slider.max);ui.postDelayed(this,350)}};dialog.setOnDismissListener{ui.removeCallbacks(tick)};dialog.show();ui.post(tick)
    }
    private fun showCrashReport(raw:String){
        val report=com.abhi.ganasuno.data.Batch19.safeReport(raw)
        if(report.isBlank()){toast("No readable crash report");return}
        val dialog=MaterialAlertDialogBuilder(this).setTitle("Previous app crash · local report").setMessage(report).setNegativeButton("Keep",null).setNeutralButton("Share report"){_,_->share(report)}.setPositiveButton("Delete"){_,_->com.abhi.ganasuno.data.CrashRecord.clear(this)}
        if(com.abhi.ganasuno.data.CrashUpload.available)dialog.setNeutralButton("Share / send"){_,_->choose("Crash report",listOf("Share locally","Review and send to your Sentry project")){n->if(n==0)share(report)else sendCrashReport(report)}}
        dialog.show()
    }
    private fun sendCrashReport(report:String){
        if(!com.abhi.ganasuno.data.CrashUpload.available)return
        val input=EditText(this).apply{hint="HTTPS Sentry project DSN";isSaveEnabled=false;inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI}
        MaterialAlertDialogBuilder(this).setTitle("Your trusted Sentry project").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Review"){_,_->
            val dsn=input.text.toString().trim();input.text.clear();val dest=com.abhi.ganasuno.data.Batch19.destination(dsn)
            if(dest==null)toast("Invalid HTTPS Sentry DSN")else MaterialAlertDialogBuilder(this).setTitle("Send once to ${dest.host}?").setMessage("The following crash summary will be sent; no listening history or account data is included. The receiving server can see your network address.\n\n$report").setNegativeButton("Cancel",null).setPositiveButton("Send report"){_,_->io.execute{val result=runCatching{com.abhi.ganasuno.data.CrashUpload.send(dsn,report)};ui.post{if(!isDestroyed&&!isFinishing)result.onSuccess{toast("Report accepted: $it")}.onFailure{toast("Report was not accepted; local copy retained")}}}}.show()
        }.show()
    }
    private fun diagnostics(){
        val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,12,20,12)}
        val filter=android.widget.EditText(this).apply{hint="Filter error code or date";setSingleLine()};panel.addView(filter)
        val live=android.widget.CheckBox(this).apply{text="Live refresh";isChecked=true};panel.addView(live)
        val output=TextView(this).apply{setTextIsSelectable(true)};panel.addView(ScrollView(this).apply{addView(output)},LinearLayout.LayoutParams(-1,(300*resources.displayMetrics.density).toInt()))
        fun filtered()=com.abhi.ganasuno.data.Diagnostics.read().lines().filter{it.contains(filter.text.toString(),true)}.joinToString("\n")
        fun refresh(){output.text=filtered().ifBlank{"No matching app events"}}
        filter.addTextChangedListener(object:android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){};override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){refresh()};override fun afterTextChanged(s:android.text.Editable?){}})
        val dialog=MaterialAlertDialogBuilder(this).setTitle("Private app diagnostics").setView(panel).setNegativeButton("Close",null).setNeutralButton("Export"){_,_->share(filtered())}.setPositiveButton("Clear",null).create()
        val tick=object:Runnable{override fun run(){if(!dialog.isShowing)return;if(live.isChecked)refresh();ui.postDelayed(this,1000)}}
        dialog.setOnDismissListener{ui.removeCallbacks(tick)};dialog.show();dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener{com.abhi.ganasuno.data.Diagnostics.clear();refresh()};refresh();ui.post(tick)
    }

    private fun playerMore(fullscreen:Boolean){
        choose("Player options",listOf("Song actions","Audio settings","Advanced playback","Player style","Waveform and music tags","Video",if(fullscreen)"Exit fullscreen" else "Fullscreen")){i->when(i){
            0->controller?.currentMediaItem?.let{songActions(fromMediaItem(it),fromPlayer=true)}
            1->choose("Audio settings",listOf("Audio quality","Device equalizer","Rotation and delay parameters","Audio output")){n->when(n){0->SettingsDialog.show(this,includeSleepTimer=false);1->com.abhi.ganasuno.ui.EqualizerDialog.show(this);2->dspMenu(includeRotation=false);3->outputPicker()}}
            2->playbackControls(advancedOnly=true)
            3->advanced.menuAction(1)
            4->controller?.currentMediaItem?.let{analyzeTrack(fromMediaItem(it))}
            5->openVideo()
            6->{currentPlayerDialog?.dismiss();nowPlaying(!fullscreen)}
        }}
    }
    private var currentPlayerDialog:androidx.appcompat.app.AlertDialog?=null
    private fun nowPlaying(fullscreen:Boolean=false){val c=controller?:return toast("Player not ready");val song=c.currentMediaItem?.let{fromMediaItem(it)}?:return toast("Play a song first");currentPlayerDialog?.dismiss();val style=LibraryStore.option("player_style",0).coerceIn(0,3);val layout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(16));background=com.abhi.ganasuno.ui.GlassFeatureStyle.ambient(this@MainActivity)};val cover=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP;background=android.graphics.drawable.GradientDrawable().apply{setColor(0xff202024.toInt());cornerRadius=dp(22).toFloat()};clipToOutline=true};layout.addView(cover,LinearLayout.LayoutParams(-1,dp((minOf(resources.configuration.screenWidthDp-48,resources.configuration.screenHeightDp/2)).coerceIn(144,420))));cover.contentDescription="Album artwork. Swipe horizontally to change track";var coverDownX=0f;var coverDownY=0f;cover.setOnTouchListener{v,event->when(event.actionMasked){MotionEvent.ACTION_DOWN->{coverDownX=event.rawX;coverDownY=event.rawY;v.parent.requestDisallowInterceptTouchEvent(true);true};MotionEvent.ACTION_UP->{v.parent.requestDisallowInterceptTouchEvent(false);val dx=event.rawX-coverDownX;val dy=event.rawY-coverDownY;if(kotlin.math.abs(dx)>dp(48)&&kotlin.math.abs(dx)>kotlin.math.abs(dy)){if(dx>0)skipTrack(false)else skipTrack(true)}else if(fullscreen&&dy>dp(64)&&kotlin.math.abs(dy)>kotlin.math.abs(dx))currentPlayerDialog?.dismiss() else v.performClick();true};MotionEvent.ACTION_CANCEL->{v.parent.requestDisallowInterceptTouchEvent(false);true};else->true}};ImageLoader.load(song.thumb,cover,R.drawable.ic_placeholder);val title=TextView(this).apply{textSize=26f;text=song.title;setTypeface(null,android.graphics.Typeface.BOLD);setPadding(0,dp(16),0,0)};layout.addView(title);val artist=TextView(this).apply{text=song.artist;textSize=16f;alpha=.75f};layout.addView(artist);val time=TextView(this);layout.addView(time);val bar:SeekBar=if(style==1)com.abhi.ganasuno.ui.WavyProgress(this)else SeekBar(this);layout.addView(bar);var seeking=false;bar.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(b:SeekBar?,p:Int,f:Boolean){};override fun onStartTrackingTouch(b:SeekBar?){seeking=true};override fun onStopTrackingTouch(b:SeekBar?){c.seekTo((b?.progress?:0)*1000L);seeking=false}});val controls=LinearLayout(this).apply{gravity=android.view.Gravity.CENTER_VERTICAL}
        fun button(parent:LinearLayout,label:String,action:()->Unit):Button{
            val v=Button(this).apply{text=label;isAllCaps=false;minHeight=dp(52);minimumWidth=0;textSize=13f;setPadding(dp(4),dp(8),dp(4),dp(8));backgroundTintList=null;background=com.abhi.ganasuno.ui.GlassFeatureStyle.surface(this@MainActivity,18);foreground=com.abhi.ganasuno.ui.GlassFeatureStyle.ripple(this@MainActivity,18);setTextColor(com.abhi.ganasuno.ui.GlassFeatureStyle.primary(this@MainActivity));setOnClickListener{action()}}
            parent.addView(v,LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(4);marginEnd=dp(4);topMargin=dp(4);bottomMargin=dp(4)});return v
        }
        fun transport(icon:Int,label:String,large:Boolean=false,action:()->Unit):android.widget.ImageButton{
            val v=android.widget.ImageButton(this).apply{contentDescription=label;setImageResource(icon);imageTintList=android.content.res.ColorStateList.valueOf(com.abhi.ganasuno.ui.GlassFeatureStyle.primary(this@MainActivity));scaleType=ImageView.ScaleType.FIT_CENTER;val inset=dp(if(large)16 else 14);setPadding(inset,inset,inset,inset);background=com.abhi.ganasuno.ui.GlassFeatureStyle.surface(this@MainActivity,32);foreground=com.abhi.ganasuno.ui.GlassFeatureStyle.ripple(this@MainActivity,32);setOnClickListener{action()}}
            controls.addView(v,LinearLayout.LayoutParams(0,dp(if(large)64 else 54),1f).apply{marginStart=dp(8);marginEnd=dp(8);topMargin=dp(6);bottomMargin=dp(6)});return v
        }
        transport(R.drawable.ic_prev,"Previous track"){skipTrack(false)}
        val play=transport(R.drawable.ic_play,"Play",true){togglePlayback(c)}
        transport(R.drawable.ic_next,"Next track"){skipTrack(true)}
        val loading=TextView(this).apply{text="Loading audio…";textSize=12f;gravity=android.view.Gravity.CENTER;visibility=View.GONE;setTextColor(com.abhi.ganasuno.ui.GlassFeatureStyle.secondary(this@MainActivity))};layout.addView(loading)
        val quickTitle=TextView(this).apply{text="Quick audio controls";textSize=14f;setPadding(dp(4),dp(12),0,dp(4));setTextColor(com.abhi.ganasuno.ui.GlassFeatureStyle.primary(this@MainActivity))};layout.addView(quickTitle)
        val quickAudio=LinearLayout(this);layout.addView(quickAudio)
        val rotationButton=button(quickAudio,"8D Audio"){LibraryStore.option("rotate",if(LibraryStore.option("rotate")==1)0 else 1,true);com.abhi.ganasuno.player.EffectsSettings.refresh()}
        val eqButton=button(quickAudio,"Equalizer"){com.abhi.ganasuno.ui.TenBandDialog.show(this)}
        val timerButton=button(quickAudio,"Sleep Timer"){com.abhi.ganasuno.ui.SettingsDialog.showSleepTimer(this)}
        fun quickIcon(button:Button,icon:Int){
            ContextCompat.getDrawable(this,icon)?.mutate()?.let{drawable->
                drawable.setTint(com.abhi.ganasuno.ui.GlassFeatureStyle.primary(this));drawable.setBounds(0,0,dp(23),dp(23));button.setCompoundDrawables(null,drawable,null,null);button.compoundDrawablePadding=dp(4)
            }
        }
        quickIcon(rotationButton,R.drawable.glass_feature_headphones);quickIcon(eqButton,R.drawable.glass_feature_sliders);quickIcon(timerButton,R.drawable.glass_feature_clock)
        fun updateLabel(button:Button,label:String){if(button.text.toString()!=label)button.text=label}
        fun syncQuickAudio(){
            updateLabel(rotationButton,"8D Audio\n${if(LibraryStore.option("rotate")==1)"On ✓" else "Off"}")
            updateLabel(eqButton,"Equalizer\n${if(LibraryStore.option("dsp_eq")==1)"On ✓" else "Off"}")
            updateLabel(timerButton,"Sleep Timer\n${com.abhi.ganasuno.ui.SettingsDialog.sleepLabel()}")
            rotationButton.isSelected=LibraryStore.option("rotate")==1
            rotationButton.contentDescription="8D stereo rotation, saved preference ${if(rotationButton.isSelected)"On" else "Off"}"
        }
        syncQuickAudio()
        val extra=LinearLayout(this)
        val favourite=android.widget.ImageButton(this).apply{
            minimumHeight=dp(48);setPadding(dp(12),dp(12),dp(12),dp(12));scaleType=ImageView.ScaleType.CENTER_INSIDE
            background=com.abhi.ganasuno.ui.GlassFeatureStyle.surface(this@MainActivity,16);foreground=com.abhi.ganasuno.ui.GlassFeatureStyle.ripple(this@MainActivity,16)
        }
        extra.addView(favourite,LinearLayout.LayoutParams(0,dp(48),1f).apply{marginStart=dp(4);marginEnd=dp(4)})
        fun setFavourite(saved:Boolean){favourite.isSelected=saved;favourite.setImageResource(if(saved)R.drawable.player_heart_filled else R.drawable.player_heart_outline);favourite.setColorFilter(if(saved)0xfff4c2c2.toInt() else if(darkGlass)0xffc4c4c4.toInt() else 0xff636366.toInt());favourite.contentDescription=if(saved)"Remove from favourites" else "Add to favourites"}
        fun syncFavourite(){setFavourite(LibraryStore.list("favorites").any{it.id==c.currentMediaItem?.mediaId})}
        syncFavourite();favourite.setOnClickListener{c.currentMediaItem?.let{val saved=LibraryStore.toggleFavorite(fromMediaItem(it));setFavourite(saved);toast(if(saved)"Saved" else "Removed")}}
        button(extra,"Queue"){artworkQueue()};button(extra,"Lyrics"){c.currentMediaItem?.let{lyricsDialog(fromMediaItem(it))}}
        val modes=LinearLayout(this);val shuffleButton=button(modes,"Shuffle"){c.shuffleModeEnabled=!c.shuffleModeEnabled;LibraryStore.option("shuffle",if(c.shuffleModeEnabled)1 else 0,true)};val repeatButton=button(modes,"Repeat"){c.repeatMode=(c.repeatMode+1)%3;LibraryStore.option("repeat",c.repeatMode,true)}
        val tools=LinearLayout(this);button(tools,"More options"){playerMore(fullscreen)}
        val waveHolder=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;visibility=if(style==3)View.VISIBLE else View.GONE};layout.addView(waveHolder,layout.indexOfChild(bar)+1);var inlineWave:com.abhi.ganasuno.ui.TrackWaveform?=null;var waveGeneration=0;var waveId="";val mesh=if(style==2)com.abhi.ganasuno.ui.MeshBackdrop().apply{track(song.id)}else null;when(style){1->{title.textSize=28f;cover.scaleType=ImageView.ScaleType.CENTER_CROP;bar.progressTintList=android.content.res.ColorStateList.valueOf(0xffa78bfa.toInt())};2->{layout.background=mesh;cover.scaleType=ImageView.ScaleType.CENTER_CROP;cover.background=android.graphics.drawable.GradientDrawable().apply{cornerRadius=24*resources.displayMetrics.density;setColor(0xff28283a.toInt())};cover.clipToOutline=true;title.textSize=29f;title.gravity=android.view.Gravity.CENTER;artist.gravity=android.view.Gravity.CENTER;title.setTextColor(android.graphics.Color.WHITE);artist.setTextColor(android.graphics.Color.WHITE);time.setTextColor(android.graphics.Color.WHITE);bar.progressTintList=android.content.res.ColorStateList.valueOf(0xfffff1f2.toInt())};3->{cover.layoutParams.height=(160*resources.displayMetrics.density).toInt();title.textSize=24f;bar.progressTintList=android.content.res.ColorStateList.valueOf(0xfff97316.toInt())}};if(fullscreen){val silver=android.content.res.ColorStateList.valueOf(if(darkGlass)0xffc4c4c4.toInt() else 0xff595959.toInt());bar.progressTintList=silver;bar.thumbTintList=silver};val config = resources.configuration
        val compactControls = config.screenHeightDp < 640 || config.fontScale > 1.3f
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = com.abhi.ganasuno.ui.GlassFeatureStyle.ambient(this@MainActivity)
        }
        if (style != 2) layout.background = null
        layout.setPadding(0, 0, 0, dp(8))
        val scroll = ScrollView(this).apply { addView(layout) }
        panel.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        panel.addView(controls, LinearLayout.LayoutParams(-1, -2))
        if (compactControls) {
            layout.addView(extra, 0)
            layout.addView(modes, 1)
            layout.addView(tools, 2)
        } else {
            for (row in listOf(extra, modes, tools)) panel.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setBackground(com.abhi.ganasuno.ui.GlassFeatureStyle.dialog(this))
            .setTitle("Now Playing")
            .setView(panel)
            .setPositiveButton("Close", null)
            .create();fun loadInlineWave(current:Song){
        if(style!=3||waveId==current.id)return
        waveId=current.id;val generation=++waveGeneration;inlineWave=null;waveHolder.removeAllViews();bar.visibility=View.VISIBLE
        val status=TextView(this).apply{text=if(current.localUri.isBlank())"Waveform unavailable for online tracks · standard seek bar" else "Decoding local waveform…"};waveHolder.addView(status)
        fun decode(online:Boolean){analysisWorker.execute{val result=runCatching{com.abhi.ganasuno.data.LocalTrackAnalyzer.analyze(this,current,online)};ui.post{
            if(!dialog.isShowing||generation!=waveGeneration||isDestroyed)return@post
            result.onSuccess{track->waveHolder.removeAllViews();val wave=com.abhi.ganasuno.ui.TrackWaveform(this,track.analysis.peaks){f->if(c.currentMediaItem?.mediaId==current.id&&c.isCurrentMediaItemSeekable)c.seekTo((f*track.duration).toLong())};inlineWave=wave;waveHolder.addView(wave,LinearLayout.LayoutParams(-1,(100*resources.displayMetrics.density).toInt()));bar.visibility=View.GONE;status.text="Audio waveform · tap, drag or use keyboard / accessibility to seek";waveHolder.addView(status);waveHolder.addView(Button(this).apply{text="Show standard seek bar instead";setOnClickListener{bar.visibility=if(bar.visibility==View.VISIBLE)View.GONE else View.VISIBLE;text=if(bar.visibility==View.VISIBLE)"Hide standard seek bar" else "Show standard seek bar instead"}});wave.markers=LibraryStore.markers(current.id).map{it.ms.toFloat()/track.duration.coerceAtLeast(1)} }.onFailure{status.text="Waveform unavailable · standard seek bar"}
        }}}
        if(current.localUri.isBlank()){status.text="Online waveform requires extra network data; seek bar remains available.";if(!current.live){if(com.abhi.ganasuno.data.LocalTrackAnalyzer.cached(current)!=null)decode(true)else waveHolder.addView(Button(this).apply{text="Analyze online waveform";setOnClickListener{isEnabled=false;status.text="Decoding stream…";decode(true)}})}}else decode(false)
    };var last="";val refresh=object:Runnable{override fun run(){if(!dialog.isShowing||controller!==c)return;val item=c.currentMediaItem;if(item!=null){val current=fromMediaItem(item);if(last!=current.id){last=current.id;syncFavourite();loadInlineWave(current);mesh?.track(current.id);title.text=current.title;artist.text=current.artist;(cover.drawable as? android.graphics.drawable.Animatable)?.stop();if(LibraryStore.option("animated_art")!=2||!com.abhi.ganasuno.ui.AnimatedCover.show(cover,current.id))ImageLoader.load(current.thumb,cover,R.drawable.ic_placeholder){bitmap->if(dialog.isShowing&&style!=2&&(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES)com.abhi.ganasuno.ui.PremiumUi.applyArtwork(panel,bitmap,0f)}}};if(LibraryStore.option("animated_art")==1&&c.isPlaying&&android.animation.ValueAnimator.areAnimatorsEnabled())cover.rotation=(cover.rotation+2f)%360f else if(LibraryStore.option("animated_art")==0)cover.rotation=0f;(cover.drawable as? android.graphics.drawable.Animatable)?.let{if(c.isPlaying&&!it.isRunning)it.start()else if(!c.isPlaying&&it.isRunning)it.stop()};if(c.isPlaying&&android.animation.ValueAnimator.areAnimatorsEnabled())mesh?.advance();val state=MiniPlaybackUi.resolve(c.currentMediaItem!=null,c.playWhenReady,c.playbackState==Player.STATE_BUFFERING,c.isPlaying || (c.playWhenReady&&c.playbackState==Player.STATE_READY),c.playerError!=null);if(play.tag!=state.showPause){play.tag=state.showPause;play.setImageResource(if(state.showPause)R.drawable.ic_pause else R.drawable.ic_play);play.contentDescription=if(state.showPause)"Pause" else "Play"};loading.visibility=if(state.showLoading)View.VISIBLE else View.GONE;syncQuickAudio();shuffleButton.isSelected=c.shuffleModeEnabled;shuffleButton.text=if(c.shuffleModeEnabled)"Shuffle on" else "Shuffle off";repeatButton.text=listOf("Repeat off","Repeat one","Repeat all")[c.repeatMode];time.text="${formatTime(c.currentPosition)} / ${formatTime(c.duration)}";inlineWave?.progress=com.abhi.ganasuno.data.Batch13.fraction(c.currentPosition,c.duration);if(!seeking){bar.max=(c.duration.coerceAtLeast(0L)/1000).coerceAtMost(Int.MAX_VALUE.toLong()).toInt();bar.progress=(c.currentPosition/1000).coerceIn(0L,bar.max.toLong()).toInt()};ui.postDelayed(this,400)}};dialog.setOnDismissListener{(cover.drawable as? android.graphics.drawable.Animatable)?.stop();cover.tag=null;waveGeneration++;ui.removeCallbacks(refresh);if(currentPlayerDialog===dialog)currentPlayerDialog=null};dialog.show();currentPlayerDialog=dialog;dialog.window?.let { window ->
            window.setLayout(if (fullscreen) -1 else window.attributes.width, if (fullscreen) -1 else dp((config.screenHeightDp - 32).coerceAtLeast(1)))
        };ui.post(refresh)}
    private fun lyricsDialog(song:Song){val saved=LibraryStore.lyric(song.id);choose("Lyrics · ${song.title}",listOf("Show saved lyrics","Fetch using preferred provider","Add or edit local LRC","Timing offset","More lyrics tools","Choose lyrics provider")){i->when(i){0->if(saved.isBlank())toast("No saved lyrics")else showLyrics(song,saved);1->{toast("Fetching lyrics");io.execute{val r=runCatching{Lyrics.fetch(song,LibraryStore.option("lyrics_provider",0))};r.onSuccess{LibraryStore.setLyric(song.id,it)};ui.post{if(!isDestroyed)r.onSuccess{showLyrics(song,it)}.onFailure{toast(it.message?:"Lyrics unavailable")}}}};2->editLyrics(song,saved);3->ask("Offset in milliseconds",LibraryStore.option("lyric_offset").toString()){v->val n=v.toIntOrNull();if(n!=null&&n in -30000..30000)LibraryStore.option("lyric_offset",n,true)else toast("Enter -30000 to 30000")};4->if(saved.isBlank())toast("Save lyrics first")else advanced.lyrics(song,saved);5->choose("Preferred lyrics provider · online providers receive artist and title",Lyrics.providers){n->LibraryStore.option("lyrics_provider",n,true);lyricsDialog(song)}}}}
    private fun editLyrics(song:Song,raw:String){
        val field=EditText(this).apply{setText(raw);minLines=6;maxLines=15;hint="[00:01.000]Line or [00:01]<00:01>Word";gravity=android.view.Gravity.TOP;inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE}
        val d=MaterialAlertDialogBuilder(this).setTitle("Local lyrics editor").setMessage("Plain lyrics, LRC, or enhanced LRC. Check validates timestamps before saving.").setView(field).setNegativeButton("Cancel",null).setNeutralButton("Check",null).setPositiveButton("Save",null).create();d.show()
        fun check(save:Boolean){val text=field.text.toString();val error=com.abhi.ganasuno.ui.WordLrc.error(text);field.error=error;if(error==null){if(save){LibraryStore.setLyric(song.id,text);d.dismiss();showLyrics(song,text)}else toast("Lyrics valid")}}
        d.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener{check(false)};d.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener{check(true)}
    }
    private fun showLyrics(song:Song,raw:String){
        val panel=com.abhi.ganasuno.ui.KaraokePanel(this,raw,
            position={controller?.takeIf{it.currentMediaItem?.mediaId==song.id}?.let{it.currentPosition+LibraryStore.option("lyric_offset")}},
            seek={ms->controller?.takeIf{it.currentMediaItem?.mediaId==song.id}?.seekTo((ms-LibraryStore.option("lyric_offset")).coerceAtLeast(0L))})
        val d=MaterialAlertDialogBuilder(this).setTitle(song.title).setView(panel).setPositiveButton("Close",null).create()
        val dark=(resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES
        d.setOnDismissListener{panel.stop()};d.show();d.window?.setLayout(-1,-1);panel.start()
        if(dark){val art=ImageView(this);ImageLoader.load(song.thumb,art,R.drawable.ic_placeholder){bitmap->if(d.isShowing)com.abhi.ganasuno.ui.PremiumUi.applyArtwork(panel,bitmap,0f)}}
    }

    companion object {
        private const val EXTRA_LIVE = "live"
    }
}
