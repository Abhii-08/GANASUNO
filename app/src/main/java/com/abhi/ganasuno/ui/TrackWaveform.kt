package com.abhi.ganasuno.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/** Waveform drawn from decoded audio peaks. Tap to seek. */
class TrackWaveform(context:Context,private val peaks:FloatArray,private val onSeek:(Float)->Unit):View(context){
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG);private var startX=0f;private var dragging=false;private var preview:Float?=null;private val slop=android.view.ViewConfiguration.get(context).scaledTouchSlop;var progress=0f;set(v){field=v.coerceIn(0f,1f);invalidate()}
    var onMarker:((Int)->Unit)?=null
    var markers:List<Float> = emptyList();set(v){field=v;invalidate()}
    init{contentDescription="Decoded audio waveform; tap to seek. Use the separate slider for accessible seeking.";isClickable=true;isFocusable=true;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES}
    override fun onDraw(c:Canvas){super.onDraw(c);if(peaks.isEmpty())return;val step=width.toFloat()/peaks.size;val mid=height/2f;paint.strokeWidth=(step*.65f).coerceAtLeast(1f);paint.strokeCap=Paint.Cap.ROUND;peaks.forEachIndexed{i,p->paint.color=if(i.toFloat()/peaks.size<=(preview?:progress))0xff16a34a.toInt()else 0xff94a3b8.toInt();val x=(i+.5f)*step;val h=abs(p).coerceIn(.025f,1f)*mid*.85f;c.drawLine(x,mid-h,x,mid+h,paint)};paint.color=0xfff59e0b.toInt();paint.strokeWidth=3f;markers.forEach{val x=it.coerceIn(0f,1f)*width;c.drawLine(x,0f,x,height.toFloat(),paint)}}
    override fun onTouchEvent(e:MotionEvent):Boolean{
        val p=com.abhi.ganasuno.data.Batch21.fraction(e.x,width)?:return false
        when(e.actionMasked){
            MotionEvent.ACTION_DOWN->{startX=e.x;dragging=false;preview=p;invalidate();return true}
            MotionEvent.ACTION_MOVE->{if(com.abhi.ganasuno.data.Batch21.dragged(startX,e.x,slop)){dragging=true;parent?.requestDisallowInterceptTouchEvent(true)};if(dragging){preview=p;invalidate()};return true}
            MotionEvent.ACTION_UP->{val selected=if(dragging)null else com.abhi.ganasuno.data.Batch20.nearestMarker(markers,p,(12*resources.displayMetrics.density/width).coerceAtMost(.06f));preview=null;parent?.requestDisallowInterceptTouchEvent(false);if(selected!=null&&onMarker!=null)onMarker?.invoke(selected)else onSeek(p);dragging=false;invalidate();performClick();return true}
            MotionEvent.ACTION_CANCEL->{preview=null;dragging=false;parent?.requestDisallowInterceptTouchEvent(false);invalidate();return true}
        };return super.onTouchEvent(e)
    }
    override fun onInitializeAccessibilityNodeInfo(info:android.view.accessibility.AccessibilityNodeInfo){
        super.onInitializeAccessibilityNodeInfo(info);info.className="android.widget.SeekBar";info.rangeInfo=android.view.accessibility.AccessibilityNodeInfo.RangeInfo.obtain(android.view.accessibility.AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT,0f,1f,progress)
        info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);info.addAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);info.addAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    }
    private fun moveTo(value:Float):Boolean{if(!value.isFinite())return false;progress=value.coerceIn(0f,1f);onSeek(progress);sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);return true}
    override fun performAccessibilityAction(action:Int,args:android.os.Bundle?):Boolean=when(action){
        android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id->args?.getFloat(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,Float.NaN)?.let{moveTo(it)}?:false
        android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD->moveTo(com.abhi.ganasuno.data.Batch22.seek(progress,.05f))
        android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD->moveTo(com.abhi.ganasuno.data.Batch22.seek(progress,-.05f))
        else->super.performAccessibilityAction(action,args)
    }
    override fun onKeyDown(keyCode:Int,event:android.view.KeyEvent):Boolean=when(keyCode){android.view.KeyEvent.KEYCODE_DPAD_LEFT->moveTo(com.abhi.ganasuno.data.Batch22.seek(progress,-.05f));android.view.KeyEvent.KEYCODE_DPAD_RIGHT->moveTo(com.abhi.ganasuno.data.Batch22.seek(progress,.05f));else->super.onKeyDown(keyCode,event)}
    override fun performClick():Boolean{super.performClick();return true}
}
