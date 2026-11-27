package com.abhi.ganasuno.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.TextView
import android.content.res.ColorStateList
import kotlin.math.*

/** The source is a sibling content layer, never this surface or its foreground controls. */
class LiquidGlassSurface @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val bounds = RectF()
    private val clip = Path()
    private var sample: Bitmap? = null
    private var hardwareBlur: Any? = null
    private var peak = 0
    private var pixels = IntArray(0)
    private var scratch = IntArray(0)
    private var artwork = intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT)
    private var tintAnimation: ValueAnimator? = null
    private var selectionAnimation: ValueAnimator? = null
    private var selected: View? = null
    private var selectionX = Float.NaN
    private var selectionWidth = 0f
    private var stretch = 0f
    private var dark = true
    private var capsule = false
    private var reduced = false
    private fun dp(n: Float) = n * resources.displayMetrics.density
    init { setWillNotDraw(false) }

    fun configure(isDark: Boolean, isCapsule: Boolean, reducedEffects: Boolean) {
        dark = isDark; capsule = isCapsule; reduced = reducedEffects
        // Transparent background supplies the outline for native elevation only.
        background = GradientDrawable().apply { setColor(Color.TRANSPARENT); cornerRadius = dp(if(capsule) 50f else 26f) }
        if (reduced) { sample = null; hardwareBlur = null; peak=0; selectionAnimation?.cancel(); tintAnimation?.cancel();selected?.let{select(it,false)} }
        invalidate()
    }
    fun setArtwork(colors: IntArray) {
        tintAnimation?.cancel()
        val old = artwork.copyOf()
        if (reduced || !ValueAnimator.areAnimatorsEnabled()) { artwork = colors.copyOf(); invalidate(); return }
        tintAnimation = ValueAnimator.ofFloat(0f,1f).apply {
            duration=300
            addUpdateListener { a -> val f=a.animatedValue as Float; artwork=IntArray(2){i->androidx.core.graphics.ColorUtils.blendARGB(old[i],colors[i],f)};invalidate() }
            start()
        }
    }
    fun select(tab: View?, animate: Boolean) {
        selected = tab
        selectionAnimation?.cancel(); stretch=0f
        if(tab==null) { selectionX=Float.NaN; invalidate();return }
        if(tab.width==0) { selectionX=Float.NaN; return }
        val destination=tab.left.toFloat()+dp(2f)
        val targetWidth=(tab.width-dp(4f)).coerceAtLeast(1f)
        if(!animate||!ValueAnimator.areAnimatorsEnabled()||selectionX.isNaN()||reduced) { selectionX=destination;selectionWidth=targetWidth;invalidate();return }
        val from=selectionX;val fromWidth=selectionWidth
        selectionAnimation=ValueAnimator.ofFloat(0f,1f).apply {
            duration=380
            addUpdateListener { a ->
                val t=a.animatedFraction
                val spring=GlassMath.spring(t)
                selectionX=from+(destination-from)*spring
                selectionWidth=fromWidth+(targetWidth-fromWidth)*spring
                stretch=dp(7f)*sin(t*PI).toFloat()*if(destination>=from)1f else -1f
                invalidate()
            }
            start()
        }
    }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) { super.onSizeChanged(w,h,oldw,oldh);sample=null;hardwareBlur=null }
    override fun onLayout(changed:Boolean,l:Int,t:Int,r:Int,b:Int){super.onLayout(changed,l,t,r,b);val tab=selected;if(tab!=null&&tab.width>0&&(changed||selectionX.isNaN()))select(tab,false)}

    /** Snapshot only the actual content under this surface; bounded resolution and throttled by the dock. */
    fun capture(content: View) {
        if(reduced || !isShown || width<=0 || height<=0 || content.width<=0) return
        val sw=(width/8).coerceIn(1,128);val sh=(height/8).coerceIn(1,48)
        try {
            val bitmap=sample?.takeIf{it.width==sw&&it.height==sh} ?: Bitmap.createBitmap(sw,sh,Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(if(dark)Color.BLACK else Color.WHITE)
            val a=IntArray(2);val b=IntArray(2);getLocationInWindow(a);content.getLocationInWindow(b)
            val c=Canvas(bitmap)
            c.scale(sw.toFloat()/width,sh.toFloat()/height)
            c.translate((b[0]-a[0]).toFloat(),(b[1]-a[1]).toFloat())
            content.draw(c)
            val n=sw*sh
            if(pixels.size!=n){pixels=IntArray(n);scratch=IntArray(n)}
            bitmap.getPixels(pixels,0,sw,0,0,sw,sh)
            val radius=(dp(26f)*sw/width).roundToInt().coerceIn(1,8)
            peak=pixels.maxOf { p->maxOf(Color.red(p),Color.green(p),Color.blue(p)) }
            hardwareBlur=if(android.os.Build.VERSION.SDK_INT>=31&&isHardwareAccelerated)runCatching{GlassHardwareBlur.record(bitmap,width,height,dp(28f))}.getOrNull() else null
            if(hardwareBlur==null){GlassMath.blur(pixels,scratch,sw,sh,radius);bitmap.setPixels(pixels,0,sw,0,0,sw,sh)}
            sample=bitmap;invalidate()
        } catch (_: RuntimeException) { sample=null;hardwareBlur=null;peak=0;invalidate() }
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w=width.toFloat();val h=height.toFloat();if(w<=0||h<=0)return
        bounds.set(0f,0f,w,h);val r=if(capsule)h/2 else dp(26f).coerceAtMost(h/2)
        clip.reset();clip.addRoundRect(bounds,r,r,Path.Direction.CW)
        val save=canvas.save();canvas.clipPath(clip)
        paint.style=Paint.Style.FILL;paint.shader=null
        paint.color=if(dark)0xff141414.toInt() else 0xffeeeeee.toInt();canvas.drawRect(bounds,paint)
        val node=hardwareBlur
        if(android.os.Build.VERSION.SDK_INT>=31&&canvas.isHardwareAccelerated&&node!=null)GlassHardwareBlur.draw(canvas,node)
        else sample?.let { paint.alpha=255;canvas.drawBitmap(it,null,bounds,paint) }
        if(dark){
            // Keep album accents subtle and ensure grey navigation labels remain legible.
            paint.alpha=255;paint.shader=LinearGradient(0f,h,w,0f,artwork,null,Shader.TileMode.CLAMP);canvas.drawRect(bounds,paint);paint.shader=null
            val artPeak=artwork.maxOf { c->maxOf(Color.red(c),Color.green(c),Color.blue(c))*Color.alpha(c)/255 }
            val brightest=(maxOf(peak,20)+artPeak).coerceIn(20,255)
            val opacity=(1f-42f/brightest).coerceIn(.25f,.86f)
            paint.color=Color.argb((opacity*255).roundToInt(),0,0,0)
        } else paint.color=0xd9ffffff.toInt()
        canvas.drawRect(bounds,paint)
        paint.alpha=255;paint.shader=LinearGradient(0f,0f,0f,h,intArrayOf(if(dark)0x22ffffff else 0x66ffffff,0x00ffffff,0x10ffffff),floatArrayOf(0f,.42f,1f),Shader.TileMode.CLAMP)
        canvas.drawRect(bounds,paint);paint.shader=null
        if(!selectionX.isNaN()&&selected!=null){
            val left=selectionX-min(0f,stretch);val right=selectionX+selectionWidth+max(0f,stretch)
            val rect=RectF(left,dp(5f),right,h-dp(5f));val sr=rect.height()/2
            paint.color=if(dark)0x28ffffff else 0x22000000;canvas.drawRoundRect(rect,sr,sr,paint)
            paint.style=Paint.Style.STROKE;paint.strokeWidth=dp(.8f);paint.color=if(dark)0x47ffffff else 0x33ffffff;canvas.drawRoundRect(rect,sr,sr,paint);paint.style=Paint.Style.FILL
        }
        canvas.restoreToCount(save)
        paint.style=Paint.Style.STROKE;paint.strokeWidth=dp(.8f)
        paint.alpha=255;paint.shader=LinearGradient(0f,0f,w,h,intArrayOf(0x78ffffff,0x1bffffff,0x40ffffff),null,Shader.TileMode.CLAMP)
        bounds.inset(dp(.6f),dp(.6f));canvas.drawRoundRect(bounds,r,r,paint);paint.shader=null;paint.style=Paint.Style.FILL
    }
    override fun onDetachedFromWindow(){selectionAnimation?.cancel();tintAnimation?.cancel();hardwareBlur=null;sample=null;super.onDetachedFromWindow()}
}

