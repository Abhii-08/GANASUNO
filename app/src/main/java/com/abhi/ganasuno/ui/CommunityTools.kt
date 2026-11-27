package com.abhi.ganasuno.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.abhi.ganasuno.data.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.Executor

class CommunityTools(private val a:Activity,private val io:Executor,private val current:()->Song?,private val seek:(Long)->Unit,private val cache:()->Unit,private val catalogue:PublicCatalogueTools){
    private fun toast(s:String)=Toast.makeText(a,s,Toast.LENGTH_LONG).show()
    private fun choose(title:String,labels:List<String>,action:(Int)->Unit){if(labels.isEmpty()){toast("No results available");return};MaterialAlertDialogBuilder(a).setTitle(title).setItems(labels.toTypedArray()){_,i->action(i)}.setNegativeButton("Close",null).show()}
    private fun <T> task(load:()->T,done:(T)->Unit){toast("Contacting community service…");io.execute{val result=runCatching{load()};a.runOnUiThread{if(!a.isDestroyed&&!a.isFinishing)result.onSuccess(done).onFailure{toast("Service unavailable; try again later")}}}}
    private fun withVideo(action:(Song)->Unit){val song=current();if(song==null||song.localUri.isNotBlank()||!Batch25.videoId(song.id))toast("Play a public online video or song first")else action(song)}
    fun show(){choose("Community and charts · v25",listOf("SponsorBlock · ${SmartSettingIndicators.onOff(LibraryStore.option("sponsor_enabled")==1)}","SponsorBlock · choose nine categories","SponsorBlock · view segments and jump","Return YouTube Dislike · community estimates","Read public YouTube comments and replies","Regional popular music videos","Playback cache size")){n->when(n){
        0->toggle();1->categories();2->withVideo{segments(it)};3->withVideo{votes(it)};4->withVideo{catalogue.comments(it.id,it.title)};5->catalogue.charts();6->cache()
    }}}
    private fun toggle(){val enabled=LibraryStore.option("sponsor_enabled")==1;MaterialAlertDialogBuilder(a).setTitle("SponsorBlock · ${if(enabled)"On" else "Off"}").setMessage("Optional community-supplied segment skipping in seekable online audio and video. When enabled, video IDs and selected categories are sent to sponsor.ajay.app. Segments may be inaccurate; highlights never auto-skip. Turn off during shared-room playback if skips conflict with the host.").setNegativeButton("Cancel",null).setPositiveButton(if(enabled)"Disable" else "Enable"){_,_->LibraryStore.option("sponsor_enabled",if(enabled)0 else 1,true);toast(if(enabled)"Disabled" else "Enabled")}.show()}
    private fun categories(){var mask=LibraryStore.option("sponsor_categories",1) and Batch25.ALL;val checked=BooleanArray(Batch25.categories.size){i->mask and (1 shl i)!=0};MaterialAlertDialogBuilder(a).setTitle("Choose categories · highlight is manual only").setMultiChoiceItems(Batch25.labels.toTypedArray(),checked){_,i,on->mask=if(on)mask or (1 shl i) else mask and (1 shl i).inv()}.setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->LibraryStore.option("sponsor_categories",mask,true);toast("Categories saved")}.show()}
    private fun timestamp(ms:Long)="%d:%02d".format(ms/60000,(ms/1000)%60)
    private fun segments(song:Song){MaterialAlertDialogBuilder(a).setTitle("Look up community segments?").setMessage("Sends the current video ID to SponsorBlock. No account information is sent.").setNegativeButton("Cancel",null).setPositiveButton("Fetch"){_,_->task({CommunityVideo.segments(song.id,LibraryStore.option("sponsor_categories",1))}){rows->choose("Community timestamps · tap to seek",rows.map{r->"${timestamp(r.start)}–${timestamp(r.end)} · ${Batch25.labels[Batch25.categories.indexOf(r.category)]}"}){i->if(current()?.id==song.id)seek(rows[i].start)else toast("Current track changed")}}}.show()}
    private fun votes(song:Song){MaterialAlertDialogBuilder(a).setTitle("Return YouTube Dislike").setMessage("Fetch community estimates for this video? Its public ID is sent to Return YouTube Dislike. These numbers are not official YouTube dislike counts.").setNegativeButton("Cancel",null).setPositiveButton("Fetch"){_,_->task({CommunityVideo.votes(song.id)}){v->MaterialAlertDialogBuilder(a).setTitle(song.title).setMessage("Community estimates\nLikes: ${v.likes?:"Unavailable"}\nDislikes: ${v.dislikes?:"Unavailable"}\nViews: ${v.views?:"Unavailable"}\n\nSource: Return YouTube Dislike (returnyoutubedislike.com)").setPositiveButton("Close",null).setNeutralButton("Source"){_,_->runCatching{a.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.returnyoutubedislike.com/")))}.onFailure{toast("No browser available")}}.show()}}.show()}
}
