package com.abhi.ganasuno.player
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.abhi.ganasuno.data.Batch10
import androidx.media3.common.Player
object ManualFade {
    private val handler=Handler(Looper.getMainLooper())
    private var step:Runnable?=null
    val isActive:Boolean get()=step!=null
    fun cancel(){step?.let{handler.removeCallbacks(it)};step=null;PlayerHub.player?.volume=PlayerHub.baseVolume()}
    fun start(inward:Boolean,duration:Int){
        PlayerHub.cancelTransition?.invoke();cancel();SleepTimer.cancel();val p=PlayerHub.player?:return
        val start=if(inward)0f else p.volume;val end=if(inward)PlayerHub.baseVolume() else 0f
        if(!inward&&!p.playWhenReady)return
        p.volume=start;if(inward)p.play()
        val item=p.currentMediaItem;val began=SystemClock.elapsedRealtime()
        val task=object:Runnable{override fun run(){
            if(PlayerHub.player!==p||p.currentMediaItem!=item||!p.playWhenReady||p.playbackState==Player.STATE_ENDED){cancel();return}
            val elapsed=SystemClock.elapsedRealtime()-began;p.volume=Batch10.fade(start,end,elapsed,duration.toLong())
            if(elapsed>=duration){if(!inward)p.pause();cancel()}else handler.postDelayed(this,20)
        }};step=task;handler.post(task)
    }
}
