package com.abhi.ganasuno.ui

import android.app.Activity
import android.content.Intent
import android.widget.EditText
import android.widget.Toast
import com.abhi.ganasuno.data.Batch15
import com.abhi.ganasuno.data.LibraryStore
import com.abhi.ganasuno.data.PlaylistLinks
import com.abhi.ganasuno.data.Song
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.Executor

class SharedPlaylists(private val a:Activity,private val io:Executor,private val open:(List<Song>,String)->Unit){
    private val prefs=a.getSharedPreferences("shared_playlist_tokens",android.content.Context.MODE_PRIVATE)
    private fun toast(text:String)=Toast.makeText(a,text,Toast.LENGTH_LONG).show()
    private fun choose(title:String,items:List<String>,action:(Int)->Unit){if(items.isEmpty()){toast("Nothing to show");return};MaterialAlertDialogBuilder(a).setTitle(title).setItems(items.toTypedArray()){_,i->action(i)}.setNegativeButton("Close",null).show()}
    private fun ask(title:String,initial:String="",action:(String)->Unit){val f=EditText(a).apply{setText(initial);setSingleLine(true)};MaterialAlertDialogBuilder(a).setTitle(title).setView(f).setPositiveButton("Continue"){_,_->action(f.text.toString())}.setNegativeButton("Cancel",null).show()}
    private fun <T> task(work:()->T,done:(T)->Unit){io.execute{val r=runCatching(work);a.runOnUiThread{if(!a.isDestroyed&&!a.isFinishing)r.onSuccess(done).onFailure{toast(it.message?:"Request failed")}}}}
    fun show(){choose("Playlist links · self-hosted",listOf("Create an expiring link","Import a shared link","Manage links I created")){i->when(i){0->{val names=LibraryStore.names();choose("Select playlist",names){n->create(names[n])}};1->ask("Paste trusted HTTPS GANASUNO playlist link"){link->MaterialAlertDialogBuilder(a).setTitle("Fetch shared playlist?").setMessage("The selected HTTPS server receives your request. Only public track IDs are imported.").setNegativeButton("Cancel",null).setPositiveButton("Fetch"){_,_->task({PlaylistLinks.read(link)}){snap->ask("Save as playlist",snap.name){raw->val name=Batch15.cleanTitle(raw);if(!LibraryStore.create(name))toast("Invalid or duplicate playlist name")else{snap.ids.forEach{id->LibraryStore.add(name,Song(id,"Video $id","Unknown artist","","https://i.ytimg.com/vi/$id/hqdefault.jpg"))};open(LibraryStore.playlist(name),name)}}}}.show()};2->manage()}}}
    fun create(name:String){val items=LibraryStore.playlist(name);val ids=Batch15.publicIds(items);if(ids.isEmpty()){toast("No public, non-live tracks to share");return};MaterialAlertDialogBuilder(a).setTitle("Share $name?").setMessage("Upload the playlist name and ${ids.size} public video IDs only. Local files, live streams and tracks beyond 100 are excluded. Anyone with the unlisted link can view it for seven days or until server restart. You can revoke the link.").setNegativeButton("Cancel",null).setPositiveButton("Choose server"){_,_->ask("Trusted HTTPS server origin"){server->task({PlaylistLinks.create(server,name,ids).also{check(prefs.edit().putString(it.link,it.revoke).commit()){"Could not store revoke token"}}}){created->share(created.link)}}}.show()}
    private fun share(link:String){a.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,link),"Share playlist URL"))}
    private fun manage(){val links=prefs.all.keys.sorted();choose("My shared playlist links",links){i->val link=links[i];choose(link,listOf("Share again","Revoke on server","Forget locally (does not revoke)")){n->when(n){0->share(link);1->MaterialAlertDialogBuilder(a).setTitle("Revoke this playlist link?").setNegativeButton("Cancel",null).setPositiveButton("Revoke"){_,_->val token=prefs.getString(link,"").orEmpty();task({PlaylistLinks.revoke(link,token);prefs.edit().remove(link).apply()}){toast("Link revoked")}}.show();2->MaterialAlertDialogBuilder(a).setTitle("Forget revoke token?").setMessage("You will no longer be able to revoke this link from this device.").setNegativeButton("Cancel",null).setPositiveButton("Forget"){_,_->prefs.edit().remove(link).apply()}.show()}}}}
}
