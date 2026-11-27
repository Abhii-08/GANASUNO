package com.abhi.ganasuno.ui

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.abhi.ganasuno.data.Batch18
import com.abhi.ganasuno.data.RoomTiming
import com.abhi.ganasuno.data.LocalTrackAnalyzer
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.abhi.ganasuno.data.Song
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Explicit, foreground-only listening rooms. The server receives public track IDs, not audio or login cookies. */
class ListeningRooms(private val a:Activity,private val player:()->MediaController?,private val item:(Song)->MediaItem) {
    private val handler=Handler(Looper.getMainLooper())
    private val worker=Executors.newSingleThreadExecutor()
    private val waveWorker=Executors.newSingleThreadExecutor()
    private val client=OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).callTimeout(10,TimeUnit.SECONDS).build()
    private var generation=0
    private var stopped=false
    private var visible=true
    private var closeRoom:(()->Unit)?=null
    private data class Session(val url:String,val code:String,val token:String)
    private fun call(url:String,action:String,body:JSONObject):JSONObject {
        val base=url.toHttpUrlOrNull()?:error("Invalid server URL")
        require(base.isHttps && base.username.isEmpty() && base.password.isEmpty() && base.query==null && base.fragment==null){"Trusted HTTPS server required"}
        val target=base.newBuilder().addPathSegments("v1/$action").build()
        client.newCall(Request.Builder().url(target).post(body.toString().toRequestBody("application/json".toMediaType())).build()).execute().use { r ->
            check(r.isSuccessful){"Room request rejected (HTTP ${r.code})"}
            val b=r.body?:error("Empty server response");val source=b.source();source.request(131073);require(source.buffer.size<=131072){"Response too large"}
            return JSONObject(source.readUtf8())
        }
    }
    private fun identity(endpoint:String):String{val prefs=a.getSharedPreferences("room_identity",android.content.Context.MODE_PRIVATE);val key=java.security.MessageDigest.getInstance("SHA-256").digest(endpoint.toByteArray()).joinToString(""){"%02x".format(it.toInt() and 255)};return prefs.getString(key,null)?:ByteArray(32).also{java.security.SecureRandom().nextBytes(it)}.joinToString(""){"%02x".format(it.toInt() and 255)}.also{prefs.edit().putString(key,it).apply()}}
    private fun request(s:Session,action:String,body:JSONObject=JSONObject())=call(s.url,action,body.put("code",s.code).put("token",s.token))
    private fun toast(text:String)=android.widget.Toast.makeText(a,text,android.widget.Toast.LENGTH_LONG).show()
    fun open(){
        if(stopped)return
        closeRoom?.invoke()
        val panel=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL;setPadding(28,12,28,12)}
        val url=EditText(a).apply{hint="https://your-room-server.example/";setSingleLine()}
        val name=EditText(a).apply{hint="Display name";setSingleLine()}
        val code=EditText(a).apply{hint="16-character room code (join only)";setSingleLine()}
        panel.addView(TextView(a).apply{text="Use a trusted self-hosted GANASUNO room server. Your display name, public track IDs and playback position are shared with approved members. Optional timestamp comments and reactions are shared only inside this room. A random per-server installation token is sent to the server for room blocking. Keep the room screen open for sync."})
        panel.addView(url);panel.addView(name);panel.addView(code)
        fun connect(create:Boolean){
            val endpoint=url.text.toString().trim().trimEnd('/')+"/";val label=name.text.toString();val roomCode=code.text.toString().trim()
            if(!create&&!roomCode.matches(Regex("[a-f0-9]{16}"))){toast("Invalid room code");return}
            val seq=++generation
            worker.execute{val result=runCatching{call(endpoint,if(create)"create" else "join",JSONObject().put("name",label).put("code",roomCode).put("identity",identity(endpoint))).let{Session(endpoint,it.getString("code"),it.getString("token"))}};handler.post{if(!stopped&&visible&&seq==generation&&!a.isDestroyed&&!a.isFinishing)result.onSuccess{room(it)}.onFailure{toast(it.message?:"Connection failed")}else result.getOrNull()?.let{s->if(!worker.isShutdown)worker.execute{runCatching{request(s,"leave")}}}}}
        }
        MaterialAlertDialogBuilder(a).setTitle("Listen Together").setView(panel).setNegativeButton("Cancel",null).setNeutralButton("Join"){_,_->connect(false)}.setPositiveButton("Create room"){_,_->connect(true)}.show()
    }
    private fun current():JSONObject? {
        val p=player()?:return null;val m=p.currentMediaItem?:return null
        if(!m.mediaId.matches(Regex("[A-Za-z0-9_-]{11}"))||m.mediaMetadata.extras?.getBoolean("live")==true)return null
        return JSONObject().put("video",m.mediaId).put("title",m.mediaMetadata.title?.toString().orEmpty()).put("artist",m.mediaMetadata.artist?.toString().orEmpty()).put("position",p.currentPosition.coerceIn(0,86400000)).put("playing",p.isPlaying).put("speed",p.playbackParameters.speed.toDouble())
    }
    private fun song(t:JSONObject):Song {val id=t.getString("video");require(id.matches(Regex("[A-Za-z0-9_-]{11}")));return Song(id,t.optString("title"),t.optString("artist"),"","https://i.ytimg.com/vi/$id/hqdefault.jpg")}
    private fun apply(t:JSONObject,arrivalAge:Long){
        val p=player()?:return;val s=song(t);val playing=t.optBoolean("playing");val speed=t.optDouble("speed",1.0).takeIf{Batch18.validSpeed(it)}?:1.0
        if(!com.abhi.ganasuno.data.LibraryStore.allowed(s.id)){p.pause();return}
        val same=p.currentMediaItem?.mediaId==s.id
        val maximum=if(same&&p.duration>0)p.duration.coerceAtMost(86400000L) else 86400000L
        val pos=(t.optLong("position").coerceIn(0L,maximum)+(if(playing)(arrivalAge*speed).toLong() else 0L)).coerceAtMost(maximum)
        val correction=com.abhi.ganasuno.data.Batch22.sync(p.currentPosition,pos,speed,playing,p.playbackState==Player.STATE_BUFFERING)
        if(!same){p.setMediaItem(item(s),pos);p.prepare()}else if(correction.seek&&p.isCurrentMediaItemSeekable)p.seekTo(pos)
        val desired=if(same)correction.speed else speed.toFloat();if(kotlin.math.abs(p.playbackParameters.speed-desired)>0.001f)p.playbackParameters=androidx.media3.common.PlaybackParameters(desired,p.playbackParameters.pitch)
        if(playing&&!p.playWhenReady)p.play() else if(!playing&&p.playWhenReady)p.pause()
    }
    private fun room(s:Session){
        com.abhi.ganasuno.player.PlayerHub.roomActive=true;com.abhi.ganasuno.player.PlayerHub.cancelTransition?.invoke()
        val seq=++generation;var active=true;var busy=false;var latest=JSONObject();val samples=mutableListOf<Long>();val priorSpeed=player()?.playbackParameters
        val panel=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL;setPadding(24,12,24,12)}
        val status=TextView(a).apply{text="Connecting…";textSize=16f};panel.addView(status)
        val follow=CheckBox(a).apply{text="Follow host playback (replaces current queue)";isChecked=false};panel.addView(follow)
        follow.setOnCheckedChangeListener{_,checked->if(!checked&&priorSpeed!=null)player()?.playbackParameters=priorSpeed}
        val broadcast=CheckBox(a).apply{text="Host: continuously publish current track";isChecked=false};panel.addView(broadcast)
        fun act(action:String,body:JSONObject=JSONObject(),after:((JSONObject)->Unit)?=null){if(!active||busy)return;busy=true;worker.execute{val r=runCatching{request(s,action,body)};handler.post{busy=false;if(active&&seq==generation){r.onSuccess{after?.invoke(it)}.onFailure{toast(it.message?:"Request failed")}}}}}
        fun button(text:String,action:()->Unit):Button=Button(a).apply{this.text=text;setOnClickListener{action()};panel.addView(this)}
        button("Copy room invitation"){val clipboard=a.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager;clipboard.setPrimaryClip(android.content.ClipData.newPlainText("GANASUNO room","GANASUNO room: ${s.code}\nServer: ${s.url}"));toast("Invitation copied; return to this screen to resume sync") }
        button("Publish current track now"){current()?.let{act("publish",it)}?:toast("Play an eligible online track first")}
        button("Suggest a track ID"){val input=EditText(a).apply{hint="11-character video ID"};MaterialAlertDialogBuilder(a).setTitle("Suggest track").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Send"){_,_->val id=input.text.toString().trim();if(id.matches(Regex("[A-Za-z0-9_-]{11}")))act("suggest",JSONObject().put("video",id).put("title",id))else toast("Invalid track ID")}.show()}
        button("Shared timestamp comments / reactions"){
            val t=latest.optJSONObject("track");val video=t?.optString("video").orEmpty()
            if(!latest.optBoolean("approved")||!com.abhi.ganasuno.data.Batch16.valid("video",video)){toast("Wait for approval and a published track");return@button}
            fun body()=JSONObject().put("video",video)
            fun list(){act("comment_list",body()){result->val rows=result.optJSONArray("comments");val items=(0 until (rows?.length()?:0)).mapNotNull{i->rows?.optJSONObject(i)}.sortedBy{it.optLong("position")};if(items.isEmpty())toast("No shared comments yet")else MaterialAlertDialogBuilder(a).setTitle("Latest 30 shared timestamp comments").setItems(items.map{c->"${c.optLong("position")/60000}:${(c.optLong("position")/1000%60).toString().padStart(2,'0')} · ${c.optString("author")} · ${c.optString("reaction")} ${c.optString("text")}"}.toTypedArray()){_,i->val c=items[i];MaterialAlertDialogBuilder(a).setTitle("Comment actions").setItems(arrayOf("Seek to timestamp (current track)","Delete if author or host")){_,n->if(n==0){val p=player();if(p?.currentMediaItem?.mediaId==video)p.seekTo(c.optLong("position").coerceAtLeast(0))else toast("Play the published track first")}else act("comment_delete",JSONObject().put("id",c.optString("id"))){list()}}.show()}.setNegativeButton("Close",null).show()}}
            fun waveform(){act("comment_list",body()){result->
                val rows=result.optJSONArray("comments");val comments=(0 until (rows?.length()?:0)).mapNotNull{i->rows?.optJSONObject(i)}.sortedBy{it.optLong("position")}
                val track=song(t?:return@act)
                val holder=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL;setPadding(24,12,24,12)}
                val info=TextView(a).apply{text="Tap a waveform marker or a comment row to read it. Send a quick reaction at the current timestamp. Turn off Follow host before seeking independently."};holder.addView(info)
                val waveBox=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL};holder.addView(waveBox)
                fun seek(ms:Long){val p=player();if(p?.currentMediaItem?.mediaId==video&&p.isCurrentMediaItemSeekable&&!follow.isChecked)p.seekTo(ms.coerceAtLeast(0))else toast("Play the published track and turn off Follow host first")}
                fun commentActions(c:JSONObject){MaterialAlertDialogBuilder(a).setTitle("${c.optString("author")} · ${c.optLong("position")/1000}s").setMessage("${c.optString("reaction")} ${c.optString("text")}").setNegativeButton("Close",null).setPositiveButton("Seek"){_,_->seek(c.optLong("position"))}.show()}
                fun showWave(summary:LocalTrackAnalyzer.Track){waveBox.removeAllViews();val wave=TrackWaveform(a,summary.analysis.peaks){f->seek((f*summary.duration).toLong())};val marked=comments.filter{it.optLong("position") in 0..summary.duration};wave.markers=marked.map{it.optLong("position").toFloat()/summary.duration.coerceAtLeast(1)};wave.onMarker={index->marked.getOrNull(index)?.let(::commentActions)};val p=player();if(p?.currentMediaItem?.mediaId==video)wave.progress=p.currentPosition.toFloat()/summary.duration.coerceAtLeast(1);waveBox.addView(wave,LinearLayout.LayoutParams(-1,(100*a.resources.displayMetrics.density).toInt()))}
                val dialog=MaterialAlertDialogBuilder(a).setTitle("Room comment waveform").setView(ScrollView(a).apply{addView(holder)}).setPositiveButton("Close",null).create()
                val cached=LocalTrackAnalyzer.cached(track)
                if(cached!=null)showWave(cached)else waveBox.addView(Button(a).apply{text="Decode current track waveform (uses data)";setOnClickListener{isEnabled=false;text="Decoding…";if(!waveWorker.isShutdown)waveWorker.execute{val decoded=runCatching{LocalTrackAnalyzer.analyze(a.applicationContext,track,true)};handler.post{if(active&&dialog.isShowing&&!a.isDestroyed)decoded.onSuccess{showWave(it)}.onFailure{isEnabled=true;text="Waveform unavailable — retry"}}}}})
                val reactions=LinearLayout(a);holder.addView(reactions)
                listOf("heart" to "♥","star" to "★","fire" to "🔥").forEach{(code,label)->reactions.addView(Button(a).apply{text=label;contentDescription="Send $code reaction";setOnClickListener{val p=player();if(p?.currentMediaItem?.mediaId!=video){toast("Play the published track first");return@setOnClickListener};val position=p.currentPosition.coerceIn(0L,86400000L);act("comment_add",body().put("position",position).put("text","").put("reaction",code)){dialog.dismiss();waveform()}}},LinearLayout.LayoutParams(0,-2,1f))}
                if(comments.isEmpty())holder.addView(TextView(a).apply{text="No shared comments yet"})
                comments.forEach{c->holder.addView(Button(a).apply{text="${c.optLong("position")/1000}s · ${c.optString("author")} · ${c.optString("reaction")} ${c.optString("text")}";setOnClickListener{commentActions(c)}})}
                dialog.show()
            }}
            MaterialAlertDialogBuilder(a).setTitle("Comments shared with approved room members").setItems(arrayOf("View / seek / delete","Post comment or reaction","Waveform and timestamp markers")){_,n->if(n==0)list()else if(n==2)waveform()else{val input=EditText(a).apply{hint="Optional comment (250 characters)";filters=arrayOf(android.text.InputFilter.LengthFilter(250))};val local=player();val position=(if(local?.currentMediaItem?.mediaId==video)local.currentPosition else t?.optLong("position")?:0L).coerceIn(0L,86400000L);MaterialAlertDialogBuilder(a).setTitle("Post at ${position/1000}s").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Choose reaction"){_,_->val text=input.text.toString().trim();val reactions=listOf("","heart","star","fire");MaterialAlertDialogBuilder(a).setTitle("Reaction").setItems(arrayOf("None","♥","★","🔥")){_,i->if(text.isBlank()&&i==0)toast("Enter a comment or choose a reaction")else act("comment_add",body().put("position",position).put("text",text).put("reaction",reactions[i])){toast("Shared in room")}}.show()}.show()}}.setNegativeButton("Close",null).show()
        }
        button("Members and host controls"){
            val arr=latest.optJSONArray("members");val list=(0 until (arr?.length()?:0)).map{arr!!.getJSONObject(it)}
            if(list.isEmpty()){toast("No members available");return@button}
            MaterialAlertDialogBuilder(a).setTitle("Members").setItems(list.map{it.optString("name")+if(it.optBoolean("host"))" · host" else if(it.optBoolean("approved"))" · approved" else " · pending"}.toTypedArray()){_,i->
                if(latest.optBoolean("host")&&!list[i].optBoolean("host"))MaterialAlertDialogBuilder(a).setTitle(list[i].optString("name")).setItems(arrayOf("Approve","Remove member","Block installation for this room","Transfer host")){_,n->act(arrayOf("approve","kick","block","transfer")[n],JSONObject().put("member",list[i].getString("id")))}.show()
            }.setNegativeButton("Close",null).show()
        }
        button("Toggle auto-approval"){act("auto",JSONObject().put("enabled",!latest.optBoolean("auto")))}
        button("View suggested tracks"){val arr=latest.optJSONArray("suggestions");val list=(0 until (arr?.length()?:0)).map{arr!!.getJSONObject(it)};if(list.isEmpty())toast("No suggestions")else MaterialAlertDialogBuilder(a).setTitle("Suggestions").setItems(list.map{"${it.optString("title")} · ${it.optString("from")}"}.toTypedArray()){_,n->if(latest.optBoolean("host")){val track=song(list[n]);player()?.let{p->p.setMediaItem(item(track));p.prepare();p.play()}}}.setNeutralButton("Clear"){_,_->act("clear_suggestions")}.setNegativeButton("Close",null).show()}
        val dialog=MaterialAlertDialogBuilder(a).setTitle("Room ${s.code}").setView(ScrollView(a).apply{addView(panel)}).setPositiveButton("Leave room",null).create()
        val tick=object:Runnable{override fun run(){if(!active||seq!=generation)return;if(visible&&!busy){busy=true;val capture=SystemClock.elapsedRealtime();val publish=if(broadcast.isChecked&&latest.optBoolean("host"))current()else null;val history=samples.toList();worker.execute{val r=runCatching{if(publish!=null)request(s,"publish",publish.put("sampleAgeMs",RoomTiming.publicationAge(capture,SystemClock.elapsedRealtime(),history)));val start=SystemClock.elapsedRealtime();val state=request(s,"state");val received=SystemClock.elapsedRealtime();Triple(state,received-start,received)};handler.post{busy=false;if(active&&seq==generation){r.onSuccess{(state,roundTrip,received)->if(roundTrip in 0..RoomTiming.MAX_RTT){samples.add(roundTrip);if(samples.size>8)samples.removeAt(0)};latest=state;val role=if(state.optBoolean("host"))"Host" else if(state.optBoolean("approved"))"Guest" else "Awaiting approval";val t=state.optJSONObject("track");status.text="$role · auto approval ${if(state.optBoolean("auto"))"on" else "off"}\n${t?.optString("title")?:"No published track"}";if(visible&&follow.isChecked&&role=="Guest"&&t!=null)runCatching{apply(t,RoomTiming.arrivalAge(roundTrip,SystemClock.elapsedRealtime()-received,history))}.onFailure{status.text="Track could not be synchronized"}}.onFailure{status.text=it.message?:"Connection failed";if(follow.isChecked)player()?.pause()}}}}};handler.postDelayed(this,2000)}}
        dialog.setOnDismissListener{active=false;handler.removeCallbacks(tick);closeRoom=null;com.abhi.ganasuno.player.PlayerHub.roomActive=false;if(follow.isChecked&&priorSpeed!=null)player()?.playbackParameters=priorSpeed;if(!worker.isShutdown)worker.execute{runCatching{request(s,"leave")}}}
        closeRoom={dialog.dismiss()};dialog.show();handler.post(tick)
    }
    fun foreground(value:Boolean){visible=value}
    fun stop(){stopped=true;generation++;closeRoom?.invoke();worker.shutdown();waveWorker.shutdownNow();}
}
