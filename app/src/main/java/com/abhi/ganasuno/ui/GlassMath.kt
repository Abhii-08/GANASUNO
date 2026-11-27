package com.abhi.ganasuno.ui
import kotlin.math.*
/** Pure functions for bounded, separable bitmap blur and damped selection motion. */
object GlassMath {
    fun spring(t:Float):Float {if(t<=0f)return 0f;if(t>=1f)return 1f;return (1.0-exp(-7.5*t)*cos(11.0*t)).toFloat()}
    fun blur(pixels:IntArray,temp:IntArray,w:Int,h:Int,r:Int){
        require(w>0&&h>0&&pixels.size==w*h&&temp.size==pixels.size&&r in 1..8)
        fun pass(src:IntArray,dst:IntArray,horizontal:Boolean){
            for(y in 0 until h)for(x in 0 until w){var a=0;var red=0;var green=0;var blue=0
                for(k in -r..r){val xx=if(horizontal)(x+k).coerceIn(0,w-1)else x;val yy=if(horizontal)y else (y+k).coerceIn(0,h-1);val p=src[yy*w+xx];a+=p ushr 24;red+=(p ushr 16)and 255;green+=(p ushr 8)and 255;blue+=p and 255}
                val n=r*2+1;dst[y*w+x]=((a/n) shl 24) or ((red/n) shl 16) or ((green/n) shl 8) or (blue/n)
            }
        }
        pass(pixels,temp,true);pass(temp,pixels,false);pass(pixels,temp,true);pass(temp,pixels,false)
    }
}
