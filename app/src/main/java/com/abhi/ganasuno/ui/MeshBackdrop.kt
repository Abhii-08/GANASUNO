package com.abhi.ganasuno.ui

import android.graphics.*
import android.graphics.drawable.Drawable
import kotlin.math.sin

/** Animated layered radial colour fields, with a dark overlay for text contrast. */
class MeshBackdrop:Drawable(){
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val hues=FloatArray(3)
    private var phase=0f
    fun track(id:String){val start=(id.hashCode().toLong() and 0xffffffffL)%360;for(i in hues.indices)hues[i]=((start+i*77)%360).toFloat();invalidateSelf()}
    fun advance(){phase=(phase+.035f)%6.283185f;invalidateSelf()}
    override fun draw(c:Canvas){val w=bounds.width().toFloat();val h=bounds.height().toFloat();if(w<=0||h<=0)return;c.save();c.translate(bounds.left.toFloat(),bounds.top.toFloat());c.clipRect(0f,0f,w,h);paint.shader=null;paint.color=0xff101321.toInt();c.drawRect(0f,0f,w,h,paint)
        for(i in hues.indices){val x=w*(.18f+.32f*i+.09f*sin(phase+i*2));val y=h*(.18f+.28f*i+.09f*sin(phase+i));val color=Color.HSVToColor(210,floatArrayOf(hues[i],.72f,.75f));paint.color=Color.WHITE;paint.shader=RadialGradient(x,y,maxOf(w,h)*.75f,intArrayOf(color,Color.TRANSPARENT),null,Shader.TileMode.CLAMP);c.drawRect(0f,0f,w,h,paint)}
        paint.shader=null;paint.color=0x66000000;c.drawRect(0f,0f,w,h,paint);c.restore()}
    override fun setAlpha(alpha:Int){};override fun setColorFilter(filter:ColorFilter?){};@Deprecated("Deprecated in Android") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
