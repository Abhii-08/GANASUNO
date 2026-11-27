package com.abhi.ganasuno.ui

import android.app.Activity
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.abhi.ganasuno.data.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.Executor

/** User identifies the exact channel; no silent or bulk account changes. */
class SubscriptionTools(private val a:Activity,private val io:Executor){
    private fun toast(s:String)=Toast.makeText(a,s,Toast.LENGTH_LONG).show()
    fun show(){
        val box=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL;setPadding(24,12,24,12)}
        box.addView(TextView(a).apply{text="Use a short-lived OAuth access token issued to your own authorized YouTube Data API client with subscription permission. This is not a password or API key. Token stays in memory, is sent only to Google, and is discarded when this operation ends. No automatic login or token refresh."})
        val channel=EditText(a).apply{hint="Exact UC channel ID or /channel/ URL";setSingleLine(true)};box.addView(channel)
        val artist=EditText(a).apply{hint="Local artist name to link";setSingleLine(true)};box.addView(artist)
        val secret=EditText(a).apply{hint="OAuth access token";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD;isSaveEnabled=false;setSingleLine(true)};box.addView(secret)
        MaterialAlertDialogBuilder(a).setTitle("Check YouTube subscription").setCancelable(false).setView(box).setNegativeButton("Cancel"){_,_->secret.text.clear()}.setPositiveButton("Check status"){_,_->val id=Batch22.channel(channel.text.toString());val name=Batch16.clean(artist.text.toString(),200);val token=secret.text.toString().trim();secret.text.clear();if(id==null||name.isBlank()||!Batch22.token(token)){toast("Enter a valid channel, artist and OAuth token");return@setPositiveButton};task({ChannelSubscriptions.state(token,id)}){state->
            val followed=state.subscriptionId!=null;val label=state.title.ifBlank{id};val action=if(followed)"Unsubscribe" else "Subscribe";MaterialAlertDialogBuilder(a).setTitle(label).setMessage("Channel ID: $id\nYouTube: ${if(followed)"Subscribed" else "Not subscribed"}\nLinked local artist: $name. Choose whether to change YouTube or only match the local library to the confirmed remote state.").setNegativeButton("Cancel",null).setNeutralButton("Match local only"){_,_->LibraryStore.setArtistFollowing(name,followed);toast("Local follow status updated")}.setPositiveButton(action){_,_->MaterialAlertDialogBuilder(a).setTitle("Confirm $action on YouTube").setMessage("Change the subscription for $label and update the linked local artist after Google confirms it?").setNegativeButton("Cancel",null).setPositiveButton("Confirm"){_,_->task({ChannelSubscriptions.set(token,id,!followed)}){updated->LibraryStore.setArtistFollowing(name,updated.subscriptionId!=null);toast("YouTube and local artist updated")}}.show()}.show()
        }}.show()
    }
    private fun <T> task(run:()->T,done:(T)->Unit){toast("Contacting YouTube…");io.execute{val result=runCatching{run()};a.runOnUiThread{if(!a.isDestroyed&&!a.isFinishing)result.onSuccess(done).onFailure{toast("Subscription request failed; check token scope, expiry and API access")}}}}
}