/** Coordinates backdrop capture, scroll clearance and artwork tint; does not own playback. */
class LiquidGlassDock(private val content:View,private val dock:View,private val nav:LiquidGlassSurface,private val mini:LiquidGlassSurface,private val scrollViews:List<View>,private val dark:Boolean) {
    private val base=scrollViews.map{it.paddingBottom}
    private var active=false
    private var queued=false
    private var reduced=false
    private val capture=Runnable {queued=false;if(active&&!reduced){nav.capture(content);mini.capture(content)}}
    private val scrollListener=ViewTreeObserver.OnScrollChangedListener{refresh()}
    private val layoutListener=ViewTreeObserver.OnGlobalLayoutListener{clearance();refresh()}
    private var observer:ViewTreeObserver?=null
    private var colors=intArrayOf(Color.TRANSPARENT,Color.TRANSPARENT)
    fun configure(lowEffects:Boolean){reduced=lowEffects;nav.configure(dark,true,reduced);mini.configure(dark,false,reduced);nav.setArtwork(colors);mini.setArtwork(colors);refresh()}
    fun start(){if(active)return;active=true;observer=content.viewTreeObserver.also{it.addOnScrollChangedListener(scrollListener);it.addOnGlobalLayoutListener(layoutListener)};clearance();refresh()}
    fun stop(){active=false;dock.removeCallbacks(capture);queued=false;observer?.takeIf{it.isAlive}?.let{it.removeOnScrollChangedListener(scrollListener);it.removeOnGlobalLayoutListener(layoutListener)};observer=null}
    fun clearance(){ val height=if(dock.visibility==View.VISIBLE)dock.height else 0;scrollViews.forEachIndexed{i,v->val bottom=base[i]+height;if(v.paddingBottom!=bottom)v.setPadding(v.paddingLeft,v.paddingTop,v.paddingRight,bottom)} }
    fun refresh(){if(!active||reduced||queued||dock.visibility!=View.VISIBLE)return;queued=true;dock.postDelayed(capture,125)}
    fun artwork(bitmap:Bitmap?){
        colors=if(bitmap==null)intArrayOf(Color.TRANSPARENT,Color.TRANSPARENT)else runCatching {
            fun color(x:Float,y:Float):Int {val c=bitmap.getPixel((x*(bitmap.width-1)).toInt(),(y*(bitmap.height-1)).toInt());return Color.argb(110,Color.red(c),Color.green(c),Color.blue(c))}
            intArrayOf(color(.2f,.75f),color(.8f,.25f))
        }.getOrDefault(intArrayOf(Color.TRANSPARENT,Color.TRANSPARENT));nav.setArtwork(colors);mini.setArtwork(colors);refresh()
    }
    fun tabs(buttons:List<TextView>,index:Int){
        buttons.forEachIndexed{i,v->val chosen=i==index;val color=if(dark)if(chosen)0xffc4c4c4.toInt() else 0xffa0a0a0.toInt() else if(chosen)0xff303030.toInt() else 0xff595959.toInt();v.isSelected=chosen;v.alpha=1f;v.setTextColor(color);v.compoundDrawableTintList=ColorStateList.valueOf(color);v.setTypeface(null,android.graphics.Typeface.NORMAL)}
        nav.select(buttons.getOrNull(index),true);refresh()
    }
}
