package com.abhi.ganasuno.ui

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.abhi.ganasuno.R

/** Lightweight layered material: the translucent cards reveal the shared ambient background.
 * The dock retains its independent sampled backdrop blur; cards do not snapshot themselves. */
object GlassFeatureStyle {
    fun dp(c: Context, n: Int) = (n * c.resources.displayMetrics.density + .5f).toInt()
    fun dark(c: Context) = c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    fun primary(c: Context) = if(dark(c)) Color.WHITE else 0xff19191d.toInt()
    fun secondary(c: Context) = if(dark(c)) 0xffc4c4c4.toInt() else 0xff595d65.toInt()
    fun chevron(c: Context) = if(dark(c)) 0xffa0a0a0.toInt() else 0xff646872.toInt()
    private fun shape(c: Context, colors: IntArray, radius: Int, edge: Int = 0): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
            cornerRadius = dp(c,radius).toFloat()
            if(edge != 0) setStroke(dp(c,1),edge)
        }
    fun surface(c: Context, radius: Int = 24): Drawable {
        val dark = dark(c)
        return LayerDrawable(arrayOf(
            shape(c,if(dark) intArrayOf(0x98282830.toInt(),0xa0111115.toInt()) else intArrayOf(0xf8ffffff.toInt(),0xdde9edf2.toInt()),radius),
            shape(c,if(dark) intArrayOf(0x14ffffff,0x00ffffff,0x09ffffff) else intArrayOf(0x80ffffff.toInt(),0x00ffffff,0x18ffffff),radius,if(dark)0x2affffff else 0x317a8290)
        ))
    }
    fun dialog(c:Context): Drawable = shape(c,if(dark(c))intArrayOf(0xff272731.toInt(),0xff161619.toInt()) else intArrayOf(Color.WHITE,0xffeff1f5.toInt()),28,if(dark(c))0x40ffffff else 0x337a8290)
    fun ripple(c: Context,radius: Int = 24): Drawable = RippleDrawable(ColorStateList.valueOf(if(dark(c))0x25ffffff else 0x19000000),null,shape(c,intArrayOf(Color.WHITE,Color.WHITE),radius))
    fun ambient(c:Context): Drawable = if(dark(c)) GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(0xff000000.toInt(),0xff10121d.toInt(),0xff110e17.toInt(),0xff000000.toInt())) else GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(0xfff9f9fc.toInt(),0xffedf1f8.toInt(),0xfff4edf3.toInt(),0xfffafafc.toInt()))
    fun icon(c:Context,color:Int):Drawable = LayerDrawable(arrayOf(
        shape(c,intArrayOf(ColorUtils.blendARGB(color,Color.WHITE,.17f),color,ColorUtils.blendARGB(color,Color.BLACK,.16f)),15),
        shape(c,intArrayOf(0x26ffffff,0x00ffffff,0x09ffffff),15,0x40ffffff)
    ))
    data class Feature(val art:Int,val color:Int)
    fun feature(group:String,title:String):Feature {
        fun f(id:Int,color:Long)=Feature(id,color.toInt())
        val t=title.lowercase(java.util.Locale.ROOT)
        return when {
            group=="Settings" && t=="appearance" -> f(R.drawable.glass_feature_appearance,0xff7452a3)
            group=="Settings" && t=="playback" -> f(R.drawable.glass_feature_play,0xff416b95)
            group=="Settings" && t=="sound & effects" -> f(R.drawable.glass_feature_sound,0xff377c78)
            group=="Settings" && t=="discovery" -> f(R.drawable.glass_feature_compass,0xff98743f)
            group=="Settings" && t=="storage" -> f(R.drawable.glass_feature_storage,0xff487b83)
            group=="Settings" && t=="account & integrations" -> f(R.drawable.glass_feature_link,0xff326a98)
            group=="Settings" && t=="system & privacy" -> f(R.drawable.glass_feature_shield,0xff636870)
            // Search shortcuts use their own meaningful artwork, but the same Library material.
            group=="Browse by type" && t=="songs" -> f(R.drawable.glass_feature_music,0xffa43f65)
            group=="Browse by type" && t=="videos" -> f(R.drawable.glass_feature_video,0xff7452a3)
            group=="More ways to explore" && t=="suggestions" -> f(R.drawable.glass_feature_search,0xff636870)
            group=="More ways to explore" && t=="online catalogue" -> f(R.drawable.glass_feature_globe,0xff326a98)
            group=="More ways to explore" && t=="songs, artists and albums" -> f(R.drawable.glass_feature_people,0xff2c797c)
            group=="More ways to explore" && t=="live stations" -> f(R.drawable.glass_feature_radio,0xff725ca5)
            group=="More ways to explore" && t=="moods and genres" -> f(R.drawable.glass_feature_compass,0xff98743f)
            group=="Library" && t=="collection" -> f(R.drawable.glass_feature_collection,0xff98743f)
            group=="Library" && t=="listening" -> f(R.drawable.glass_feature_history,0xff526e9b)
            group=="Library" && t=="storage" -> f(R.drawable.glass_feature_storage,0xff4f718b)
            t=="favourites" -> f(R.drawable.glass_feature_heart,0xffa43f65)
            t=="offline audio" || t=="offline downloads" || t=="offline music" || t.startsWith("download") -> f(R.drawable.glass_feature_download,0xff3e639b)
            t=="import audio" || t.startsWith("import") -> f(R.drawable.glass_feature_upload,0xff7452a3)
            t=="artists and albums" || t=="artists" -> f(R.drawable.glass_feature_people,0xff2c797c)
            t=="favourite collections" -> f(R.drawable.glass_feature_collection,0xff98743f)
            t=="podcasts" -> f(R.drawable.glass_feature_podcast,0xff725ca5)
            t=="followed artists" -> f(R.drawable.glass_feature_follow,0xff326a98)
            t=="search library" || t.contains("search history") -> f(R.drawable.glass_feature_search,0xff636870)
            t=="albums" || t.contains("artwork") -> f(R.drawable.glass_feature_album,0xff7452a3)
            t.contains("playlist") || group=="Playlists" -> f(R.drawable.glass_feature_playlist,0xff416b85)
            t.contains("most played") || t.contains("analytics") || t.contains("fingerprint") -> f(R.drawable.glass_feature_chart,0xff947640)
            t.contains("history") || t.contains("recently") -> f(R.drawable.glass_feature_history,0xff526e9b)
            t.contains("language") || t.contains("country") -> f(R.drawable.glass_feature_globe,0xff307b80)
            t.contains("theme") || t.contains("glass") || t.contains("style") -> f(R.drawable.glass_feature_appearance,0xff7452a3)
            t.contains("equalizer") || t.contains("software eq") || t.contains("layout") || t.contains("manage") -> f(R.drawable.glass_feature_sliders,0xff487b83)
            t.contains("audio output") -> f(R.drawable.glass_feature_headphones,0xff476b93)
            t.contains("privacy") -> f(R.drawable.glass_feature_shield,0xff5e6675)
            t.contains("support") || t.contains("diagnostics") || t.contains("about") -> f(R.drawable.glass_feature_info,0xff626975)
            group=="Playback" -> f(R.drawable.glass_feature_play,0xff47749a)
            group=="Sound" -> f(R.drawable.glass_feature_sound,0xff377c78)
            group=="Lyrics" -> f(R.drawable.glass_feature_lyrics,0xff9d5378)
            group=="Discovery" -> f(R.drawable.glass_feature_compass,0xff937341)
            group=="Storage" -> f(R.drawable.glass_feature_storage,0xff4f718b)
            group=="Integrations" -> f(R.drawable.glass_feature_link,0xff6b5c9a)
            group=="System & privacy" -> f(R.drawable.glass_feature_shield,0xff626975)
            group=="Listening insights" || group=="Listening" -> f(R.drawable.glass_feature_chart,0xff947640)
            group=="Player & appearance" || group=="Appearance" -> f(R.drawable.glass_feature_appearance,0xff7452a3)
            else -> f(R.drawable.glass_feature_music,0xff586e91)
        }
    }
    fun press(view:View,reduced:Boolean) {
        view.setOnTouchListener { v,event ->
            if(!reduced && ValueAnimator.areAnimatorsEnabled()) when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(.985f).scaleY(.985f).setDuration(90).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }
            false // Native click, scrolling and accessibility remain in control.
        }
    }
}

