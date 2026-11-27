package com.abhi.ganasuno.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.abhi.ganasuno.R
import com.abhi.ganasuno.data.Song

/** Reusable native artwork shelves. All callbacks are supplied by the activity. */
class PremiumUi(private val context: Context, private val target: LinearLayout) {
    private fun dp(n:Int) = (n*context.resources.displayMetrics.density+.5f).toInt()
    private fun text(value:String,size:Float,bold:Boolean=false)=TextView(context).apply {
        text=value;textSize=size;if(bold)setTypeface(null,Typeface.BOLD)
    }
    private fun rounded(color:Int,radius:Int)=GradientDrawable().apply{setColor(color);cornerRadius=dp(radius).toFloat()}
    private fun press(v:View,label:String,action:()->Unit) {
        v.isFocusable=true;v.contentDescription=label;v.minimumHeight=dp(48)
        v.foreground=RippleDrawable(ColorStateList.valueOf(0x33ffffff),null,rounded(Color.WHITE,12))
        v.setOnClickListener{action()}
    }
    fun message(value:String):TextView=text(value,14f).apply {setPadding(dp(20),dp(12),dp(20),dp(12));target.addView(this)}
    private fun heading(value:String,more:(()->Unit)?=null) {
        val row=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(20),dp(12),dp(20),dp(6))}
        row.addView(text(value,22f,true),LinearLayout.LayoutParams(0,-2,1f))
        if(more!=null)row.addView(text("See all ›",14f,true).apply{gravity=Gravity.CENTER;setPadding(dp(10),0,0,0);setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.brand_primary));press(this,"See all $value",more)})
        target.addView(row)
    }
    fun shelf(title:String,tracks:List<Song>,more:()->Unit,play:(Song)->Unit) {
        if(tracks.isEmpty())return
        heading(title,more)
        val row=LinearLayout(context).apply{setPadding(dp(20),0,dp(8),dp(12))}
        val scroll=HorizontalScrollView(context).apply{isHorizontalScrollBarEnabled=false;clipToPadding=false;addView(row)}
        val size=dp((context.resources.configuration.screenWidthDp/3).coerceIn(110,180))
        tracks.take(12).forEach{song->
            val card=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL}
            val art=ImageView(context).apply{scaleType=ImageView.ScaleType.CENTER_CROP;background=rounded(0xff27272a.toInt(),12);clipToOutline=true;importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO}
            card.addView(art,LinearLayout.LayoutParams(size,size));ImageLoader.load(song.thumb,art,R.drawable.ic_placeholder)
            card.addView(text(song.title,15f,true).apply{maxLines=2;ellipsize=TextUtils.TruncateAt.END;setPadding(0,dp(6),0,0)})
            card.addView(text(song.artist,13f).apply{maxLines=1;ellipsize=TextUtils.TruncateAt.END;alpha=.72f})
            press(card,"Play ${song.title} by ${song.artist}"){play(song)}
            row.addView(card,LinearLayout.LayoutParams(size,-2).apply{marginEnd=dp(12)})
        }
        target.addView(scroll,LinearLayout.LayoutParams(-1,-2))
    }
    fun links(title:String,links:List<Pair<String,()->Unit>>) {
        heading(title)
        links.chunked(2).forEach{pair->
            val row=LinearLayout(context).apply{setPadding(dp(20),0,dp(20),dp(10))}
            pair.forEachIndexed{i,(label,action)->
                val tile=text(label+"  ›",16f,true).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(12),dp(12),dp(12));setTextColor(Color.WHITE);background=rounded(0xff242428.toInt(),14);press(this,label,action)}
                row.addView(tile,LinearLayout.LayoutParams(0,-2,1f).apply{if(i==0)marginEnd=dp(10)})
            }
            target.addView(row)
        }
    }
    companion object {
        fun applyArtwork(view:View,bitmap:Bitmap,radius:Float){
            val next=artBackground(bitmap,radius)
            val previous=view.background
            if(previous==null||!android.animation.ValueAnimator.areAnimatorsEnabled())view.background=next
            else {
                val transition=android.graphics.drawable.TransitionDrawable(arrayOf(previous,next))
                transition.isCrossFadeEnabled=true;view.background=transition;transition.startTransition(300)
                view.postDelayed({if(view.background===transition)view.background=next},310)
            }
        }
        /** Sample a few artwork pixels; darken for consistent white-text contrast. No extra dependency. */
        fun artBackground(bitmap:Bitmap,radius:Float):GradientDrawable {
            fun sample(x:Float,y:Float):Int {
                val color=bitmap.getPixel((x*(bitmap.width-1)).toInt(),(y*(bitmap.height-1)).toInt())
                return Color.rgb((Color.red(color)*.30).toInt(),(Color.green(color)*.30).toInt(),(Color.blue(color)*.30).toInt())
            }
            return GradientDrawable(GradientDrawable.Orientation.BL_TR,intArrayOf(sample(.2f,.8f),sample(.5f,.5f),sample(.8f,.2f))).apply{cornerRadius=radius}
        }
    }
}
