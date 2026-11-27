package com.abhi.ganasuno.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Shared portrait and landscape lyrics renderer. One ticker per open dialog. */
class KaraokePanel(context:Context,raw:String,private val position:()->Long?,private val seek:(Long)->Unit,lyricTextSize:Float=22f,foreground:Int?=null):ScrollView(context) {
    private val lines=LrcParser.parse(raw)
    private val prepared=lines.mapIndexed{i,l->KaraokeTiming.prepare(l.text,lines.getOrNull(i+1)?.ms)}
    private val body=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(18,16,18,24)}
    private val handler=Handler(Looper.getMainLooper())
    private val views:List<TextView>
    private val primary:Int
    private val secondary:Int
    private var last=-2
    private var lastPosition:Long?=null
    private var running=false
    init {
        addView(body)
        primary=foreground?:TextView(context).currentTextColor
        secondary=Color.argb((Color.alpha(primary)*.55f).toInt(),Color.red(primary),Color.green(primary),Color.blue(primary))
        val texts=if(lines.isEmpty())raw.lines()else prepared.map{it.text}
        views=texts.mapIndexed{i,value->TextView(context).apply{
            text=value;this.textSize=lyricTextSize;setTextColor(if(lines.isEmpty())primary else secondary)
            setTypeface(null,Typeface.BOLD);setPadding(10,14,10,14)
            if(i<lines.size)setOnClickListener{seek(lines[i].ms)}
            body.addView(this,LinearLayout.LayoutParams(-1,-2))
        }}
    }
    private val ticker=object:Runnable{override fun run(){
        if(!running)return
        val p=position();val active=if(p==null)-1 else lines.indexOfLast{it.ms<=p}
        if(active!=last){
            if(last in views.indices&&last<prepared.size){views[last].text=prepared[last].text;views[last].setTextColor(secondary)}
            if(active in views.indices){views[active].setTextColor(primary);smoothScrollTo(0,(views[active].top-height/3).coerceAtLeast(0))}
            last=active;lastPosition=null
        }
        if(active>=0&&p!=null&&p!=lastPosition){views[active].text=LyricHighlight.render(prepared[active],p,primary);lastPosition=p}
        handler.postDelayed(this,33)
    }}
    fun start(){if(!running){running=true;handler.post(ticker)}}
    fun stop(){running=false;handler.removeCallbacks(ticker)}
    override fun onDetachedFromWindow(){stop();super.onDetachedFromWindow()}
}