class GlassFeatureCard(context: Context, group:String, title:String, detail:String, reduced:Boolean, action:()->Unit) : LinearLayout(context) {
    private fun dp(n:Int)=GlassFeatureStyle.dp(context,n)
    private val baseDescription="$title. $detail"
    private val statusLabel=TextView(context).apply {
        textSize=12f;setTextColor(GlassFeatureStyle.primary(context));setPadding(0,dp(4),0,0)
        visibility=View.GONE
    }
    fun setCurrentStatus(value:String?){
        val label=value?.let{"Current: $it"}.orEmpty()
        if(statusLabel.text.toString()==label)return
        statusLabel.text=label;statusLabel.visibility=if(value==null)View.GONE else View.VISIBLE
        contentDescription=if(value==null)baseDescription else "$baseDescription. $label"
    }
    init {
        orientation=HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; minimumHeight=dp(76)
        setPadding(dp(13),dp(11),dp(12),dp(11)); background=GlassFeatureStyle.surface(context)
        foreground=GlassFeatureStyle.ripple(context); isClickable=true; isFocusable=true
        contentDescription="$title. $detail"; elevation=dp(1).toFloat()
        val feature=GlassFeatureStyle.feature(group,title)
        val icon=ImageView(context).apply {
            background=GlassFeatureStyle.icon(context,feature.color); setImageResource(feature.art)
            scaleType=ImageView.ScaleType.FIT_CENTER;setPadding(dp(11),dp(11),dp(11),dp(11))
            importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        addView(icon,LayoutParams(dp(46),dp(46)))
        val labels=LinearLayout(context).apply {orientation=VERTICAL;importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS}
        labels.addView(TextView(context).apply {text=title;textSize=16f;typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL);setTextColor(GlassFeatureStyle.primary(context))},LayoutParams(-1,-2))
        labels.addView(TextView(context).apply {text=detail;textSize=13f;setTextColor(GlassFeatureStyle.secondary(context));setPadding(0,dp(3),0,0)},LayoutParams(-1,-2))
        labels.addView(statusLabel,LayoutParams(-1,-2))
        addView(labels,LayoutParams(0,-2,1f).apply{marginStart=dp(12);marginEnd=dp(8)})
        addView(ImageView(context).apply {setImageResource(R.drawable.glass_feature_chevron);setColorFilter(GlassFeatureStyle.chevron(context));importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO},LayoutParams(dp(19),dp(19)))
        setOnClickListener{action()};GlassFeatureStyle.press(this,reduced)
    }
}
