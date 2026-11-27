package com.abhi.ganasuno.ui

import android.app.Activity
import android.content.res.ColorStateList
import android.widget.RadioButton
import android.graphics.Color
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.abhi.ganasuno.R
import com.abhi.ganasuno.data.Prefs
import com.abhi.ganasuno.databinding.DialogSettingsBinding
import com.abhi.ganasuno.player.PlayerHub
import com.abhi.ganasuno.player.SleepTimer

/** Settings: audio quality, audio language, video quality, silence skip, normalize, sleep timer. */
object SettingsDialog {
    fun show(activity: Activity, includeSleepTimer:Boolean=true) {
        val b = DialogSettingsBinding.inflate(activity.layoutInflater)

        // Keep native radio/switch semantics and all existing preference callbacks.
        val dark=GlassFeatureStyle.dark(activity)
        val neutral=if(dark)0xffc4c4c4.toInt() else 0xff404a58.toInt()
        val muted=if(dark)0xff71717b.toInt() else 0xff9299a3.toInt()
        val states=arrayOf(intArrayOf(android.R.attr.state_checked),intArrayOf())
        val tint=ColorStateList(states,intArrayOf(neutral,muted))
        listOf(b.groupQuality,b.groupLang,b.groupVideo).forEach { group ->
            group.background=GlassFeatureStyle.surface(activity,20)
            val inset=GlassFeatureStyle.dp(activity,10)
            group.setPadding(inset,inset,inset,inset)
            for(i in 0 until group.childCount){
                (group.getChildAt(i) as? RadioButton)?.apply {
                    buttonTintList=tint;minimumHeight=GlassFeatureStyle.dp(activity,48)
                    setTextColor(GlassFeatureStyle.primary(activity))
                }
            }
        }
        listOf(b.switchSilence,b.switchNormalize).forEach { toggle ->
            toggle.background=GlassFeatureStyle.surface(activity,20)
            toggle.minimumHeight=GlassFeatureStyle.dp(activity,60)
            val inset=GlassFeatureStyle.dp(activity,12)
            toggle.setPadding(inset,inset,inset,inset)
            toggle.thumbTintList=ColorStateList.valueOf(if(dark)Color.WHITE else 0xfffafafa.toInt())
            toggle.trackTintList=tint
            toggle.setTextColor(GlassFeatureStyle.primary(activity))
        }
        b.btnSleep.backgroundTintList=ColorStateList.valueOf(if(dark)0xff35353d.toInt() else 0xffe2e6ed.toInt())
        b.btnSleep.setTextColor(GlassFeatureStyle.primary(activity))

        // ---- audio quality
        b.groupQuality.check(
            when (Prefs.audioQuality) {
                Prefs.Q_LOW -> R.id.qLow
                Prefs.Q_HIGH -> R.id.qHigh
                Prefs.Q_320 -> R.id.q320
                else -> R.id.qNormal
            },
        )
        b.groupQuality.setOnCheckedChangeListener { _, id ->
            Prefs.audioQuality =
                when (id) {
                    R.id.qLow -> Prefs.Q_LOW
                    R.id.qHigh -> Prefs.Q_HIGH
                    R.id.q320 -> Prefs.Q_320
                    else -> Prefs.Q_NORMAL
                }
        }

        // ---- audio language
        b.groupLang.check(
            when (Prefs.audioLang) {
                "hi" -> R.id.lHindi
                "en" -> R.id.lEnglish
                "pa" -> R.id.lPunjabi
                else -> R.id.lOriginal
            },
        )
        b.groupLang.setOnCheckedChangeListener { _, id ->
            Prefs.audioLang =
                when (id) {
                    R.id.lHindi -> "hi"
                    R.id.lEnglish -> "en"
                    R.id.lPunjabi -> "pa"
                    else -> ""
                }
        }

        // ---- video quality
        b.groupVideo.check(
            when (Prefs.videoHeight) {
                360 -> R.id.v360
                480 -> R.id.v480
                720 -> R.id.v720
                else -> R.id.v1080
            },
        )
        b.groupVideo.setOnCheckedChangeListener { _, id ->
            Prefs.videoHeight =
                when (id) {
                    R.id.v360 -> 360
                    R.id.v480 -> 480
                    R.id.v720 -> 720
                    else -> 1080
                }
        }

        // ---- sound
        b.switchSilence.isChecked = Prefs.skipSilence
        val silenceTitle=b.switchSilence.text.toString()
        b.switchSilence.text="$silenceTitle · ${SmartSettingIndicators.onOff(Prefs.skipSilence)}"
        b.switchSilence.setOnCheckedChangeListener { _, checked ->
            Prefs.skipSilence = checked
            b.switchSilence.text="$silenceTitle · ${SmartSettingIndicators.onOff(checked)}"
            PlayerHub.applyAll()
        }
        b.switchNormalize.isChecked = Prefs.normalize
        val normalizeTitle=b.switchNormalize.text.toString()
        b.switchNormalize.text="$normalizeTitle · ${SmartSettingIndicators.onOff(Prefs.normalize)}"
        b.switchNormalize.setOnCheckedChangeListener { _, checked ->
            Prefs.normalize = checked
            b.switchNormalize.text="$normalizeTitle · ${SmartSettingIndicators.onOff(checked)}"
            PlayerHub.applyNormalize()
        }

        // ---- sleep timer: same action used by the full-player shortcut.
        fun refreshSleep() { b.sleepStatus.text = sleepLabel() }
        refreshSleep()
        if(!includeSleepTimer){
            b.sleepHeader.visibility=android.view.View.GONE
            b.sleepStatus.visibility=android.view.View.GONE
            b.btnSleep.visibility=android.view.View.GONE
        }
        b.btnSleep.setOnClickListener { showSleepTimer(activity){refreshSleep()} }

        MaterialAlertDialogBuilder(activity)
            .setBackground(GlassFeatureStyle.dialog(activity))
            .setTitle(R.string.settings)
            .setView(b.root)
            .setPositiveButton(R.string.close, null)
            .show()
    }
    fun sleepLabel():String = when {
        SleepTimer.untilSongEnd -> "End of song"
        SleepTimer.isActive -> "${((SleepTimer.remainingMs()+59_999L)/60_000L).coerceAtLeast(1)} min left"
        else -> "Off"
    }
    fun showSleepTimer(activity:Activity,onChanged:()->Unit={}) {
        val minutes=intArrayOf(15,30,45,60,90)
        val labels=listOf("Off")+minutes.map{"$it min"}+listOf("End of song")
        MaterialAlertDialogBuilder(activity).setBackground(GlassFeatureStyle.dialog(activity))
            .setTitle("Sleep Timer · ${sleepLabel()}")
            .setItems(labels.mapIndexed{i,label->if((i==0&&!SleepTimer.isActive)||(i==labels.lastIndex&&SleepTimer.untilSongEnd))"✓  $label" else label}.toTypedArray()){_,which->
                when(which){0->if(SleepTimer.isActive)SleepTimer.cancel();labels.lastIndex->SleepTimer.startEndOfSong();else->SleepTimer.startMinutes(minutes[which-1])}
                onChanged()
            }.setNegativeButton("Close",null).show()
    }
}
