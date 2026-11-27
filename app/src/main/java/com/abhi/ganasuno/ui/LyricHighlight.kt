package com.abhi.ganasuno.ui

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan

/** Progressive karaoke colour fill without font-size jumps or fabricated word timestamps. */
object LyricHighlight {
    private fun blend(a:Int,b:Int,f:Float):Int {
        fun part(x:Int,y:Int)=(x+(y-x)*f.coerceIn(0f,1f)).toInt()
        return Color.argb(part(Color.alpha(a),Color.alpha(b)),part(Color.red(a),Color.red(b)),part(Color.green(a),Color.green(b)),part(Color.blue(a),Color.blue(b)))
    }
    fun render(line:KaraokeTiming.Line,position:Long,foreground:Int):CharSequence {
        if(line.segments.isEmpty())return line.text
        val muted=Color.argb((Color.alpha(foreground)*.42f).toInt(),Color.red(foreground),Color.green(foreground),Color.blue(foreground))
        val out=SpannableStringBuilder(line.text)
        if(out.isEmpty())return out
        out.setSpan(StyleSpan(Typeface.BOLD),0,out.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        // Non-overlapping spans keep the paint deterministic across Android versions.
        var at=0
        for(fill in KaraokeTiming.fills(line,position)) {
            if(fill.start>at)out.setSpan(ForegroundColorSpan(muted),at,fill.start,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if(fill.end>fill.start)out.setSpan(ForegroundColorSpan(blend(muted,foreground,fill.fraction)),fill.start,fill.end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            at=fill.end
        }
        if(at<out.length)out.setSpan(ForegroundColorSpan(muted),at,out.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return out
    }
}
