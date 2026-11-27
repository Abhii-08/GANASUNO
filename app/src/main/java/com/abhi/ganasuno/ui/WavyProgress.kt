package com.abhi.ganasuno.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.widget.SeekBar
import kotlin.math.*

/** Native accessible SeekBar with an expressive sine-wave track and ordinary seek gestures. */
class WavyProgress(context:Context):SeekBar(context) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val path=Path()
    init { progressDrawable=android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT);thumb=android.graphics.drawable.GradientDrawable().apply{shape=android.graphics.drawable.GradientDrawable.OVAL;setColor(0xffa78bfa.toInt());setSize(dp(18),dp(18))};thumbOffset=dp(9);minimumHeight=dp(48);contentDescription="Playback position" }
    private fun dp(n:Int)=(n*resources.displayMetrics.density).roundToInt()
    override fun onDraw(canvas:Canvas) {
        val l=paddingLeft.toFloat();val r=(width-paddingRight).toFloat();val y=(paddingTop+height-paddingBottom)/2f
        val span=(r-l).coerceAtLeast(0f);val f=if(max>0)progress.toFloat()/max else 0f
        val rtl=layoutDirection==LAYOUT_DIRECTION_RTL
        fun x(d:Float)=if(rtl)r-d else l+d
        p.style=Paint.Style.STROKE;p.strokeCap=Paint.Cap.ROUND;p.strokeWidth=dp(4).toFloat();p.color=0xff777783.toInt()
        canvas.drawLine(x(span*f),y,x(span),y,p)
        path.reset();path.moveTo(x(0f),y);val end=span*f;var d=0f
        while(d<end){val amp=min(1f,min(d,end-d)/dp(8).coerceAtLeast(1));path.lineTo(x(d),y+dp(4)*amp*sin(d/dp(5).coerceAtLeast(1)));d+=dp(2).coerceAtLeast(1)}
        path.lineTo(x(end),y);p.color=0xffa78bfa.toInt();canvas.drawPath(path,p)
        super.onDraw(canvas)
    }
}
