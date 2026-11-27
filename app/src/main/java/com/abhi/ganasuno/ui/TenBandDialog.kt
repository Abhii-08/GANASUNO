@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.abhi.ganasuno.ui

import android.app.Activity
import android.widget.*
import com.abhi.ganasuno.data.LibraryStore
import com.abhi.ganasuno.player.EffectsSettings
import com.abhi.ganasuno.player.TenBandDsp
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object TenBandDialog {
    fun show(a:Activity) {
        val panel=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL;setPadding(24,12,24,12)}
        fun save(k:String,v:Int){LibraryStore.option(k,v,true);EffectsSettings.refresh()}
        panel.addView(TextView(a).apply{text="Software 10-band EQ · PCM16 mono/stereo music. Use negative headroom to reduce clipping; disable hardware EQ to avoid stacking filters."})
        panel.addView(Switch(a).apply{isChecked=LibraryStore.option("dsp_eq")==1;text="Enable software EQ · ${SmartSettingIndicators.onOff(isChecked)}";setOnCheckedChangeListener{_,v->save("dsp_eq",if(v)1 else 0);text="Enable software EQ · ${SmartSettingIndicators.onOff(v)}"}})
        val bars=mutableListOf<SeekBar>()
        fun slider(title:String,key:String,min:Int,max:Int,default:Int):SeekBar{
            val label=TextView(a);panel.addView(label);val b=SeekBar(a);b.max=max-min;b.progress=LibraryStore.option(key,default).coerceIn(min,max)-min;panel.addView(b)
            fun update(v:Int){label.text="$title · $v dB"};update(b.progress+min)
            b.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,p:Int,f:Boolean){update(p+min);save(key,p+min)};override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){}});return b
        }
        fun profile(gains:List<Int>){gains.forEachIndexed{i,g->save("dsp_band_$i",g);bars[i].progress=g+12}}
        panel.addView(Button(a).apply{text="Presets";setOnClickListener{val curves=listOf(List(10){0},listOf(5,5,3,1,0,0,0,0,0,0),listOf(-3,-2,0,2,4,4,2,0,-1,-2),listOf(3,2,0,-1,0,1,2,3,3,2));SmartSettingIndicators.show(MaterialAlertDialogBuilder(a),a,"Software EQ presets",listOf("Flat","Bass boost","Vocal clarity","V shape"),curves.indexOf(List(10){LibraryStore.option("dsp_band_$it") })){i->profile(curves[i])}}})
        panel.addView(Button(a).apply{text="Import GraphicEQ profile";setOnClickListener{val input=EditText(a).apply{hint="GraphicEQ: 20 -3; 100 2; 1000 0; 10000 -2"};MaterialAlertDialogBuilder(a).setTitle("Paste fixed-frequency EQ points").setMessage("Use an AutoEq GraphicEQ export or your own points. Interpolated to 10 bands; not exact parametric correction.").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Apply"){_,_->runCatching{TenBandDsp.graphicProfile(input.text.toString())}.onSuccess{profile(it)}.onFailure{Toast.makeText(a,"Invalid GraphicEQ profile",Toast.LENGTH_LONG).show()}}.show()}})
        val profiles=a.getSharedPreferences("headphone_profiles",android.content.Context.MODE_PRIVATE)
        panel.addView(Button(a).apply{text="Save named headphone profile";setOnClickListener{
            val name=EditText(a).apply{hint="Headphone model / profile name"};val eq=EditText(a).apply{hint="GraphicEQ: 20 -3; 100 2; 1000 0";minLines=3};val fields=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL;addView(name);addView(eq)}
            MaterialAlertDialogBuilder(a).setTitle("Import your AutoEq profile").setView(fields).setNegativeButton("Cancel",null).setPositiveButton("Save and apply"){_,_->runCatching{val key=com.abhi.ganasuno.data.Batch13.profileName(name.text.toString())?:error("Invalid profile name");val raw=eq.text.toString();require(raw.length<=30000);val gains=TenBandDsp.graphicProfile(raw);require(profiles.contains(key)||profiles.all.size<100){"Profile library full"};profiles.edit().putString(key,raw).apply();profile(gains)}.onFailure{Toast.makeText(a,it.message?:"Invalid profile",Toast.LENGTH_LONG).show()}}.show()
        }})
        panel.addView(Button(a).apply{text="Search saved headphone models";setOnClickListener{
            val input=EditText(a).apply{hint="Search model name"};MaterialAlertDialogBuilder(a).setTitle("Search imported profiles").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Search"){_,_->val names=com.abhi.ganasuno.data.Batch13.profiles(profiles.all.keys.toList(),input.text.toString());if(names.isEmpty())Toast.makeText(a,"No saved matching profiles. Import a GraphicEQ profile first.",Toast.LENGTH_LONG).show()else MaterialAlertDialogBuilder(a).setTitle("Matching profiles").setItems(names.toTypedArray()){_,i->val name=names[i];MaterialAlertDialogBuilder(a).setTitle(name).setNegativeButton("Close",null).setNeutralButton("Delete"){_,_->profiles.edit().remove(name).apply()}.setPositiveButton("Apply"){_,_->runCatching{TenBandDsp.graphicProfile(profiles.getString(name,"").orEmpty())}.onSuccess{profile(it)}.onFailure{Toast.makeText(a,"Invalid saved profile",Toast.LENGTH_LONG).show()}}.show()}.show()}.show()
        }})
        panel.addView(Button(a).apply{text="Search online AutoEq catalogue";setOnClickListener{
            val field=EditText(a).apply{hint="Headphone model (e.g. Sony WH-1000XM4)"};MaterialAlertDialogBuilder(a).setTitle("AutoEq online profiles").setMessage("Fetch the public GitHub catalogue (up to 24 MB). GraphicEQ points are approximated to ten bands.").setView(field).setNegativeButton("Cancel",null).setPositiveButton("Search"){_,_->val query=field.text.toString().trim();if(query.isBlank())return@setPositiveButton;Thread{val result=runCatching{com.abhi.ganasuno.data.RemoteMetadata.profiles(query)};a.runOnUiThread{if(!a.isDestroyed&&!a.isFinishing)result.onSuccess{rows->if(rows.isEmpty())Toast.makeText(a,"No matching profiles",Toast.LENGTH_LONG).show()else MaterialAlertDialogBuilder(a).setTitle("Choose measurement source").setItems(rows.map{"${it.name} · ${it.source}"}.toTypedArray()){_,i->val row=rows[i];Thread{val r=runCatching{val raw=com.abhi.ganasuno.data.RemoteMetadata.graphic(row);val curve=TenBandDsp.graphicProfile(raw);raw to curve};a.runOnUiThread{if(!a.isDestroyed&&!a.isFinishing)r.onSuccess{(raw,curve)->MaterialAlertDialogBuilder(a).setTitle("Apply ${row.name}?").setMessage("Approximate ten-band correction. Use negative headroom.").setNegativeButton("Cancel",null).setPositiveButton("Apply"){_,_->val key="${row.name} · ${row.source}".take(100);if(profiles.contains(key)||profiles.all.size<100)profiles.edit().putString(key,raw).apply();profile(curve)}.show()}.onFailure{Toast.makeText(a,it.message?:"Profile fetch failed",Toast.LENGTH_LONG).show()}}}.start()}.show()}.onFailure{Toast.makeText(a,it.message?:"Catalogue unavailable",Toast.LENGTH_LONG).show()}}}.start()}.show()
        }})
        slider("Headroom","dsp_headroom",-24,0,-6)
        TenBandDsp.frequencies.forEachIndexed{i,f->bars.add(slider("$f Hz","dsp_band_$i",-12,12,0))}
        MaterialAlertDialogBuilder(a).setTitle("10-band software EQ").setView(ScrollView(a).apply{addView(panel)}).setPositiveButton("Close",null).show()
    }
}
