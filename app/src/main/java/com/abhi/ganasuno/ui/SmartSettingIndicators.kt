package com.abhi.ganasuno.ui

import android.content.Context
import android.content.res.ColorStateList
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Switch
import androidx.media3.common.Player
import com.abhi.ganasuno.data.LibraryStore
import com.abhi.ganasuno.data.Prefs
import com.abhi.ganasuno.player.SleepTimer
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Read existing preferences, never invent a selected state for an action-only menu. */
object SmartSettingIndicators {
    private fun opt(key:String,default:Int=0)=LibraryStore.option(key,default)
    fun onOff(value:Boolean)=if(value) "On" else "Off"
    private fun flag(key:String,default:Int=0)=onOff(opt(key,default)==1)
    private fun index(value:Int, vararg choices:Int)=choices.indexOf(value)
    fun selection(context:Context,title:String,player:Player?):Int? = when {
        title.startsWith("Cache size ·") -> com.abhi.ganasuno.data.Batch25.cacheChoices.indexOf(opt("media_cache_mib",64))
        title.startsWith("Explicit filter ·") -> opt("skip_explicit")
        title.startsWith("Include app-owned audio") -> opt("backup_audio")
        title.startsWith("Preferred lyrics provider ·") -> opt("lyrics_provider")
        title.startsWith("Software preamp (dB") -> (-12..6 step 3).toList().indexOf(opt("preamp"))
        title.startsWith("Software reverb ·") -> opt("reverb")
        else -> when(title){
            "Rotation" -> opt("rotate")
            "Direction" -> index(opt("rotate_direction",1),1,-1)
            "Delay" -> {
                val ms=opt("delay_ms"); val feedback=opt("delay_feedback",20); val mix=opt("delay_mix",20)
                when {ms==0->0;ms==95&&feedback==12&&mix==15->1;ms==350&&feedback==28&&mix==22->2;ms==480&&feedback==53&&mix==30->3;else->-1}
            }
            "Liquid Glass" -> opt("glass_reduced")
            "Endless artist mix" -> opt("endless")
            "Next-track preloading" -> opt("preload")
            "Sleep fade" -> index(opt("sleep_fade",8000),2000,5000,8000,15000)
            "Speed" -> player?.playbackParameters?.speed?.let{listOf(.5f,.75f,1f,1.25f,1.5f,2f).indexOf(it)}
            "Pitch" -> player?.playbackParameters?.pitch?.let{listOf(.5f,.75f,1f,1.25f,1.5f,2f).indexOf(it)}
            "Theme" -> index(opt("theme",2),-1,1,2)
            "Theme colours" -> opt("palette")
            "Player appearance" -> opt("player_style")
            "Animated cover artwork" -> opt("animated_art").takeIf{it in 0..1} ?: -1 // import/remove are commands, not modes
            "Layout direction" -> opt("rtl_preview")
            "App language" -> listOf("","en","ar","he").indexOf(androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags())
            "Content country" -> opt("country")
            "Home source" -> opt("home_source")
            "Home result limit" -> index(opt("home_limit",30),10,20,30,50,100)
            "Library layout" -> opt("ui_grid")
            "Video results" -> opt("video_mode")
            "Schedule" -> index(opt("backup_days"),0,1,7,30)
            "Keep snapshots" -> index(opt("backup_keep",5),3,5,10,20)
            "Stop playback when app is removed from Recents" -> opt("stop_on_exit")
            "Reverb send level" -> index(opt("reverb_mix",35),10,25,50,75,100)
            "Release channel" -> context.getSharedPreferences("updates",Context.MODE_PRIVATE).getInt("channel",0)
            "Automatic checks" -> if(context.getSharedPreferences("updates",Context.MODE_PRIVATE).getBoolean("auto",false))1 else 0
            else -> null
        }
    }
    /** True only for recognized setting menus. Callback retains the original index and behaviour. */
    fun show(builder:MaterialAlertDialogBuilder,context:Context,title:String,labels:List<String>,selected:Int?,action:(Int)->Unit):Boolean {
        if(selected==null)return false
        val checked=selected.takeIf{it in labels.indices} ?: -1
        val current=labels.getOrNull(checked) ?: "Custom / no matching preset"
        builder.setBackground(GlassFeatureStyle.dialog(context)).setTitle(title)
        if(labels==listOf("Off","On") && checked>=0){
            val panel=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;val pad=GlassFeatureStyle.dp(context,20);setPadding(pad,pad,pad,pad)}
            val toggle=Switch(context).apply{isChecked=checked==1;text=if(isChecked)"On · Enabled" else "Off · Disabled";minimumHeight=GlassFeatureStyle.dp(context,60);background=GlassFeatureStyle.surface(context,20);val pad=GlassFeatureStyle.dp(context,12);setPadding(pad,pad,pad,pad);setTextColor(GlassFeatureStyle.primary(context));trackTintList=ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked),intArrayOf()),intArrayOf(0xff8e8e93.toInt(),0xff57575d.toInt()))}
            panel.addView(toggle,LinearLayout.LayoutParams(-1,-2))
            val dialog=builder.setView(panel).setNegativeButton("Close",null).create()
            toggle.setOnCheckedChangeListener{_,enabled->toggle.text=if(enabled)"On · Enabled" else "Off · Disabled";dialog.dismiss();action(if(enabled)1 else 0)}
            dialog.show()
        }else{
            // Mixed command menus must not mark import/remove actions as radio settings.
            val display=labels.mapIndexed{i,label->if(i==checked)"✓  $label  · Current" else label}
            builder.setTitle("$title\nCurrent: $current").setItems(display.toTypedArray()){_,i->action(i)}.setNegativeButton("Close",null).show()
        }
        return true
    }
    fun summary(title:String,player:Player?):String?=when(title){
        "Liquid Glass effects" -> if(opt("glass_reduced")==1)"Reduced effects" else "Full effects"
        "Audio & video quality" -> "${listOf("Low","Normal","High","320 kbps preferred").getOrNull(Prefs.audioQuality)?:"Custom"} · ${Prefs.videoHeight}p · Skip silence ${onOff(Prefs.skipSilence)} · Normalize ${onOff(Prefs.normalize)} · Timer ${if(SleepTimer.untilSongEnd)"End of song" else if(SleepTimer.isActive)"On" else "Off"}"
        "Playback controls" -> player?.let{"Shuffle ${onOff(it.shuffleModeEnabled)} · Repeat ${listOf("Off","One","All").getOrElse(it.repeatMode){"Unknown"}} · ${it.playbackParameters.speed}x"}?:"Player not connected"
        "Crossfade & DJ transitions" -> "Crossfade ${if(opt("crossfade_seconds")==0)"Off" else "${opt("crossfade_seconds")} s"} · DJ ${flag("dj_transition")}" 
        "Manual fade" -> "${opt("manual_fade",1000)} ms"
        "Explicit content" -> if(opt("skip_explicit")==1)"Skip labelled tracks" else "Allow all"
        "320 kbps preference" -> onOff(Prefs.audioQuality==Prefs.Q_320)
        "Video playback mode" -> if(opt("video_mode")==1)"Video player" else "Audio only"
        "Equalizer" -> "Saved preference: ${flag("eq_enabled")}"
        "10-band software EQ" -> "Saved preference: ${flag("dsp_eq")}"
        "Stereo rotation & delay" -> "Rotation ${flag("rotate")} · Delay ${if(opt("delay_ms")==0)"Off" else "${opt("delay_ms")} ms"}"
        "Reverb and preamp" -> "Reverb ${listOf("Off","Small room","Medium room","Large room","Medium hall","Large hall","Plate","Cathedral").getOrNull(opt("reverb"))?:"Custom"} · ${opt("preamp")} dB"
        "Player style" -> listOf("Classic","Expressive","Mesh","Waveform").getOrNull(opt("player_style"))
        "Animated artwork" -> listOf("Off","Rotation","Imported artwork").getOrNull(opt("animated_art"))
        "Theme" -> when(opt("theme",2)){-1->"System";1->"Light";2->"Dark";else->null}
        "Theme colours" -> listOf("Default","Wallpaper","Green","Orange","Pink","Titanium Grey","Custom").getOrNull(opt("palette"))
        "Language and RTL" -> listOf("Device direction","Left to right","Right to left").getOrNull(opt("rtl_preview"))
        "Country & home feed" -> "${listOf("India","USA","UK","Japan","South Korea","Germany").getOrNull(opt("country"))?:"Custom"} · ${opt("home_limit",30)} results"
        "Playback cache" -> "Saved preference: ${flag("media_cache")} · ${opt("media_cache_mib",64)} MiB"
        "Stop service on exit" -> if(opt("stop_on_exit")==1)"Stop service" else "Keep playing"
        else -> null
    }
}
