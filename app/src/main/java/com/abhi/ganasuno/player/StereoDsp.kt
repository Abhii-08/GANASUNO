package com.abhi.ganasuno.player

import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.roundToInt

/** Streaming PCM stereo balance and feedback delay. No platform dependencies. */
class StereoDsp(private val rate:Int,private val channels:Int) {
    data class Options(val rotate:Boolean=false,val period:Int=8,val direction:Int=1,val delayMs:Int=0,val feedback:Int=20,val mix:Int=20)
    private val ring=FloatArray((rate/2+2)*channels)
    private var write=0;private var phase=0.0;private var prevDelay=0
    fun process(samples:ShortArray,options:Options) {
        require(samples.size%channels==0)
        val delay=(rate*options.delayMs.coerceIn(0,500)/1000)*channels
        if(delay!=prevDelay){ring.fill(0f);write=0;prevDelay=delay}
        if(!options.rotate&&delay==0)return
        val feedback=options.feedback.coerceIn(0,65)/100f;val wet=options.mix.coerceIn(0,45)/100f
        val step=2*PI/(rate*options.period.coerceIn(3,30))*if(options.direction<0)-1 else 1
        for(frame in samples.indices step channels){
            val pan=if(options.rotate&&channels==2)sin(phase).toFloat()*.85f else 0f
            for(ch in 0 until channels){
                val gain=if(channels!=2)1f else if(ch==0)1f-pan.coerceAtLeast(0f) else 1f+pan.coerceAtMost(0f)
                val dry=samples[frame+ch]*gain
                val echo=if(delay>0)ring[(write+ring.size-delay)%ring.size] else 0f
                if(delay>0){ring[write]=(dry+echo*feedback).coerceIn(-32768f,32767f);write=(write+1)%ring.size}
                samples[frame+ch]=(if(delay>0)dry*(1f-wet)+echo*wet else dry).roundToInt().coerceIn(-32768,32767).toShort()
            }
            if(options.rotate){phase+=step;if(phase>2*PI)phase-=2*PI;if(phase<0)phase+=2*PI}
        }
    }
}
