@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.abhi.ganasuno.ui

import android.app.Activity
import android.media.audiofx.Equalizer
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.abhi.ganasuno.player.PlayerHub
import com.abhi.ganasuno.data.LibraryStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Hardware-dependent Android EQ; band count is provided by the device, not simulated. */
object EqualizerDialog {
    private var eq: Equalizer?=null
    private var sessionId=0
    fun release(){runCatching{eq?.release()};eq=null;sessionId=0}
    fun apply(){val id=PlayerHub.player?.audioSessionId?:return;if(id<=0)return;if(eq==null||sessionId!=id){release();runCatching{eq=Equalizer(0,id);sessionId=id}};eq?.let{e->runCatching{for(i in 0 until e.numberOfBands.toInt())e.setBandLevel(i.toShort(),LibraryStore.option("eq_$i").coerceIn(e.bandLevelRange[0].toInt(),e.bandLevelRange[1].toInt()).toShort());e.enabled=LibraryStore.option("eq_enabled")==1}}}
    fun show(a:Activity){apply();val e=eq;if(e==null){Toast.makeText(a,"Equalizer unavailable. Start a song first.",Toast.LENGTH_LONG).show();return};val layout=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL;setPadding(24,12,24,12)};val enabled=android.widget.Switch(a).apply{isChecked=e.enabled;text="Enable equalizer · ${SmartSettingIndicators.onOff(isChecked)}";setOnCheckedChangeListener{_,v->LibraryStore.option("eq_enabled",if(v)1 else 0,true);runCatching{e.enabled=v};text="Enable equalizer · ${SmartSettingIndicators.onOff(e.enabled)}"}};layout.addView(enabled);val presets=android.widget.Button(a).apply{text="Device EQ presets";setOnClickListener{val names=(0 until e.numberOfPresets.toInt()).map{e.getPresetName(it.toShort())};if(names.isEmpty())Toast.makeText(a,"No hardware presets",Toast.LENGTH_SHORT).show()else MaterialAlertDialogBuilder(a).setTitle("Equalizer presets").setItems(names.toTypedArray()){_,i->runCatching{e.usePreset(i.toShort());for(band in 0 until e.numberOfBands.toInt())LibraryStore.option("eq_$band",e.getBandLevel(band.toShort()).toInt(),true);LibraryStore.option("eq_enabled",1,true);e.enabled=true;Toast.makeText(a,"Preset saved; reopen equalizer to see curve",Toast.LENGTH_SHORT).show()}}.show()}};layout.addView(presets);val min=e.bandLevelRange[0].toInt();val max=e.bandLevelRange[1].toInt();for(i in 0 until e.numberOfBands.toInt()){val label=TextView(a);layout.addView(label);val bar=SeekBar(a);layout.addView(bar);bar.max=max-min;bar.progress=e.getBandLevel(i.toShort()).toInt()-min;fun title(level:Int){label.text="${e.getCenterFreq(i.toShort())/1000} Hz · ${level/100.0} dB"};title(bar.progress+min);bar.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(b:SeekBar?,p:Int,f:Boolean){val v=p+min;title(v);if(f){LibraryStore.option("eq_$i",v,true);runCatching{e.setBandLevel(i.toShort(),v.toShort())}}};override fun onStartTrackingTouch(b:SeekBar?){};override fun onStopTrackingTouch(b:SeekBar?){}})};val scroll=android.widget.ScrollView(a).apply{addView(layout)};MaterialAlertDialogBuilder(a).setTitle("Device equalizer").setView(scroll).setNeutralButton("Flat"){_,_->for(i in 0 until e.numberOfBands.toInt())LibraryStore.option("eq_$i",0,true);apply()}.setPositiveButton("Close",null).show()}
}
