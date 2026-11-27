package com.abhi.ganasuno.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import androidx.annotation.RequiresApi

/** API 31+: applies RenderEffect only to the captured underlying-content layer. */
@RequiresApi(31)
internal object GlassHardwareBlur {
    fun record(sample:Bitmap,width:Int,height:Int,radius:Float):Any {
        val node=RenderNode("ganasuno-backdrop")
        node.setPosition(0,0,width,height)
        val canvas=node.beginRecording(width,height)
        try {canvas.drawBitmap(sample,null,RectF(0f,0f,width.toFloat(),height.toFloat()),Paint(Paint.FILTER_BITMAP_FLAG))}
        finally {node.endRecording()}
        node.setRenderEffect(RenderEffect.createBlurEffect(radius,radius,Shader.TileMode.CLAMP))
        return node
    }
    fun draw(canvas:Canvas,node:Any){canvas.drawRenderNode(node as RenderNode)}
}
